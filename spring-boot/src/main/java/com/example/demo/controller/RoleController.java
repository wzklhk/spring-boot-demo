package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.Role;
import com.example.demo.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 角色管理接口（RBAC）—— 统一 Result 包装
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<Result<List<Role>>> list() {
        return ResponseEntity.ok(Result.success(roleService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<Role>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(Result.success(roleService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<Result<Role>> create(@Valid @RequestBody Role role) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.created(roleService.create(role)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<Role>> update(@PathVariable Long id, @Valid @RequestBody Role role) {
        return ResponseEntity.ok(Result.updated(roleService.update(id, role)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.ok(Result.deleted());
    }
}
