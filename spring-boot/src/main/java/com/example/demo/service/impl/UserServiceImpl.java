package com.example.demo.service.impl;

import com.example.demo.auth.SecurityUtils;
import com.example.demo.cache.CacheInvalidationService;
import com.example.demo.cache.CacheKeyFactory;
import com.example.demo.cache.MultiLevelCache;
import com.example.demo.cache.dto.UserCacheDto;
import com.example.demo.pojo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserRoleRepository;
import com.example.demo.service.UserService;
import com.example.demo.pojo.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务 JPA 实现 —— 继承 BaseServiceImpl 获得统一 CRUD + 分页 + 条件分页；
 * 覆写 create/update/delete 保持特殊逻辑（密码加密、唯一性校验、级联清理）。
 * 非默认实现：需要时可 @Qualifier("userServiceImpl") 显式注入。
 */
@Service
public class UserServiceImpl extends BaseServiceImpl<User, Long, UserRepository, UserVO, UserVO>
        implements UserService {

    private static final String CACHE_USER = "user";

    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CacheInvalidationService invalidation;
    private final MultiLevelCache cache;

    public UserServiceImpl(UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           PasswordEncoder passwordEncoder,
                           CacheInvalidationService invalidation,
                           MultiLevelCache cache) {
        super(userRepository);
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.invalidation = invalidation;
        this.cache = cache;
    }

    @Override
    protected String entityName() {
        return "用户";
    }

    @Override
    protected User newEntity() {
        return new User();
    }

    @Override
    protected UserVO newVO() {
        return new UserVO();
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(Long id) {
        // 用户资料走脱敏缓存；内部写路径使用 repository 直读，避免污染缓存对象
        UserCacheDto dto = cache.get(CACHE_USER, CacheKeyFactory.id(id), UserCacheDto.class,
                () -> repository.findById(id).map(UserCacheDto::from).orElse(null));
        if (dto == null) {
            throw new RuntimeException("用户不存在，ID: " + id);
        }
        return dto.toProfileUser();
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        UserCacheDto dto = cache.get(CACHE_USER, CacheKeyFactory.username(username), UserCacheDto.class,
                () -> repository.findByUsername(username).map(UserCacheDto::from).orElse(null));
        if (dto == null) {
            throw new RuntimeException("用户不存在，用户名: " + username);
        }
        return dto.toProfileUser();
    }

    @Override
    @Transactional
    public User create(User user) {
        if (repository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在: " + user.getUsername());
        }
        if (repository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("邮箱已被注册: " + user.getEmail());
        }
        // 密码 BCrypt 加密存储；未传密码时使用默认密码 123456
        String rawPassword = (user.getPassword() == null || user.getPassword().isBlank())
                ? "123456" : user.getPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        // 记录创建人：当前登录管理员（种子/系统创建为 NULL）
        String creator = SecurityUtils.currentUsername();
        if (creator != null) {
            user.setCreatedBy(creator);
        }
        return repository.save(user);
    }

    @Override
    @Transactional
    public User update(Long id, User user) {
        // Repository 直读（不经缓存），避免原地修改 Caffeine 中共享的缓存对象
        User existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + id));
        String oldUsername = existing.getUsername();
        // 密码不允许通过更新接口修改，仅更新基础资料
        existing.setUsername(user.getUsername());
        existing.setEmail(user.getEmail());
        User saved = repository.save(existing);
        invalidation.evictUserAfterUpdate(id, oldUsername, saved.getUsername());
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + id));
        // 级联清理：用户-角色 关联
        userRoleRepository.deleteByUserId(id);
        repository.deleteById(id);
        // 事务提交后失效用户资料与角色/权限派生缓存
        invalidation.evictUserAfterDelete(id, user.getUsername());
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        // 密码校验必须直读 DB（缓存 DTO 不含 password）
        User user = repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在，用户名: " + username));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码不正确");
        }
        if (oldPassword.equals(newPassword)) {
            throw new RuntimeException("新密码不能与旧密码相同");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
        invalidation.evictUserProfile(user.getId(), username);
    }
}