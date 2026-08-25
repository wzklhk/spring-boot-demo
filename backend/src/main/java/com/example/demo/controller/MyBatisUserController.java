package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.service.UserMyBatisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * MyBatis 版用户接口 —— 与 /api/users（JPA）功能等价，用于对比验证。
 */
@RestController
@RequestMapping("/api/mybatis/users")
@RequiredArgsConstructor
public class MyBatisUserController {

    private final UserMyBatisService userMyBatisService;

    @GetMapping
    public ResponseEntity<Result<List<User>>> list() {
        return ResponseEntity.ok(Result.success("success (MyBatis)", userMyBatisService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<User>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(Result.success("success (MyBatis)", userMyBatisService.findById(id)));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<Result<User>> getByUsername(@PathVariable String username) {
        return ResponseEntity.ok(Result.success("success (MyBatis)", userMyBatisService.findByUsername(username)));
    }

    @PostMapping
    public ResponseEntity<Result<User>> create(@Valid @RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success("创建成功 (MyBatis)", userMyBatisService.create(user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<User>> update(@PathVariable Long id, @Valid @RequestBody User user) {
        return ResponseEntity.ok(Result.success("更新成功 (MyBatis)", userMyBatisService.update(id, user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> delete(@PathVariable Long id) {
        userMyBatisService.delete(id);
        return ResponseEntity.ok(Result.success("删除成功 (MyBatis)", null));
    }
}
