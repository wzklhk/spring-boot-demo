package com.example.demo.cache.dto;

import com.example.demo.pojo.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户缓存 DTO —— 脱敏版本：绝不携带 password/角色等安全敏感字段。
 * 用于 L1/L2 缓存用户资料；需要密码的认证/改密路径始终直读数据库。
 */
@Data
public class UserCacheDto {

    private Long id;
    private String username;
    private String email;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserCacheDto from(User user) {
        UserCacheDto dto = new UserCacheDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setCreatedBy(user.getCreatedBy());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

    /** 还原为“用户资料”实体（password 恒为 null，仅用于序列化/展示，不允许再走密码校验或持久化） */
    public User toProfileUser() {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setCreatedBy(createdBy);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);
        return user;
    }
}