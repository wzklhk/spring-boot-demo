package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 文档配置（Knife4j + springdoc-openapi）
 *
 * <p>访问入口（dev）：
 * <ul>
 *   <li>Knife4j UI:  http://localhost:9090/doc.html （或经 Vite 代理 http://localhost:5173/doc.html）</li>
 *   <li>OpenAPI JSON: http://localhost:9090/v3/api-docs</li>
 * </ul>
 * 生产环境（prod）已禁用：application-prod.yml 里 springdoc.api-docs.enabled=false + knife4j.enable=false
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI springBootDemoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("spring-boot-demo API")
                        .description("Spring Boot 3.5 演示项目接口文档\n"
                                + "技术栈: Spring Boot 3.5.16 / Java 21 / JPA + MyBatis 双持久层 / JWT 鉴权")
                        .version("1.0.0")
                        .contact(new Contact().name("wzklhk"))
                        .license(new License().name("MIT")));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("1-认证模块")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userManagementApi() {
        // ⚠️ 分组名不能含 "/" —— Knife4j 会把 /v3/api-docs/{group} 拼成多段路径导致 404（已踩坑）
        // 路径模式 /api/users/** 同时覆盖 RbacController 的子资源路径（/api/users/{userId}/roles 等）
        return GroupedOpenApi.builder()
                .group("2-用户与权限管理")
                .pathsToMatch("/api/users/**", "/api/roles/**", "/api/permissions/**")
                .build();
    }
}
