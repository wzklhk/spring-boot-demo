package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Column(nullable = false)
    private String email;

    /** 密码（BCrypt 加密存储）。WRITE_ONLY：可接收请求但不参与序列化，永不泄露 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, length = 100)
    private String password;

    /** created_at / updated_at 由数据库约束自动维护（DEFAULT CURRENT_TIMESTAMP / ON UPDATE），应用侧不写入 */
    @Column(name = "created_at", updatable = false)
    @Generated(GenerationTime.INSERT)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Generated(GenerationTime.ALWAYS)
    private LocalDateTime updatedAt;
}
