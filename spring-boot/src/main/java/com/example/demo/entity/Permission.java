package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 权限实体（RBAC）—— 对应表 permissions
 * 权限编码约定：资源:动作，如 user:create / role:delete
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 权限编码，如 user:create，唯一 */
    @NotBlank(message = "权限编码不能为空")
    @Column(nullable = false, unique = true, length = 128)
    private String code;

    /** 权限名称，如 创建用户 */
    @NotBlank(message = "权限名称不能为空")
    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
