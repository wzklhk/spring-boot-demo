package com.example.demo.service;

import com.example.demo.cache.CacheInvalidationService;
import com.example.demo.cache.CacheKeyFactory;
import com.example.demo.cache.MultiLevelCache;
import com.example.demo.mapper.RbacMapper;
import com.example.demo.pojo.entity.Permission;
import com.example.demo.pojo.entity.Role;
import com.example.demo.pojo.entity.RolePermission;
import com.example.demo.pojo.entity.UserRole;
import com.example.demo.pojo.vo.UserPermissionVO;
import com.example.demo.repository.PermissionRepository;
import com.example.demo.repository.RolePermissionRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RBAC 关联管理服务：用户-角色、角色-权限 的分配/移除，以及权限聚合查询。
 * <p>缓存策略：关联关系本身不直接缓存（写热点、一致性枢纽），
 * 缓存其“派生读模型”：user-roles / role-permissions / user-permissions；
 * 任何关系写入都在事务提交后失效对应派生 key，并由 Redis Pub/Sub 同步其他 JVM。
 */
@Service
@RequiredArgsConstructor
public class RbacService {

    public static final String CACHE_USER_ROLES = "user-roles";
    public static final String CACHE_ROLE_PERMISSIONS = "role-permissions";
    public static final String CACHE_USER_PERMISSIONS = "user-permissions";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RbacMapper rbacMapper;
    private final MultiLevelCache cache;
    private final CacheInvalidationService invalidation;

    // ── 用户-角色 ──────────────────────────────────────────────

    /** 查询用户拥有的角色列表（L1 → L2 → DB） */
    public List<Role> getUserRoles(Long userId) {
        requireUser(userId);
        return cache.getList(CACHE_USER_ROLES, CacheKeyFactory.id(userId), Role.class,
                () -> {
                    List<Long> roleIds = userRoleRepository.findByUserId(userId).stream()
                            .map(UserRole::getRoleId)
                            .collect(Collectors.toList());
                    return roleIds.isEmpty() ? List.of() : roleRepository.findAllById(roleIds);
                });
    }

    /** 给用户分配角色（幂等：已分配则忽略） */
    @Transactional
    public Role assignRoleToUser(Long userId, Long roleId) {
        requireUser(userId);
        Role role = requireRole(roleId);
        if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            userRoleRepository.save(UserRole.builder().userId(userId).roleId(roleId).build());
        }
        invalidation.evictUserRoleLinks(userId);
        return role;
    }

    /** 移除用户的角色 */
    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        requireUser(userId);
        requireRole(roleId);
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        invalidation.evictUserRoleLinks(userId);
    }

    // ── 角色-权限 ──────────────────────────────────────────────

    /** 查询角色拥有的权限列表（L1 → L2 → DB） */
    public List<Permission> getRolePermissions(Long roleId) {
        requireRole(roleId);
        return cache.getList(CACHE_ROLE_PERMISSIONS, CacheKeyFactory.id(roleId), Permission.class,
                () -> {
                    List<Long> permissionIds = rolePermissionRepository.findByRoleId(roleId).stream()
                            .map(RolePermission::getPermissionId)
                            .collect(Collectors.toList());
                    return permissionIds.isEmpty() ? List.of() : permissionRepository.findAllById(permissionIds);
                });
    }

    /** 给角色分配权限（幂等：已分配则忽略） */
    @Transactional
    public Permission assignPermissionToRole(Long roleId, Long permissionId) {
        requireRole(roleId);
        Permission permission = requirePermission(permissionId);
        if (!rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            rolePermissionRepository.save(RolePermission.builder().roleId(roleId).permissionId(permissionId).build());
        }
        invalidation.evictRolePermissionLinks(roleId);
        return permission;
    }

    /** 移除角色的权限 */
    @Transactional
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        requireRole(roleId);
        requirePermission(permissionId);
        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
        invalidation.evictRolePermissionLinks(roleId);
    }

    // ── 聚合查询 ───────────────────────────────────────────────

    /** 查询用户拥有的全部权限（MyBatis 多表 join，含角色来源；L1 → L2 → DB） */
    public List<UserPermissionVO> getUserPermissions(Long userId) {
        requireUser(userId);
        return cache.getList(CACHE_USER_PERMISSIONS, CacheKeyFactory.id(userId),
                UserPermissionVO.class, () -> rbacMapper.findUserPermissions(userId));
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