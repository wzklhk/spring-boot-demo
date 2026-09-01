package com.example.demo.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户视图对象 —— 条件分页查询的入参与返回体。
 *
 * 入参：非空字段作为等值查询条件（不包含密码，密码永不随接口返回）；
 * 返回：与实体字段一一对应（id/username/email/时间戳），作为分页结果 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;

    private String username;

    private String email;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
