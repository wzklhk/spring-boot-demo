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
 * 角色实体（RBAC）—— 对应表 role
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 角色编码，如 ADMIN / OPERATOR，唯一 */
    @NotBlank(message = "角色编码不能为空")
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    /** 角色名称，如 管理员 / 操作员 */
    @NotBlank(message = "角色名称不能为空")
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
