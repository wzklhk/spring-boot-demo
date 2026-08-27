package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + id));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在，用户名: " + username));
    }

    @Transactional
    public User create(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在: " + user.getUsername());
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("邮箱已被注册: " + user.getEmail());
        }
        // 密码 BCrypt 加密存储；未传密码时使用默认密码 123456
        String rawPassword = (user.getPassword() == null || user.getPassword().isBlank())
                ? "123456" : user.getPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, User user) {
        User existing = findById(id);
        existing.setUsername(user.getUsername());
        existing.setEmail(user.getEmail());
        return userRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("用户不存在，ID: " + id);
        }
        // 级联清理：用户-角色 关联
        userRoleRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }
}
