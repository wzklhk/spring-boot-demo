package com.example.demo.service;

import com.example.demo.entity.Permission;
import com.example.demo.entity.Role;
import com.example.demo.entity.RolePermission;
import com.example.demo.entity.UserRole;
import com.example.demo.mapper.RbacMapper;
import com.example.demo.repository.*;
import com.example.demo.vo.UserPermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RBAC 关联管理服务：用户-角色、角色-权限 的分配/移除，以及权限聚合查询
 */
@Service
@RequiredArgsConstructor
public class RbacService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RbacMapper rbacMapper;

    // ── 用户-角色 ──────────────────────────────────────────────

    /** 查询用户拥有的角色列表 */
    public List<Role> getUserRoles(Long userId) {
        requireUser(userId);
        List<Long> roleIds = userRoleRepository.findByUserId(userId).stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
        return roleIds.isEmpty() ? List.of() : roleRepository.findAllById(roleIds);
    }

    /** 给用户分配角色（幂等：已分配则忽略） */
    @Transactional
    public Role assignRoleToUser(Long userId, Long roleId) {
        requireUser(userId);
        Role role = requireRole(roleId);
        if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            userRoleRepository.save(UserRole.builder().userId(userId).roleId(roleId).build());
        }
        return role;
    }

    /** 移除用户的角色 */
    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        requireUser(userId);
        requireRole(roleId);
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
    }

    // ── 角色-权限 ──────────────────────────────────────────────

    /** 查询角色拥有的权限列表 */
    public List<Permission> getRolePermissions(Long roleId) {
        requireRole(roleId);
        List<Long> permissionIds = rolePermissionRepository.findByRoleId(roleId).stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
        return permissionIds.isEmpty() ? List.of() : permissionRepository.findAllById(permissionIds);
    }

    /** 给角色分配权限（幂等：已分配则忽略） */
    @Transactional
    public Permission assignPermissionToRole(Long roleId, Long permissionId) {
        requireRole(roleId);
        Permission permission = requirePermission(permissionId);
        if (!rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            rolePermissionRepository.save(RolePermission.builder().roleId(roleId).permissionId(permissionId).build());
        }
        return permission;
    }

    /** 移除角色的权限 */
    @Transactional
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        requireRole(roleId);
        requirePermission(permissionId);
        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }

    // ── 聚合查询 ───────────────────────────────────────────────

    /** 查询用户拥有的全部权限（MyBatis 多表 join，含角色来源） */
    public List<UserPermissionVO> getUserPermissions(Long userId) {
        requireUser(userId);
        return rbacMapper.findUserPermissions(userId);
    }

    // ── 内部校验 ───────────────────────────────────────────────

    private void requireUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("用户不存在，ID: " + userId);
        }
    }

    private Role requireRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在，ID: " + roleId));
    }

    private Permission requirePermission(Long permissionId) {
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("权限不存在，ID: " + permissionId));
    }
}
