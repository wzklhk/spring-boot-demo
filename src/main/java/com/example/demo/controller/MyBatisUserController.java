package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserMyBatisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MyBatis 版用户接口 —— 与 /api/users（JPA）功能等价，用于对比验证。
 */
@RestController
@RequestMapping("/api/mybatis/users")
@RequiredArgsConstructor
public class MyBatisUserController {

    private final UserMyBatisService userMyBatisService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        List<User> users = userMyBatisService.findAll();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success (MyBatis)");
        result.put("data", users);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        User user = userMyBatisService.findById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success (MyBatis)");
        result.put("data", user);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<Map<String, Object>> getByUsername(@PathVariable String username) {
        User user = userMyBatisService.findByUsername(username);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success (MyBatis)");
        result.put("data", user);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody User user) {
        User saved = userMyBatisService.create(user);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 201);
        result.put("message", "创建成功 (MyBatis)");
        result.put("data", saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                        @Valid @RequestBody User user) {
        User updated = userMyBatisService.update(id, user);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功 (MyBatis)");
        result.put("data", updated);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        userMyBatisService.delete(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功 (MyBatis)");
        return ResponseEntity.ok(result);
    }
}
