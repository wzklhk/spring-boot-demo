package com.example.demo.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 缓存失效服务：只在数据库事务提交后执行失效（afterCommit）。
 * 回滚时不执行任何缓存变更。每个写入口按业务语义调用对应方法。
 * <p>User 采用“脱敏资料缓存”：失效时同时清理 主键 key 与 username 索引 key，
 * 避免“改用户名后旧 key 残留”导致按旧用户名命中过期资料。
 */
@Service
@RequiredArgsConstructor
public class CacheInvalidationService {

    public static final String CACHE_USER = "user";
    public static final String CACHE_ROLE = "role";
    public static final String CACHE_PERMISSION = "permission";
    public static final String CACHE_USER_ROLES = "user-roles";
    public static final String CACHE_ROLE_PERMISSIONS = "role-permissions";
    public static final String CACHE_USER_PERMISSIONS = "user-permissions";

    private final MultiLevelCache cache;

    /** Role 被更新（名称/编码/描述变化会影响 user-roles、user-permissions 内容） */
    public void evictRoleAndDerived(Long roleId) {
        afterCommit(() -> {
            cache.evict(CACHE_ROLE, CacheKeyFactory.id(roleId));
            cache.evictAll(CACHE_USER_ROLES);
            cache.evictAll(CACHE_USER_PERMISSIONS);
        });
    }

    /** Role 被删除（含级联 user_role / role_permission） */
    public void evictRoleAfterDelete(Long roleId) {
        afterCommit(() -> {
            cache.evict(CACHE_ROLE, CacheKeyFactory.id(roleId));
            cache.evict(CACHE_ROLE_PERMISSIONS, CacheKeyFactory.id(roleId));
            cache.evictAll(CACHE_USER_ROLES);
            cache.evictAll(CACHE_USER_PERMISSIONS);
        });
    }

    /** Permission 被更新/删除（role-permissions、user-permissions 内容随之变化） */
    public void evictPermissionAndDerived(Long permissionId) {
        afterCommit(() -> {
            cache.evict(CACHE_PERMISSION, CacheKeyFactory.id(permissionId));
            cache.evictAll(CACHE_ROLE_PERMISSIONS);
            cache.evictAll(CACHE_USER_PERMISSIONS);
        });
    }

    /** 用户资料（主键 key + username 索引 key）失效；可用于改密等资料无关写后的兜底清理 */
    public void evictUserProfile(Long userId, String username) {
        afterCommit(() -> {
            cache.evict(CACHE_USER, CacheKeyFactory.id(userId));
            if (username != null && !username.isBlank()) {
                cache.evict(CACHE_USER, CacheKeyFactory.username(username));
            }
        });
    }

    /** 用户基础资料被更新（可能改了 username：新旧两个索引 key 都要清） */
    public void evictUserAfterUpdate(Long userId, String oldUsername, String newUsername) {
        afterCommit(() -> {
            cache.evict(CACHE_USER, CacheKeyFactory.id(userId));
            if (oldUsername != null && !oldUsername.isBlank()) {
                cache.evict(CACHE_USER, CacheKeyFactory.username(oldUsername));
            }
            if (newUsername != null && !newUsername.isBlank()) {
                cache.evict(CACHE_USER, CacheKeyFactory.username(newUsername));
            }
        });
    }

    /** 用户被删除（其用户资料与角色/权限派生缓存一并失效） */
    public void evictUserAfterDelete(Long userId, String username) {
        afterCommit(() -> {
            cache.evict(CACHE_USER, CacheKeyFactory.id(userId));
            if (username != null && !username.isBlank()) {
                cache.evict(CACHE_USER, CacheKeyFactory.username(username));
            }
            cache.evict(CACHE_USER_ROLES, CacheKeyFactory.id(userId));
            cache.evict(CACHE_USER_PERMISSIONS, CacheKeyFactory.id(userId));
        });
    }

    /** 用户的角色分配/移除发生变化 */
    public void evictUserRoleLinks(Long userId) {
        afterCommit(() -> {
            cache.evict(CACHE_USER_ROLES, CacheKeyFactory.id(userId));
            cache.evict(CACHE_USER_PERMISSIONS, CacheKeyFactory.id(userId));
        });
    }

    /** 角色的权限分配/移除发生变化（影响所有持有该角色的用户聚合） */
    public void evictRolePermissionLinks(Long roleId) {
        afterCommit(() -> {
            cache.evict(CACHE_ROLE_PERMISSIONS, CacheKeyFactory.id(roleId));
            cache.evictAll(CACHE_USER_PERMISSIONS);
        });
    }

    /** 无事务时立即执行；有事务时注册 afterCommit */
    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}