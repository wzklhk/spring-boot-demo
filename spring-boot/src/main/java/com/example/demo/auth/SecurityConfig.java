package com.example.demo.auth;

import com.example.demo.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Spring Security 配置（无状态 JWT 模式）
 *
 * 放行规则：
 *   - /api/auth/login|logout   登录/登出（无需 token；公开注册已下线）
 *   - /h2-console/**          H2 控制台（dev）
 *   - /doc.html 等            Knife4j/Swagger 文档（prod 通过 springdoc.api-docs.enabled=false 整体禁用）
 *   - 静态资源 + SPA 页面      无需 token（页面数据经 /api 携带 token 获取）
 *   - RBAC 配置写操作（见下）   仅 ADMIN 角色可创建/修改/删除
 *   - /api/**                 查看类接口仅需登录（任意已认证角色）
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)) // h2-console iframe
            .authorizeHttpRequests(auth -> auth
                // 登录/退出 无需 token；公开注册已下线（仅 ADMIN 可通过 POST /api/user 创建用户）
                .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                .requestMatchers("/h2-console/**",
                        "/doc.html", "/swagger-ui.html", "/swagger-ui/**",
                        "/v3/api-docs/**", "/webjars/**",
                        "/", "/index.html", "/assets/**", "/favicon.ico", "/favicon.svg").permitAll()
                // RBAC 配置/管理写操作仅 ADMIN：用户/角色/权限 的创建、更新、删除，以及角色分配/移除
                .requestMatchers(HttpMethod.POST, "/api/user", "/api/role", "/api/permission").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,
                        "/api/user/*/role", "/api/user/*/role/*",
                        "/api/role/*/permission", "/api/role/*/permission/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,
                        "/api/user/*", "/api/role/*", "/api/permission/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE,
                        "/api/user/*", "/api/user/*/role", "/api/user/*/role/*",
                        "/api/role/*", "/api/role/*/permission", "/api/role/*/permission/*",
                        "/api/permission/*").hasRole("ADMIN")
                // 其余 /api/**（查询/查看/登录态相关）仅需登录
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()          // SPA 路由（/users 等）交给前端路由，页面本身不鉴权
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        writeJson(response, 401, Result.error(401, "未登录或 token 已失效")))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        writeJson(response, 403, Result.error(403, "无权访问")))
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeJson(jakarta.servlet.http.HttpServletResponse response, int status, Result<?> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
