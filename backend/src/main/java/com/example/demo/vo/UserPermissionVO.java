package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户权限视图对象 —— MyBatis 多表 join 聚合查询结果
 * 包含权限信息 + 来源角色（一个权限可来自多个角色）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionVO {

    private Long id;
    private String code;
    private String name;
    private String description;

    /** 来源角色编码（如 ADMIN） */
    private String roleCode;

    /** 来源角色名称（如 管理员） */
    private String roleName;

    private LocalDateTime grantedAt;
}
