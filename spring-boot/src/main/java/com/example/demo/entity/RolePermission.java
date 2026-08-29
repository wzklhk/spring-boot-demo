package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import java.time.LocalDateTime;

/**
 * 角色-权限关联实体（RBAC）—— 对应表 role_permissions
 * 一个角色可拥有多个权限，一个权限可被多个角色拥有（多对多）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role_permissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_permission", columnNames = {"role_id", "permission_id"}))
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    /** created_at 由数据库约束自动维护（DEFAULT CURRENT_TIMESTAMP），应用侧不写入 */
    @Column(name = "created_at", updatable = false)
    @Generated(GenerationTime.INSERT)
    private LocalDateTime createdAt;
}
