package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.pojo.entity.Permission;
import com.example.demo.pojo.entity.Role;
import com.example.demo.service.RbacService;
import com.example.demo.pojo.vo.UserPermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RBAC 关联管理接口 —— 用户-角色 / 角色-权限 分配与聚合查询
 * RESTful 子资源风格（路径使用单数资源名）：
 *   /api/user/{userId}/role             用户的角色
 *   /api/user/{userId}/permission       用户的全部权限（聚合）
 *   /api/role/{roleId}/permission       角色的权限
 */
@Tag(name = "角色权限分配（RBAC）",
        description = "用户-角色、角色-权限的分配/移除与聚合查询。\n"
                + "路径为 RESTful 子资源风格（单数资源名）：/api/user/{userId}/role、/api/role/{roleId}/permission 等")
@RestController
@RequiredArgsConstructor
public class RbacController {

    private final RbacService rbacService;

    // ── 用户-角色 ──────────────────────────────────────────────

    /** 查询用户拥有的角色 */
    @GetMapping("/api/user/{userId}/role")
    @Operation(summary = "查询用户拥有的角色")
    public ResponseEntity<Result<List<Role>>> getUserRoles(@PathVariable Long userId) {
        return ResponseEntity.ok(Result.success(rbacService.getUserRoles(userId)));
    }

    /** 给用户分配角色（幂等） */
    @PostMapping("/api/user/{userId}/role/{roleId}")
    @Operation(summary = "给用户分配角色", description = "幂等分配：已存在则直接返回")
    public ResponseEntity<Result<Role>> assignRoleToUser(@PathVariable Long userId, @PathVariable Long roleId) {
        Role role = rbacService.assignRoleToUser(userId, roleId);
        return ResponseEntity.ok(Result.success("角色分配成功", role));
    }

    /** 移除用户的角色 */
    @DeleteMapping("/api/user/{userId}/role/{roleId}")
    @Operation(summary = "移除用户的角色")
    public ResponseEntity<Result<Void>> removeRoleFromUser(@PathVariable Long userId, @PathVariable Long roleId) {
        rbacService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.ok(Result.success("角色移除成功", null));
    }

    // ── 角色-权限 ──────────────────────────────────────────────

    /** 查询角色拥有的权限 */
    @GetMapping("/api/role/{roleId}/permission")
    @Operation(summary = "查询角色拥有的权限")
    public ResponseEntity<Result<List<Permission>>> getRolePermissions(@PathVariable Long roleId) {
        return ResponseEntity.ok(Result.success(rbacService.getRolePermissions(roleId)));
    }

    /** 给角色分配权限（幂等） */
    @PostMapping("/api/role/{roleId}/permission/{permissionId}")
    @Operation(summary = "给角色分配权限", description = "幂等分配：已存在则直接返回")
    public ResponseEntity<Result<Permission>> assignPermissionToRole(@PathVariable Long roleId,
                                                                     @PathVariable Long permissionId) {
        Permission permission = rbacService.assignPermissionToRole(roleId, permissionId);
        return ResponseEntity.ok(Result.success("权限分配成功", permission));
    }

    /** 移除角色的权限 */
    @DeleteMapping("/api/role/{roleId}/permission/{permissionId}")
    @Operation(summary = "移除角色的权限")
    public ResponseEntity<Result<Void>> removePermissionFromRole(@PathVariable Long roleId,
                                                                 @PathVariable Long permissionId) {
        rbacService.removePermissionFromRole(roleId, permissionId);
        return ResponseEntity.ok(Result.success("权限移除成功", null));
    }

    // ── 聚合查询 ───────────────────────────────────────────────

    /** 查询用户拥有的全部权限（MyBatis 多表 join，含角色来源） */
    @GetMapping("/api/user/{userId}/permission")
    @Operation(summary = "查询用户全部权限", description = "聚合查询（MyBatis 多表 JOIN），返回权限列表及角色来源")
    public ResponseEntity<Result<List<UserPermissionVO>>> getUserPermissions(@PathVariable Long userId) {
        return ResponseEntity.ok(Result.success(rbacService.getUserPermissions(userId)));
    }
}