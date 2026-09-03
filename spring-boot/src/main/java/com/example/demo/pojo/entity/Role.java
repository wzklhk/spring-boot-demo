package com.example.demo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * 角色实体（RBAC）—— 对应表 role；主键与创建/修改时间见 {@link BaseDO}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role")
public class Role extends BaseDO {

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
}