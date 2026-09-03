package com.example.demo.service;

import com.example.demo.cache.CacheInvalidationService;
import com.example.demo.cache.CacheKeyFactory;
import com.example.demo.cache.MultiLevelCache;
import com.example.demo.pojo.entity.Role;
import com.example.demo.pojo.vo.RoleVO;
import com.example.demo.repository.RolePermissionRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRoleRepository;
import com.example.demo.service.impl.BaseServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色业务（RBAC）—— 继承 BaseServiceImpl 获得统一 CRUD + 分页 + 条件分页；
 * 覆写 create/update/delete 保持原有特殊逻辑（编码/名称唯一校验、级联清理）。
 * <p>缓存：Role 按 ID 走 L1+L2（读多写少）；更新/删除在事务提交后
 * 统一失效 Role 与相关派生缓存（user-roles / user-permissions）。
 */
@Service
public class RoleService extends BaseServiceImpl<Role, Long, RoleRepository, RoleVO, RoleVO> {

    private static final String CACHE_ROLE = "role";

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final MultiLevelCache cache;
    private final CacheInvalidationService invalidation;

    public RoleService(RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       RolePermissionRepository rolePermissionRepository,
                       MultiLevelCache cache,
                       CacheInvalidationService invalidation) {
        super(roleRepository);
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.cache = cache;
        this.invalidation = invalidation;
    }

    @Override
    protected String entityName() {
        return "角色";
    }

    @Override
    protected Role newEntity() {
        return new Role();
    }

    @Override
    protected RoleVO newVO() {
        return new RoleVO();
    }

    @Override
    @Transactional(readOnly = true)
    public Role getById(Long id) {
        return cache.get(CACHE_ROLE, CacheKeyFactory.id(id), Role.class, () -> super.getById(id));
    }

    @Override
    @Transactional
    public Role create(Role role) {
        if (repository.existsByCode(role.getCode())) {
            throw new RuntimeException("角色编码已存在: " + role.getCode());
        }
        if (repository.existsByName(role.getName())) {
            throw new RuntimeException("角色名称已存在: " + role.getName());
        }
        return repository.save(role);
    }

    @Override
    @Transactional
    public Role update(Long id, Role role) {
        // 更新用 Repository 直读，避免原地修改 Caffeine 中共享的缓存对象
        Role existing = super.getById(id);
        existing.setCode(role.getCode());
        existing.setName(role.getName());
        existing.setDescription(role.getDescription());
        Role saved = repository.save(existing);
        // 角色名称/编码变化会影响 user-roles、user-permissions 缓存内容
        invalidation.evictRoleAndDerived(saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("角色不存在，ID: " + id);
        }
        // 级联清理：用户-角色、角色-权限 关联
        userRoleRepository.deleteByRoleId(id);
        rolePermissionRepository.deleteByRoleId(id);
        repository.deleteById(id);
        invalidation.evictRoleAfterDelete(id);
    }
}