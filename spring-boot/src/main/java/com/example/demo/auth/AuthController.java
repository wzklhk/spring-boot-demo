package com.example.demo.auth;

import com.example.demo.auth.dto.ChangePasswordRequest;
import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.vo.AuthResponse;
import com.example.demo.common.Result;
import com.example.demo.pojo.entity.User;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证接口：登录 / 修改密码 / 退出 / 注销账号（公开注册已下线）
 * - 密码统一 BCrypt 加密存储（见 UserService）
 * - 仅 ADMIN 可通过 POST /api/user 创建新用户（见 SecurityConfig 角色规则）
 * - 登录成功返回 JWT token 与角色列表，前端据此控制查看/修改入口
 */
@Tag(name = "认证管理",
        description = "登录 / 修改密码 / 退出 / 注销（公开注册已下线，仅 ADMIN 可创建用户）。\n"
                + "任意已登录用户可修改自己的密码；登录成功返回 JWT token 与角色，"
                + "后续请求头携带 Authorization: Bearer {token} 即可访问 /api/** 接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    /** 登录：校验用户名密码，成功返回 token 与角色列表 */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "校验用户名密码，成功返回 JWT token 与角色列表")
    public ResponseEntity<Result<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        User user = userService.findByUsername(req.getUsername());
        // 角色编码（去掉 ROLE_ 前缀）随登录结果返回，前端据此控制查看/修改入口
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .toList();
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(Result.success("登录成功",
                AuthResponse.from(user, token, jwtService.getExpirationSeconds(), roles)));
    }

    /** 修改当前登录用户密码：校验旧密码通过后写入新密码（BCrypt 加密存储） */
    @PostMapping("/password")
    @Operation(summary = "修改密码", description = "任意已登录用户可修改自己的密码；需在请求体携带旧密码以校验身份")
    public ResponseEntity<Result<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        String username = SecurityUtils.currentUsername();
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(401, "未登录或 token 已失效"));
        }
        userService.changePassword(username, req.getOldPassword(), req.getNewPassword());
        return ResponseEntity.ok(Result.success("密码修改成功", null));
    }

    /** 退出登录：token 加入黑名单，立即失效 */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "当前请求携带的 token 加入黑名单，立即失效")
    public ResponseEntity<Result<Void>> logout(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token != null) {
            jwtService.invalidate(token);
        }
        return ResponseEntity.ok(Result.success("退出成功", null));
    }

    /** 注销账号：删除当前登录用户（含角色关联级联清理） */
    @DeleteMapping("/account")
    @Operation(summary = "注销账号", description = "删除当前登录用户（含角色关联级联清理），需携带有效 token")
    public ResponseEntity<Result<Void>> deleteAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(authentication.getName());
        userService.delete(user.getId());
        return ResponseEntity.ok(Result.success("账号已注销", null));
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
