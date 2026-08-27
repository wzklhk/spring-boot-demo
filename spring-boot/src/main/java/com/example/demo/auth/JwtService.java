package com.example.demo.auth;

import com.example.demo.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 令牌服务：生成 / 解析 / 校验，以及登出黑名单（内存实现，重启清空）
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationSeconds;

    /** 已登出的 token 黑名单（内存版，重启失效；生产可换 Redis） */
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expirationSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationSeconds = expirationSeconds;
    }

    /** 生成 token（jti 保证每次唯一；subject=用户名，claim: uid） */
    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())   // 唯一 jti：即使同一秒重复签发，token 也各不相同
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationSeconds * 1000))
                .signWith(key)
                .compact();
    }

    /** 解析 token 中的用户名；无效/过期返回 null */
    public String extractUsername(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /** 校验 token：签名/过期合法且 jti 不在黑名单 */
    public boolean isValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return !blacklist.contains(claims.getId()) && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /** 登出：按 jti 加入黑名单（仅失效这一个 token，不影响同秒签发的其他 token） */
    public void invalidate(String token) {
        try {
            blacklist.add(parseClaims(token).getId());
        } catch (Exception e) {
            // token 已损坏则无需加入黑名单
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
