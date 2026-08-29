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
 * 用户-角色关联实体（RBAC）—— 对应表 user_roles
 * 一个用户可拥有多个角色，一个角色可被多个用户拥有（多对多）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_role", columnNames = {"user_id", "role_id"}))
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /** created_at 由数据库约束自动维护（DEFAULT CURRENT_TIMESTAMP），应用侧不写入 */
    @Column(name = "created_at", updatable = false)
    @Generated(GenerationTime.INSERT)
    private LocalDateTime createdAt;
}
