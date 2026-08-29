package com.example.demo.service.impl;

import com.example.demo.common.PageResult;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.repository.UserRoleRepository;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务 MyBatis 实现（默认）—— 走 UserMapper（XML SQL），与 JPA 共享同一张 users 表。
 * {@link Primary} 使其成为 UserService 接口注入的默认实现（Controller/Auth 无感知切换）。
 */
@Service
@Primary
@RequiredArgsConstructor
public class UserMyBatisServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    /** 全量查询（部分内部场景仍需要） */
    public List<User> findAll() {
        return userMapper.findAll();
    }

    @Override
    public PageResult<User> page(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        long total = userMapper.count();
        List<User> list = userMapper.selectPage((safePage - 1) * safeSize, safeSize);
        return PageResult.of(list, total, safePage, safeSize);
    }

    @Override
    public User getById(Long id) {
        return findById(id);
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
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在，用户名: " + username);
        }
        return user;
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
        userMapper.insert(user);
        // created_at / updated_at 由数据库约束自动填充，重查一次返回真实值
        return findById(user.getId());
    }

    @Override
    @Transactional
    public User update(Long id, User user) {
        User existing = findById(id);
        existing.setUsername(user.getUsername());
        existing.setEmail(user.getEmail());
        userMapper.update(existing);
        // updated_at 由数据库约束自动刷新，重查一次返回真实值
        return findById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (userMapper.findById(id) == null) {
            throw new RuntimeException("用户不存在，ID: " + id);
        }
        // 级联清理：用户-角色 关联
        userRoleRepository.deleteByUserId(id);
        userMapper.deleteById(id);
    }
}
