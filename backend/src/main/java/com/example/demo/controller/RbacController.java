            package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.Permission;
import com.example.demo.entity.Role;
import com.example.demo.service.RbacService;
import com.example.demo.vo.UserPermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RBAC 关联管理接口 —— 用户-角色 / 角色-权限 分配与聚合查询
 * RESTful 子资源风格：
 *   /api/users/{userId}/roles            用户的角色
 *   /api/users/{userId}/permissions      用户的全部权限（聚合）
 *   /api/roles/{roleId}/permissions      角色的权限
 */
@RestController
@RequiredArgsConstructor
public class RbacController {

    private final RbacService rbacService;

    // ── 用户-角色 ──────────────────────────────────────────────

    /** 查询用户拥有的角色 */
    @GetMapping("/api/users/{userId}/roles")
    public ResponseEntity<Result<List<Role>>> getUserRoles(@PathVariable Long userId) {
        return ResponseEntity.ok(Result.success(rbacService.getUserRoles(userId)));
    }

    /** 给用户分配角色（幂等） */
    @PostMapping("/api/users/{userId}/roles/{roleId}")
    public ResponseEntity<Result<Role>> assignRoleToUser(@PathVariable Long userId, @PathVariable Long roleId) {
        Role role = rbacService.assignRoleToUser(userId, roleId);
        return ResponseEntity.ok(Result.success("角色分配成功", role));
    }

    /** 移除用户的角色 */
    @DeleteMapping("/api/users/{userId}/roles/{roleId}")
    public ResponseEntity<Result<Void>> removeRoleFromUser(@PathVariable Long userId, @PathVariable Long roleId) {
        rbacService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.ok(Result.success("角色移除成功", null));
    }

    // ── 角色-权限 ──────────────────────────────────────────────

    /** 查询角色拥有的权限 */
    @GetMapping("/api/roles/{roleId}/permissions")
    public ResponseEntity<Result<List<Permission>>> getRolePermissions(@PathVariable Long roleId) {
        return ResponseEntity.ok(Result.success(rbacService.getRolePermissions(roleId)));
    }

    /** 给角色分配权限（幂等） */
    @PostMapping("/api/roles/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Result<Permission>> assignPermissionToRole(@PathVariable Long roleId,
                                                                     @PathVariable Long permissionId) {
        Permission permission = rbacService.assignPermissionToRole(roleId, permissionId);
        return ResponseEntity.ok(Result.success("权限分配成功", permission));
    }

    /** 移除角色的权限 */
    @DeleteMapping("/api/roles/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Result<Void>> removePermissionFromRole(@PathVariable Long roleId,
                                                                 @PathVariable Long permissionId) {
        rbacService.removePermissionFromRole(roleId, permissionId);
        return ResponseEntity.ok(Result.success("权限移除成功", null));
    }

    // ── 聚合查询 ───────────────────────────────────────────────

    /** 查询用户拥有的全部权限（MyBatis 多表 join，含角色来源） */
    @GetMapping("/api/users/{userId}/permissions")
    public ResponseEntity<Result<List<UserPermissionVO>>> getUserPermissions(@PathVariable Long userId) {
        return ResponseEntity.ok(Result.success(rbacService.getUserPermissions(userId)));
    }
}
