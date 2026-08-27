package com.example.demo.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SPA 路由兜底：Vue Router 使用 history 模式，
 * 直接访问 /users、/mybatis-users 等前端路由时转发到 index.html，
 * 由前端路由接管渲染（避免刷新页面 404）。
 *
 * 优先级：@RestController 接口 > 静态资源 > 此处兜底转发，
 * 因此 /api/** 与 /assets/** 不受影响。
 */
@Configuration
public class SpaForwardController implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 单层前端路由：/users、/mybatis-users 等
        registry.addViewController("/{spring:[a-zA-Z0-9-_]+}")
                .setViewName("forward:/index.html");
        // 多层前端路由（未来扩展时使用）
        registry.addViewController("/**/{spring:[a-zA-Z0-9-_]+}")
                .setViewName("forward:/index.html");
    }
}
