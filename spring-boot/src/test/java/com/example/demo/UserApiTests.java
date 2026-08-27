package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户统一接口集成测试（真实 HTTP + H2 内存库，无副作用）。
 * 覆盖：默认分页（page=1 size=10）、按用户名查询、CRUD、旧 MyBatis 路径已下线。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiTests {

    @Autowired
    private TestRestTemplate rest;

    private record Token(String value, String username) {
    }

    private Token register() {
        String suffix = String.valueOf(System.currentTimeMillis());
        String username = "api_" + suffix;
        Map<String, String> body = Map.of(
                "username", username,
                "email", username + "@test.com",
                "password", "123456");
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/register", body, Map.class);
        assertEquals(200, resp.getBody().get("code"), "注册应成功");
        Map data = (Map) resp.getBody().get("data");
        return new Token((String) data.get("token"), username);
    }

    private HttpEntity<Void> auth(Token token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.value());
        return new HttpEntity<>(headers);
    }

    @Test
    void listReturnsPaginationByDefault() {
        Token token = register();
        ResponseEntity<Map> resp = rest.exchange("/api/users", HttpMethod.GET, auth(token), Map.class);
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
    void findByUsernameReturnsSingleUser() {
        Token token = register();
        ResponseEntity<Map> resp = rest.exchange(
                "/api/users/username/" + token.username(),
                HttpMethod.GET, auth(token), Map.class);
        assertEquals(200, resp.getBody().get("code"));
        Map user = (Map) resp.getBody().get("data");
        assertEquals(token.username(), user.get("username"));
    }

    @Test
    void crudLifecycle() {
        Token token = register();
        String username = "crud_" + System.currentTimeMillis();
        Map<String, String> createBody = Map.of("username", username, "email", username + "@test.com");

        // POST 创建 → 201
        ResponseEntity<Map> created = rest.exchange("/api/users", HttpMethod.POST,
                new HttpEntity<>(createBody, authHeaders(token)), Map.class);
        assertEquals(201, created.getStatusCode().value());
        Map data = (Map) created.getBody().get("data");
        Number id = (Number) data.get("id");
        assertTrue(id.longValue() > 0);

        // PUT 更新
        Map<String, String> updateBody = Map.of("username", username, "email", username + "_v2@test.com");
        ResponseEntity<Map> updated = rest.exchange("/api/users/" + id, HttpMethod.PUT,
                new HttpEntity<>(updateBody, authHeaders(token)), Map.class);
        assertEquals(200, updated.getBody().get("code"));
        assertEquals(username + "_v2@test.com", ((Map) updated.getBody().get("data")).get("email"));

        // GET 单条
        ResponseEntity<Map> got = rest.exchange("/api/users/" + id, HttpMethod.GET, auth(token), Map.class);
        assertEquals(200, got.getBody().get("code"));

        // DELETE → 再查返回业务错误（记录不存在）
        rest.exchange("/api/users/" + id, HttpMethod.DELETE, auth(token), Map.class);
        ResponseEntity<Map> after = rest.exchange("/api/users/" + id, HttpMethod.GET, auth(token), Map.class);
        assertEquals(400, after.getBody().get("code"));
        assertTrue(((String) after.getBody().get("message")).contains("用户不存在"));
    }

    @Test
    void oldMyBatisPathIsGone() {
        Token token = register();
        // 404 错误页是 text/html，用 String 接收
        ResponseEntity<String> resp = rest.exchange(
                "/api/mybatis/users", HttpMethod.GET, auth(token), String.class);
        assertEquals(404, resp.getStatusCode().value(), "旧 /api/mybatis/users 路由应已下线");
    }

    /** 种子数据：data.sql 初始化的 admin / 123456 应可登录（验证 BCrypt hash + 幂等插入） */
    @Test
    void adminSeedUserCanLogin() {
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/login",
                Map.of("username", "admin", "password", "123456"), Map.class);
        assertEquals(200, resp.getBody().get("code"), "种子 admin 用户应可登录");
        Map data = (Map) resp.getBody().get("data");
        assertNotNull(data.get("token"));
    }

    private HttpHeaders authHeaders(Token token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.value());
        headers.set("Content-Type", "application/json");
        return headers;
    }
}
