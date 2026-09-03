package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户统一接口集成测试（真实 HTTP + 内存库，无副作用）。
 * 覆盖：默认分页、条件分页查询（VO 入参）、按用户名查询、CRUD、
 *       RBAC 权限（仅 ADMIN 可写、其他角色只读）、禁止自行注册、用户对象记录创建人。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiTests {

    @Autowired
    private TestRestTemplate rest;

    private record Token(String value, String username) {
    }

    /** 以种子管理员 admin 登录（测试中的所有写操作均以 admin 身份执行） */
    private Token admin() {
        return login("admin", "123456");
    }

    private Token login(String username, String password) {
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/login",
                Map.of("username", username, "password", password), Map.class);
        assertEquals(200, resp.getBody().get("code"), "登录应成功: " + username);
        Map data = (Map) resp.getBody().get("data");
        return new Token((String) data.get("token"), username);
    }

    /** admin 通过 POST /api/user 创建用户，返回新用户数据 */
    private Map<String, Object> createUserByAdmin(Token token, String username, String email) {
        ResponseEntity<Map> resp = rest.exchange("/api/user", HttpMethod.POST,
                new HttpEntity<>(Map.of("username", username, "email", email), authHeaders(token)), Map.class);
        assertEquals(HttpStatus.CREATED.value(), resp.getStatusCode().value(), "admin 创建用户应返回 201");
        assertEquals(201, resp.getBody().get("code"));
        return (Map<String, Object>) resp.getBody().get("data");
    }

    private HttpEntity<Void> auth(Token token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.value());
        return new HttpEntity<>(headers);
    }

    @Test
    void queryWithEmptyVoReturnsPlainPagination() {
        Token token = admin();
        // 统一查询 API：传空 VO {} 退化为普通分页查询
        ResponseEntity<Map> resp = rest.exchange("/api/user/query?page=1&size=10", HttpMethod.POST,
                new HttpEntity<>(Map.of(), authHeaders(token)), Map.class);
        assertEquals(200, resp.getBody().get("code"));
        Map page = (Map) resp.getBody().get("data");
        // 默认缺省：第 1 页、每页 10 条（同一上下文测试间数据累积，数量只做下限断言）
        assertEquals(1, page.get("page"));
        assertEquals(10, page.get("size"));
        assertTrue(((Number) page.get("total")).longValue() >= 1);
        assertTrue(((java.util.List<?>) page.get("list")).size() >= 1);
        assertTrue(((Number) page.get("pages")).intValue() >= 1);
    }

    @Test
    void conditionalQueryReturnsPagedVo() {
        Token token = admin();
        String username = "cond_" + System.currentTimeMillis();
        createUserByAdmin(token, username, username + "@test.com");

        // 请求体传 UserVO（非空字段为等值条件），返回 VO 分页结果
        ResponseEntity<Map> resp = rest.exchange("/api/user/query?page=1&size=10", HttpMethod.POST,
                new HttpEntity<>(Map.of("username", username), authHeaders(token)), Map.class);
        assertEquals(200, resp.getBody().get("code"));
        Map page = (Map) resp.getBody().get("data");
        assertEquals(1, page.get("page"));
        assertEquals(10, page.get("size"));
        java.util.List<?> list = (java.util.List<?>) page.get("list");
        assertEquals(1, list.size(), "按用户名等值条件应恰好命中 1 条");
        Map user = (Map) list.get(0);
        assertEquals(username, user.get("username"));
        assertFalse(user.containsKey("password"), "条件查询返回 VO 不应包含密码");
        assertEquals("admin", user.get("createdBy"), "创建人应为当前登录的 admin");
        // 时间应按世界时间（UTC）输出并带 Z，前端据此转本地时间显示
        String createdAt = (String) user.get("createdAt");
        assertNotNull(createdAt, "返回应包含创建时间");
        assertTrue(createdAt.endsWith("Z"), "时间应按 UTC 输出（ISO-8601 带 Z）: " + createdAt);
        long driftMinutes = Math.abs(Duration.between(Instant.parse(createdAt), Instant.now()).toMinutes());
        assertTrue(driftMinutes <= 5, "创建时间应为 UTC 世界时间，与当前 UTC 偏差过大: " + createdAt);
    }

    @Test
    void findByUsernameReturnsSingleUser() {
        Token token = admin();
        String username = "lookup_" + System.currentTimeMillis();
        createUserByAdmin(token, username, username + "@test.com");

        ResponseEntity<Map> resp = rest.exchange(
                "/api/user/username/" + username,
                HttpMethod.GET, auth(token), Map.class);
        assertEquals(200, resp.getBody().get("code"));
        Map user = (Map) resp.getBody().get("data");
        assertEquals(username, user.get("username"));
        assertEquals("admin", user.get("createdBy"), "用户对象应保存创建它的用户");
    }

    @Test
    void crudLifecycle() {
        Token token = admin();
        String username = "crud_" + System.currentTimeMillis();
        Map<String, Object> created = createUserByAdmin(token, username, username + "@test.com");
        Number id = (Number) created.get("id");
        assertTrue(id.longValue() > 0);

        // PUT 更新
        Map<String, String> updateBody = Map.of("username", username, "email", username + "_v2@test.com");
        ResponseEntity<Map> updated = rest.exchange("/api/user/" + id, HttpMethod.PUT,
                new HttpEntity<>(updateBody, authHeaders(token)), Map.class);
        assertEquals(200, updated.getBody().get("code"));
        assertEquals(username + "_v2@test.com", ((Map) updated.getBody().get("data")).get("email"));

        // GET 单条
        ResponseEntity<Map> got = rest.exchange("/api/user/" + id, HttpMethod.GET, auth(token), Map.class);
        assertEquals(200, got.getBody().get("code"));

        // DELETE → 再查返回业务错误（记录不存在）
        rest.exchange("/api/user/" + id, HttpMethod.DELETE, auth(token), Map.class);
        ResponseEntity<Map> after = rest.exchange("/api/user/" + id, HttpMethod.GET, auth(token), Map.class);
        assertEquals(400, after.getBody().get("code"));
        assertTrue(((String) after.getBody().get("message")).contains("用户不存在"));
    }

    @Test
    void nonAdminCanOnlyRead() {
        Token admin = admin();
        // admin 创建普通用户（无角色），该用户登录后仅能查看
        String username = "viewer_" + System.currentTimeMillis();
        Map<String, Object> created = createUserByAdmin(admin, username, username + "@test.com");
        Number viewerId = (Number) created.get("id");
        Token viewer = login(username, "123456");

        // 查看类接口：任意已认证角色可用
        ResponseEntity<Map> query = rest.exchange("/api/user/query?page=1&size=10", HttpMethod.POST,
                new HttpEntity<>(Map.of(), authHeaders(viewer)), Map.class);
        assertEquals(200, query.getBody().get("code"), "普通角色应可查看用户列表");

        // 创建用户 → 403
        ResponseEntity<Map> create = rest.exchange("/api/user", HttpMethod.POST,
                new HttpEntity<>(Map.of("username", "x_" + System.currentTimeMillis(),
                        "email", "x_" + System.currentTimeMillis() + "@test.com"), authHeaders(viewer)), Map.class);
        assertEquals(HttpStatus.FORBIDDEN.value(), create.getStatusCode().value(), "普通角色不能创建用户");
        assertEquals(403, create.getBody().get("code"));

        // 更新/删除用户 → 403
        ResponseEntity<Map> update = rest.exchange("/api/user/" + viewerId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("username", username, "email", username + "@test.com"),
                        authHeaders(viewer)), Map.class);
        assertEquals(HttpStatus.FORBIDDEN.value(), update.getStatusCode().value(), "普通角色不能修改用户");
        ResponseEntity<Map> delete = rest.exchange("/api/user/" + viewerId, HttpMethod.DELETE,
                auth(viewer), Map.class);
        assertEquals(HttpStatus.FORBIDDEN.value(), delete.getStatusCode().value(), "普通角色不能删除用户");

        // RBAC 配置写操作（分配角色）→ 403
        ResponseEntity<Map> assign = rest.exchange("/api/user/" + viewerId + "/role/1", HttpMethod.POST,
                auth(viewer), Map.class);
        assertEquals(HttpStatus.FORBIDDEN.value(), assign.getStatusCode().value(), "普通角色不能分配角色");
        ResponseEntity<Map> unassign = rest.exchange("/api/user/" + viewerId + "/role/1", HttpMethod.DELETE,
                auth(viewer), Map.class);
        assertEquals(HttpStatus.FORBIDDEN.value(), unassign.getStatusCode().value(), "普通角色不能移除角色");
    }

    @Test
    void selfRegistrationIsDisabled() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/register",
                Map.of("username", "anon_" + suffix,
                        "email", "anon_" + suffix + "@test.com",
                        "password", "123456"), Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), resp.getStatusCode().value(),
                "公开注册应已下线（未登录调用返回 401）");
        assertEquals(401, resp.getBody().get("code"));
    }

    @Test
    void userCanChangeOwnPassword() {
        Token admin = admin();
        String username = "pwd_" + System.currentTimeMillis();
        createUserByAdmin(admin, username, username + "@test.com");
        Token before = login(username, "123456");

        // 普通用户可修改自己的密码（POST /api/auth/password，非 ADMIN 专属）
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/password",
                new HttpEntity<>(Map.of("oldPassword", "123456", "newPassword", "abc12345"),
                        authHeaders(before)), Map.class);
        assertEquals(200, resp.getBody().get("code"), "修改密码应成功");
        assertEquals("密码修改成功", resp.getBody().get("message"));

        // 新密码可登录，旧密码失效
        login(username, "abc12345");
        ResponseEntity<Map> oldLogin = rest.postForEntity("/api/auth/login",
                Map.of("username", username, "password", "123456"), Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), oldLogin.getStatusCode().value(), "旧密码应失效");
    }

    @Test
    void changePasswordRejectsWrongOldPassword() {
        Token admin = admin();
        String username = "badpwd_" + System.currentTimeMillis();
        createUserByAdmin(admin, username, username + "@test.com");
        Token token = login(username, "123456");

        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/password",
                new HttpEntity<>(Map.of("oldPassword", "wrong-old", "newPassword", "abc12345"),
                        authHeaders(token)), Map.class);
        assertEquals(400, resp.getBody().get("code"), "旧密码错误应返回业务错误");
        assertTrue(((String) resp.getBody().get("message")).contains("旧密码不正确"));
    }

    @Test
    void oldMyBatisPathIsGone() {
        Token token = admin();
        // 404 错误页是 text/html，用 String 接收
        ResponseEntity<String> resp = rest.exchange(
                "/api/mybatis/users", HttpMethod.GET, auth(token), String.class);
        assertEquals(404, resp.getStatusCode().value(), "旧 /api/mybatis/users 路由应已下线");
    }

    /** 种子数据：admin / 123456 + ADMIN 角色绑定应生效，登录结果带角色 */
    @Test
    void adminSeedUserCanLoginWithAdminRole() {
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/login",
                Map.of("username", "admin", "password", "123456"), Map.class);
        assertEquals(200, resp.getBody().get("code"), "种子 admin 用户应可登录");
        Map data = (Map) resp.getBody().get("data");
        assertNotNull(data.get("token"));
        Map user = (Map) data.get("user");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) user.get("roles");
        assertTrue(roles.contains("ADMIN"), "admin 应拥有 ADMIN 角色: " + roles);
    }

    private HttpHeaders authHeaders(Token token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.value());
        headers.set("Content-Type", "application/json");
        return headers;
    }
}