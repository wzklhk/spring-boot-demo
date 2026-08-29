package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
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

    /** created_at / updated_at 由数据库约束自动维护（DEFAULT CURRENT_TIMESTAMP / ON UPDATE），应用侧不写入 */
    @Column(name = "created_at", updatable = false)
    @Generated(GenerationTime.INSERT)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Generated(GenerationTime.ALWAYS)
    private LocalDateTime updatedAt;
}
