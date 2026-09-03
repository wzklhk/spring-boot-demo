package com.example.demo.service.impl;

import com.example.demo.auth.SecurityUtils;
import com.example.demo.cache.CacheInvalidationService;
import com.example.demo.cache.CacheKeyFactory;
import com.example.demo.cache.MultiLevelCache;
import com.example.demo.cache.dto.UserCacheDto;
import com.example.demo.common.PageResult;
import com.example.demo.pojo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.repository.UserRoleRepository;
import com.example.demo.service.UserService;
import com.example.demo.pojo.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务 MyBatis 实现（默认）—— 走 UserMapper（XML SQL），与 JPA 共享同一张 user 表。
 * {@link Primary} 使其成为 UserService 接口注入的默认实现（Controller/Auth 无感知切换）。
 */
@Service
@Primary
@RequiredArgsConstructor
public class UserMyBatisServiceImpl implements UserService {

    private static final String CACHE_USER = "user";

    private final UserMapper userMapper;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CacheInvalidationService invalidation;
    private final MultiLevelCache cache;

    /** 全量查询（部分内部场景仍需要） */
    public List<User> findAll() {
        return userMapper.findAll();
    }

    @Override
    public PageResult<UserVO> query(UserVO query, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        // 空 VO 时 XML 的 <where> 不生成条件 → 退化为普通分页查询
        long total = userMapper.countByCondition(query);
        List<User> list = userMapper.selectByCondition(query, (safePage - 1) * safeSize, safeSize);
        List<UserVO> vos = list.stream().map(UserMyBatisServiceImpl::toUserVO).toList();
        return PageResult.of(vos, total, safePage, safeSize);
    }

    @Override
    public User getById(Long id) {
        // 用户资料走脱敏缓存（DTO 不含 password）；写路径内部使用 findById 直读 DB
        UserCacheDto dto = cache.get(CACHE_USER, CacheKeyFactory.id(id), UserCacheDto.class,
                () -> {
                    User user = userMapper.findById(id);
                    return user == null ? null : UserCacheDto.from(user);
                });
        if (dto == null) {
            throw new RuntimeException("用户不存在，ID: " + id);
        }
        return dto.toProfileUser();
    }

    public User findById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在，ID: " + id);
        }
        return user;
    }

    @Override
    public User findByUsername(String username) {
        UserCacheDto dto = cache.get(CACHE_USER, CacheKeyFactory.username(username), UserCacheDto.class,
                () -> {
                    User user = userMapper.findByUsername(username);
                    return user == null ? null : UserCacheDto.from(user);
                });
        if (dto == null) {
            throw new RuntimeException("用户不存在，用户名: " + username);
        }
        return dto.toProfileUser();
    }

    @Override
    @Transactional
    public User create(User user) {
        if (userMapper.countByUsername(user.getUsername()) > 0) {
            throw new RuntimeException("用户名已存在: " + user.getUsername());
        }
        if (userMapper.countByEmail(user.getEmail()) > 0) {
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
        userMapper.insert(user);
        // created_at / updated_at 由数据库约束自动填充，重查一次返回真实值
        return findById(user.getId());
    }

    @Override
    @Transactional
    public User update(Long id, User user) {
        User existing = findById(id);
        String oldUsername = existing.getUsername();
        existing.setUsername(user.getUsername());
        existing.setEmail(user.getEmail());
        userMapper.update(existing);
        // 事务提交后清理主键 key 与旧/新 username 索引 key
        invalidation.evictUserAfterUpdate(id, oldUsername, existing.getUsername());
        // updated_at 由数据库约束自动刷新，重查一次返回真实值
        return findById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在，ID: " + id);
        }
        // 级联清理：用户-角色 关联
        userRoleRepository.deleteByUserId(id);
        userMapper.deleteById(id);
        // 事务提交后失效用户资料与角色/权限派生缓存
        invalidation.evictUserAfterDelete(id, user.getUsername());
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在，用户名: " + username);
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码不正确");
        }
        if (oldPassword.equals(newPassword)) {
            throw new RuntimeException("新密码不能与旧密码相同");
        }
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
        // 改密本身不改变脱敏资料，但清掉可能存在的资料缓存避免任何过期残留
        invalidation.evictUserProfile(user.getId(), username);
    }

    /** 实体 → VO（不含密码） */
    private static UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}