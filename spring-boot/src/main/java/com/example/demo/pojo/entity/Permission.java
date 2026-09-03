package com.example.demo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * 权限实体（RBAC）—— 对应表 permission；主键与创建/修改时间见 {@link BaseDO}
 * 权限编码约定：资源:动作，如 user:create / role:delete
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permission")
public class Permission extends BaseDO {

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
}