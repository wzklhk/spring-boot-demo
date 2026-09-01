package com.example.demo.service;

import com.example.demo.pojo.entity.Permission;
import com.example.demo.repository.PermissionRepository;
import com.example.demo.repository.RolePermissionRepository;
import com.example.demo.service.impl.BaseServiceImpl;
import com.example.demo.pojo.vo.PermissionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 权限业务（RBAC）—— 继承 BaseServiceImpl 获得统一 CRUD + 分页 + 条件分页；
 * 覆写 create/update/delete 保持原有特殊逻辑（编码/名称唯一校验、级联清理）。
 */
@Service
public class PermissionService extends BaseServiceImpl<Permission, Long, PermissionRepository, PermissionVO, PermissionVO> {

    private final RolePermissionRepository rolePermissionRepository;

    public PermissionService(PermissionRepository permissionRepository,
                             RolePermissionRepository rolePermissionRepository) {
        super(permissionRepository);
        this.rolePermissionRepository = rolePermissionRepository;
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
        Permission existing = getById(id);
        existing.setCode(permission.getCode());
        existing.setName(permission.getName());
        existing.setDescription(permission.getDescription());
        return repository.save(existing);
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
    }
}