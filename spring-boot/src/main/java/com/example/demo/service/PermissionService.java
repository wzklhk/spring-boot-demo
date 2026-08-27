package com.example.demo.service;

import com.example.demo.entity.Permission;
import com.example.demo.repository.PermissionRepository;
import com.example.demo.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public List<Permission> findAll() {
        return permissionRepository.findAll();
    }

    public Permission findById(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("权限不存在，ID: " + id));
    }

    @Transactional
    public Permission create(Permission permission) {
        if (permissionRepository.existsByCode(permission.getCode())) {
            throw new RuntimeException("权限编码已存在: " + permission.getCode());
        }
        if (permissionRepository.existsByName(permission.getName())) {
            throw new RuntimeException("权限名称已存在: " + permission.getName());
        }
        return permissionRepository.save(permission);
    }

    @Transactional
    public Permission update(Long id, Permission permission) {
        Permission existing = findById(id);
        existing.setCode(permission.getCode());
        existing.setName(permission.getName());
        existing.setDescription(permission.getDescription());
        return permissionRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!permissionRepository.existsById(id)) {
            throw new RuntimeException("权限不存在，ID: " + id);
        }
        // 级联清理：角色-权限 关联
        rolePermissionRepository.deleteByPermissionId(id);
        permissionRepository.deleteById(id);
    }
}
