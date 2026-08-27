package com.example.demo.service;

import com.example.demo.entity.Role;
import com.example.demo.repository.RolePermissionRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRoleRepository;
import com.example.demo.service.impl.BaseServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色业务（RBAC）—— 继承 BaseServiceImpl 获得统一 CRUD + 分页；
 * 覆写 create/update/delete 保持原有特殊逻辑（编码/名称唯一校验、级联清理）。
 */
@Service
public class RoleService extends BaseServiceImpl<Role, Long, RoleRepository> {

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleService(RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       RolePermissionRepository rolePermissionRepository) {
        super(roleRepository);
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    protected String entityName() {
        return "角色";
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
        Role existing = getById(id);
        existing.setCode(role.getCode());
        existing.setName(role.getName());
        existing.setDescription(role.getDescription());
        return repository.save(existing);
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
    }
}
