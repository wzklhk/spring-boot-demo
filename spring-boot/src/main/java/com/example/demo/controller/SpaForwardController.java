package com.example.demo.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SPA 路由兜底（兼容 history 模式直链）。
 *
 * 当前前端使用 hash 模式（URL 形如 /#/users），浏览器不会直接请求前端路由路径，
 * 此兜底仅对「无 # 的历史直链」访问（如 /users）生效：转发到 index.html 由前端接管渲染，
 * 避免返回 404。
 *
 * 仅覆盖「单层」前端路由（当前路由均为单层）；
 * 不配置多层通配兜底——否则 /api/xxx 等未匹配的后端路径会被吞掉返回 index.html（200），
 * 导致未知 API 无法正常 404。未来新增多层前端路由时再按需放宽。
 */
@Configuration
public class SpaForwardController implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 单层前端路由：/users、/login 等
        registry.addViewController("/{spring:[a-zA-Z0-9-_]+}")
                .setViewName("forward:/index.html");
    }
}
