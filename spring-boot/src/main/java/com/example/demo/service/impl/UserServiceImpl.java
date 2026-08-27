package com.example.demo.service.impl;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserRoleRepository;
import com.example.demo.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务 JPA 实现 —— 继承 BaseServiceImpl 获得统一 CRUD + 分页；
 * 覆写 create/update/delete 保持特殊逻辑（密码加密、唯一性校验、级联清理）。
 * 非默认实现：需要时可 @Qualifier("userServiceImpl") 显式注入。
 */
@Service
public class UserServiceImpl extends BaseServiceImpl<User, Long, UserRepository> implements UserService {

    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           PasswordEncoder passwordEncoder) {
        super(userRepository);
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected String entityName() {
        return "用户";
    }

    @Override
    public User findByUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在，用户名: " + username));
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
        return repository.save(user);
    }

    @Override
    @Transactional
    public User update(Long id, User user) {
        User existing = getById(id);
        // 密码不允许通过更新接口修改，仅更新基础资料
        existing.setUsername(user.getUsername());
        existing.setEmail(user.getEmail());
        return repository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("用户不存在，ID: " + id);
        }
        // 级联清理：用户-角色 关联
        userRoleRepository.deleteByUserId(id);
        repository.deleteById(id);
    }
}
