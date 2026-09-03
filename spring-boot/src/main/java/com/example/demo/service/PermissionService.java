package com.example.demo.service;

import com.example.demo.cache.CacheInvalidationService;
import com.example.demo.cache.CacheKeyFactory;
import com.example.demo.cache.MultiLevelCache;
import com.example.demo.pojo.entity.Permission;
import com.example.demo.pojo.vo.PermissionVO;
import com.example.demo.repository.PermissionRepository;
import com.example.demo.repository.RolePermissionRepository;
import com.example.demo.service.impl.BaseServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 权限业务（RBAC）—— 继承 BaseServiceImpl 获得统一 CRUD + 分页 + 条件分页；
 * 覆写 create/update/delete 保持原有特殊逻辑（编码/名称唯一校验、级联清理）。
 * <p>缓存：Permission 按 ID 走 L1+L2（典型只读字典）；更新/删除在事务提交后
 * 统一失效 Permission 与派生缓存（role-permissions / user-permissions）。
 */
@Service
public class PermissionService extends BaseServiceImpl<Permission, Long, PermissionRepository, PermissionVO, PermissionVO> {

    private static final String CACHE_PERMISSION = "permission";

    private final RolePermissionRepository rolePermissionRepository;
    private final MultiLevelCache cache;
    private final CacheInvalidationService invalidation;

    public PermissionService(PermissionRepository permissionRepository,
                             RolePermissionRepository rolePermissionRepository,
                             MultiLevelCache cache,
                             CacheInvalidationService invalidation) {
        super(permissionRepository);
        this.rolePermissionRepository = rolePermissionRepository;
        this.cache = cache;
        this.invalidation = invalidation;
    }

    @Override
    protected String entityName() {
        return "权限";
    }

    @Override
    protected Permission newEntity() {
        return new Permission();
    }

    @Override
    protected PermissionVO newVO() {
        return new PermissionVO();
    }

    @Override
    @Transactional(readOnly = true)
    public Permission getById(Long id) {
        return cache.get(CACHE_PERMISSION, CacheKeyFactory.id(id), Permission.class,
                () -> super.getById(id));
    }

    @Override
    @Transactional
    public Permission create(Permission permission) {
        if (repository.existsByCode(permission.getCode())) {
            throw new RuntimeException("权限编码已存在: " + permission.getCode());
        }
        if (repository.existsByName(permission.getName())) {
            throw new RuntimeException("权限名称已存在: " + permission.getName());
        }
        return repository.save(permission);
    }

    @Override
    @Transactional
    public Permission update(Long id, Permission permission) {
        // 更新用 Repository 直读，避免原地修改 Caffeine 中共享的缓存对象
        Permission existing = super.getById(id);
        existing.setCode(permission.getCode());
        existing.setName(permission.getName());
        existing.setDescription(permission.getDescription());
        Permission saved = repository.save(existing);
        // 权限内容变化会影响 role-permissions、user-permissions 缓存
        invalidation.evictPermissionAndDerived(saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("权限不存在，ID: " + id);
        }
        // 级联清理：角色-权限 关联
        rolePermissionRepository.deleteByPermissionId(id);
        repository.deleteById(id);
        invalidation.evictPermissionAndDerived(id);
    }
}