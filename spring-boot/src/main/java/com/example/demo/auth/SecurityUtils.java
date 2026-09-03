package com.example.demo.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具 —— 从 SecurityContext 读取操作者身份，供服务层记录创建人。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 当前登录用户名；未登录（匿名/无上下文）返回 null */
    public static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof String principal && "anonymousUser".equals(principal)) {
            return null;
        }
        return authentication.getName();
    }
}