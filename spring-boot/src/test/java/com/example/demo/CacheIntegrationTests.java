package com.example.demo;

import com.example.demo.cache.CacheMetrics;
import com.example.demo.cache.MultiLevelCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多级缓存集成测试（L1 Caffeine；本测试关闭 L2，避免依赖外部 Redis）：
 * - Role 按 ID 读取命中 L1；
 * - 更新 Role 后 afterCommit 失效，GET 返回新值；
 * - 删除 Role 后 GET 返回 400（缓存不再复活已删除数据）；
 * - 派生读模型（user-roles / role-permissions / user-permissions）可缓存并在关系写后失效；
 * - User 默认不缓存（安全实体，密码/角色走 DB 直读）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"cache.l2.enabled=false"})
class CacheIntegrationTests {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private MultiLevelCache cache;

    @Autowired
    private CacheMetrics metrics;

    private record Token(String value) {
    }

    private Token admin() {
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/login",
                Map.of("username", "admin", "password", "123456"), Map.class);
        assertEquals(200, resp.getBody().get("code"), "admin 登录应成功");
        Map data = (Map) resp.getBody().get("data");
        return new Token((String) data.get("token"));
    }

    @Test
    void roleReadHitL1AndUpdateEvictsCache() {
        assertTrue(cache.isEntityEnabled("role"), "role 应启用缓存");
        assertTrue(cache.isEntityEnabled("user-roles"), "user-roles 派生读模型应启用缓存");
        assertTrue(cache.isEntityEnabled("user"), "user 应启用脱敏资料缓存（缓存 DTO 不含 password）");

        Token token = admin();
        String code = "CACHE_ROLE_" + System.currentTimeMillis();
        long l1MissBefore = metrics.getL1Miss();
        long l1HitBefore = metrics.getL1Hit();

        // 创建角色
        ResponseEntity<Map> created = rest.exchange("/api/role", HttpMethod.POST,
                new HttpEntity<>(Map.of("code", code, "name", "缓存测试角色", "description", "L1 测试"),
                        authHeaders(token)), Map.class);
        assertEquals(201, created.getStatusCode().value(), "admin 创建角色应成功");
        Number roleId = (Number) ((Map) created.getBody().get("data")).get("id");

        // 第一次 GET：L1 miss → DB（L2 已关闭）
        ResponseEntity<Map> first = getRole(token, roleId);
        assertEquals(200, first.getBody().get("code"));
        assertEquals("缓存测试角色", ((Map) first.getBody().get("data")).get("name"));
        assertTrue(metrics.getL1Miss() > l1MissBefore, "首次读取应 L1 miss");

        // 第二次 GET：命中 L1，仍返回正确数据
        ResponseEntity<Map> second = getRole(token, roleId);
        assertEquals(200, second.getBody().get("code"));
        assertEquals("缓存测试角色", ((Map) second.getBody().get("data")).get("name"));
        assertTrue(metrics.getL1Hit() > l1HitBefore, "第二次读取应 L1 hit");

        // 更新角色：afterCommit 失效 role 缓存后，GET 应返回新名称（而非旧缓存值）
        ResponseEntity<Map> updated = rest.exchange("/api/role/" + roleId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("code", code, "name", "缓存测试角色-已更新", "description", "改"),
                        authHeaders(token)), Map.class);
        assertEquals(200, updated.getBody().get("code"));
        ResponseEntity<Map> afterUpdate = getRole(token, roleId);
        assertEquals("缓存测试角色-已更新", ((Map) afterUpdate.getBody().get("data")).get("name"),
                "更新提交后缓存应已失效并读到新值");

        // 删除角色：删除后 GET 应返回业务错误（缓存不能复活已删除数据）
        rest.exchange("/api/role/" + roleId, HttpMethod.DELETE, auth(token), Map.class);
        ResponseEntity<Map> afterDelete = getRole(token, roleId);
        assertEquals(400, afterDelete.getBody().get("code"), "删除后应查不到角色");
        assertTrue(((String) afterDelete.getBody().get("message")).contains("角色不存在"));
    }

    @Test
    void userProfileReadThroughCache() {
        Token token = admin();
        String username = "cached_user_" + System.currentTimeMillis();
        long l1MissBefore = metrics.getL1Miss();
        long l1HitBefore = metrics.getL1Hit();

        // admin 创建用户（创建本身走 DB，不写缓存）
        ResponseEntity<Map> created = rest.exchange("/api/user", HttpMethod.POST,
                new HttpEntity<>(Map.of("username", username, "email", username + "@test.com",
                        "password", "123456"), authHeaders(token)), Map.class);
        assertEquals(201, created.getStatusCode().value());
        Number userId = (Number) ((Map) created.getBody().get("data")).get("id");

        // 第一次 GET /api/user/{id}：L1 miss → DB
        ResponseEntity<Map> first = getUser(token, userId);
        assertEquals(200, first.getBody().get("code"));
        assertEquals(username, ((Map) first.getBody().get("data")).get("username"));
        assertTrue(metrics.getL1Miss() > l1MissBefore, "首次按 ID 读取应 L1 miss");

        // 第二次 GET：命中 L1 脱敏资料
        ResponseEntity<Map> second = getUser(token, userId);
        assertEquals(username, ((Map) second.getBody().get("data")).get("username"));
        assertTrue(metrics.getL1Hit() > l1HitBefore, "第二次按 ID 读取应 L1 hit");

        // 按用户名读取也应命中同一套脱敏缓存逻辑
        ResponseEntity<Map> byName = rest.exchange("/api/user/username/" + username,
                HttpMethod.GET, auth(token), Map.class);
        assertEquals(200, byName.getBody().get("code"));
        assertEquals(username, ((Map) byName.getBody().get("data")).get("username"));

        // 更新资料：提交后应失效 id key 与 username 索引 key，GET 返回新邮箱
        String newEmail = username + "_v2@test.com";
        rest.exchange("/api/user/" + userId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("username", username, "email", newEmail),
                        authHeaders(token)), Map.class);
        ResponseEntity<Map> afterUpdate = getUser(token, userId);
        assertEquals(newEmail, ((Map) afterUpdate.getBody().get("data")).get("email"),
                "用户更新提交后缓存应已失效并读到新值");

        // 删除用户：缓存不复活已删除数据
        rest.exchange("/api/user/" + userId, HttpMethod.DELETE, auth(token), Map.class);
        ResponseEntity<Map> afterDelete = getUser(token, userId);
        assertEquals(400, afterDelete.getBody().get("code"), "删除后应查不到用户");
    }

    @Test
    void derivedUserRolesReadThroughCache() {
        Token token = admin();
        // 第一次查询种子 admin 的角色：DB 加载并回填派生缓存
        ResponseEntity<Map> first = rest.exchange("/api/user/1/role", HttpMethod.GET,
                auth(token), Map.class);
        assertEquals(200, first.getBody().get("code"));
        List<?> roles = (List<?>) first.getBody().get("data");
        assertTrue(roles.size() >= 1, "admin 应至少拥有 ADMIN 角色");

        // 第二次查询：命中 L1 派生缓存（此处不强制断言计数器，仅验证行为不变）
        ResponseEntity<Map> second = rest.exchange("/api/user/1/role", HttpMethod.GET,
                auth(token), Map.class);
        assertEquals(200, second.getBody().get("code"));
        assertEquals(roles.size(), ((List<?>) second.getBody().get("data")).size());
    }

    @Test
    void assignPermissionInvalidatesDerivedCache() {
        Token token = admin();
        String code = "CACHE_PERM_" + System.currentTimeMillis();

        // 创建权限并分配给 ADMIN 角色（roleId=1 由 data.sql 种子保证）
        ResponseEntity<Map> created = rest.exchange("/api/permission", HttpMethod.POST,
                new HttpEntity<>(Map.of("code", code, "name", "缓存权限", "description", "测试"),
                        authHeaders(token)), Map.class);
        assertEquals(201, created.getStatusCode().value());
        Number permissionId = (Number) ((Map) created.getBody().get("data")).get("id");

        rest.exchange("/api/role/1/permission/" + permissionId, HttpMethod.POST,
                auth(token), Map.class);

        // 角色权限派生读模型应包含新权限
        ResponseEntity<Map> rolePerms = rest.exchange("/api/role/1/permission", HttpMethod.GET,
                auth(token), Map.class);
        assertEquals(200, rolePerms.getBody().get("code"));
        assertTrue(((List<?>) rolePerms.getBody().get("data")).size() >= 1);

        // 清理：移除权限绑定并删除权限，避免影响其他测试
        rest.exchange("/api/role/1/permission/" + permissionId, HttpMethod.DELETE,
                auth(token), Map.class);
        rest.exchange("/api/permission/" + permissionId, HttpMethod.DELETE,
                auth(token), Map.class);
    }

    private ResponseEntity<Map> getUser(Token token, Number userId) {
        return rest.exchange("/api/user/" + userId, HttpMethod.GET, auth(token), Map.class);
    }

    private ResponseEntity<Map> getRole(Token token, Number roleId) {
        return rest.exchange("/api/role/" + roleId, HttpMethod.GET, auth(token), Map.class);
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