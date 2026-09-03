package com.example.demo;

import com.example.demo.cache.CacheMetrics;
import com.example.demo.cache.MultiLevelCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * L2（Redis）集成测试：需要本地 Redis（spring-boot-demo-redis:6379）。
 * 覆盖：L1 miss → L2 miss → DB 回填；强制清 L1 后命中 L2；
 * 更新角色 afterCommit 删除 Redis key 并失效 L1；删除后不再命中旧缓存。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CacheRedisIntegrationTests {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private MultiLevelCache cache;

    @Autowired
    private CacheMetrics metrics;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private record Token(String value) {
    }

    private Token admin() {
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/login",
                Map.of("username", "admin", "password", "123456"), Map.class);
        assertEquals(200, resp.getBody().get("code"), "admin 登录应成功");
        return new Token((String) ((Map) resp.getBody().get("data")).get("token"));
    }

    @Test
    void redisL2HitAndAfterCommitInvalidation() {
        clearSbKeys(); // 清掉历史缓存，保证断言从干净状态开始
        Token token = admin(); // 登录按用户名读取用户资料 → 写入 sb:user:username:admin（脱敏 DTO）
        String code = "REDIS_ROLE_" + System.currentTimeMillis();

        // User 脱敏资料应已写入 Redis，且绝不含 password
        String userJson = redisTemplate.opsForValue().get("sb:user:username:admin");
        assertNotNull(userJson, "admin 登录后应生成 sb:user:username:admin");
        assertFalse(userJson.contains("password"), "Redis 中的用户资料不得包含密码字段");
        String redisKey = "sb:role:";

        // 创建角色
        ResponseEntity<Map> created = rest.exchange("/api/role", HttpMethod.POST,
                new HttpEntity<>(Map.of("code", code, "name", "Redis缓存角色"), authHeaders(token)), Map.class);
        assertEquals(201, created.getStatusCode().value());
        Number roleId = (Number) ((Map) created.getBody().get("data")).get("id");
        redisKey += roleId;

        long l2MissBefore = metrics.getL2Miss();
        long l2HitBefore = metrics.getL2Hit();

        // 第一次 GET：L1 miss → L2 miss → DB → 回填 L2/L1
        ResponseEntity<Map> first = getRole(token, roleId);
        assertEquals("Redis缓存角色", ((Map) first.getBody().get("data")).get("name"));
        assertTrue(metrics.getL2Miss() > l2MissBefore, "首次读取应 L2 miss");
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey(redisKey)), "DB 读取后应回填 Redis key");

        // 第二次 GET：命中 L1
        getRole(token, roleId);

        // 清 L1 后第三次 GET：应命中 L2（不再访问 DB）
        cache.evictLocal("role", String.valueOf(roleId));
        ResponseEntity<Map> fromL2 = getRole(token, roleId);
        assertEquals("Redis缓存角色", ((Map) fromL2.getBody().get("data")).get("name"));
        assertTrue(metrics.getL2Hit() > l2HitBefore, "清除 L1 后应命中 L2");

        // 更新：afterCommit 先删 Redis key 再返回，随后 GET 重新加载新值
        ResponseEntity<Map> updated = rest.exchange("/api/role/" + roleId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("code", code, "name", "Redis缓存角色-已更新"), authHeaders(token)), Map.class);
        assertEquals(200, updated.getBody().get("code"));
        assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(redisKey)), "更新提交后 Redis key 应被删除");

        ResponseEntity<Map> afterUpdate = getRole(token, roleId);
        assertEquals("Redis缓存角色-已更新", ((Map) afterUpdate.getBody().get("data")).get("name"));

        // 删除：Redis key 被清理，删除后查询不复活旧数据
        rest.exchange("/api/role/" + roleId, HttpMethod.DELETE, auth(token), Map.class);
        assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(redisKey)), "删除后 Redis key 应被清理");
        ResponseEntity<Map> afterDelete = getRole(token, roleId);
        assertEquals(400, afterDelete.getBody().get("code"), "删除后应查不到角色");

        assertEquals(0, metrics.getRedisError(), "Redis 正常时不应产生错误计数");
    }

    private ResponseEntity<Map> getRole(Token token, Number roleId) {
        return rest.exchange("/api/role/" + roleId, HttpMethod.GET, auth(token), Map.class);
    }

    private void clearSbKeys() {
        java.util.Set<String> keys = redisTemplate.keys("sb:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private HttpEntity<Void> auth(Token token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.value());
        return new HttpEntity<>(headers);
    }

    private HttpHeaders authHeaders(Token token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.value());
        headers.set("Content-Type", "application/json");
        return headers;
    }
}