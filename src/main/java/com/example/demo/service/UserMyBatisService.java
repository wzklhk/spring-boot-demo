package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis 版 UserService —— 与 JPA 版 UserService 功能等价，
 * 但走 UserMapper（XML SQL），与 JPA 共享同一张 users 表。
 */
@Service
@RequiredArgsConstructor
public class UserMyBatisService {

    private final UserMapper userMapper;

    public List<User> findAll() {
        return userMapper.findAll();
    }

    public User findById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在，ID: " + id);
        }
        return user;
    }

    public User findByUsername(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在，用户名: " + username);
        }
        return user;
    }

    @Transactional
    public User create(User user) {
        if (userMapper.countByUsername(user.getUsername()) > 0) {
            throw new RuntimeException("用户名已存在: " + user.getUsername());
        }
        if (userMapper.countByEmail(user.getEmail()) > 0) {
            throw new RuntimeException("邮箱已被注册: " + user.getEmail());
        }
        // MyBatis 不走 JPA 生命周期回调，时间戳需手动填充
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    @Transactional
    public User update(Long id, User user) {
        User existing = findById(id);
        existing.setUsername(user.getUsername());
        existing.setEmail(user.getEmail());
        existing.setUpdatedAt(LocalDateTime.now());
        userMapper.update(existing);
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        if (userMapper.findById(id) == null) {
            throw new RuntimeException("用户不存在，ID: " + id);
        }
        userMapper.deleteById(id);
    }
}
