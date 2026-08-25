package com.example.demo.service;

import com.example.demo.entity.Role;
import com.example.demo.repository.RolePermissionRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public Role findById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在，ID: " + id));
    }

    @Transactional
    public Role create(Role role) {
        if (roleRepository.existsByCode(role.getCode())) {
            throw new RuntimeException("角色编码已存在: " + role.getCode());
        }
        if (roleRepository.existsByName(role.getName())) {
            throw new RuntimeException("角色名称已存在: " + role.getName());
        }
        return roleRepository.save(role);
    }

    @Transactional
    public Role update(Long id, Role role) {
        Role existing = findById(id);
        existing.setCode(role.getCode());
        existing.setName(role.getName());
        existing.setDescription(role.getDescription());
        return roleRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new RuntimeException("角色不存在，ID: " + id);
        }
        // 级联清理：用户-角色、角色-权限 关联
        userRoleRepository.deleteByRoleId(id);
        rolePermissionRepository.deleteByRoleId(id);
        roleRepository.deleteById(id);
    }
}
