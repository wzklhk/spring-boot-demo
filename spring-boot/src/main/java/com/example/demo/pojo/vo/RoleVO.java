package com.example.demo.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 角色视图对象 —— 条件分页查询的入参与返回体。
 *
 * 入参：非空字段作为等值查询条件；返回：与实体字段一一对应的分页结果 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleVO {

    private Long id;

    private String code;

    private String name;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
