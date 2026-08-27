package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.Permission;
import com.example.demo.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 权限管理接口（RBAC）—— 统一 Result 包装
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<Result<List<Permission>>> list() {
        return ResponseEntity.ok(Result.success(permissionService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<Permission>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(Result.success(permissionService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<Result<Permission>> create(@Valid @RequestBody Permission permission) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.created(permissionService.create(permission)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<Permission>> update(@PathVariable Long id, @Valid @RequestBody Permission permission) {
        return ResponseEntity.ok(Result.updated(permissionService.update(id, permission)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ResponseEntity.ok(Result.deleted());
    }
}
