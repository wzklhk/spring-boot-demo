package com.example.demo.auth;

import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.auth.vo.AuthResponse;
import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口：注册 / 登录 / 退出 / 注销账号
 * - 密码统一 BCrypt 加密存储（见 UserService）
 * - 登录/注册成功返回 JWT token，前端后续请求头携带 Authorization: Bearer <token>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    /** 注册用户（密码 BCrypt 加密存储），注册成功直接返回 token（注册即登录） */
    @PostMapping("/register")
    public ResponseEntity<Result<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(req.getPassword())
                .build();
        User saved = userService.create(user);
        String token = jwtService.generateToken(saved);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success("注册成功", AuthResponse.from(saved, token, jwtService.getExpirationSeconds())));
    }

    /** 登录：校验用户名密码，成功返回 token */
    @PostMapping("/login")
    public ResponseEntity<Result<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        User user = userService.findByUsername(req.getUsername());
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(Result.success("登录成功", AuthResponse.from(user, token, jwtService.getExpirationSeconds())));
    }

    /** 退出登录：token 加入黑名单，立即失效 */
    @PostMapping("/logout")
    public ResponseEntity<Result<Void>> logout(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token != null) {
            jwtService.invalidate(token);
        }
        return ResponseEntity.ok(Result.success("退出成功", null));
    }

    /** 注销账号：删除当前登录用户（含角色关联级联清理） */
    @DeleteMapping("/account")
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
