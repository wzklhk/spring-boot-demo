package com.example.demo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 用户实体 —— 对应表 user；主键与创建/修改时间见 {@link BaseDO}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user")
public class User extends BaseDO {

    @NotBlank(message = "用户名不能为空")
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Column(nullable = false)
    private String email;

    /** 创建人用户名（哪个用户创建了本用户）；种子/系统创建为 NULL */
    @Column(name = "created_by", length = 64)
    private String createdBy;

    /** 密码（BCrypt 加密存储）。WRITE_ONLY：可接收请求但不参与序列化，永不泄露 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, length = 100)
    private String password;
}