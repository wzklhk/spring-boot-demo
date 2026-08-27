package com.example.demo.repository;

import com.example.demo.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRoleId(Long roleId);

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);

    void deleteByRoleId(Long roleId);

    void deleteByPermissionId(Long permissionId);

    void deleteByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
