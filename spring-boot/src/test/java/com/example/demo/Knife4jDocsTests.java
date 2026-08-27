package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Knife4j / OpenAPI 文档完整性测试：
 * /v3/api-docs 应包含所有 controller 的 @Tag 与 @Operation 标记。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Knife4jDocsTests {

    @Autowired
    private TestRestTemplate rest;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void apiDocsContainAllControllerTags() throws Exception {
        ResponseEntity<String> resp = rest.exchange("/v3/api-docs", HttpMethod.GET, null, String.class);
        assertEquals(200, resp.getStatusCode().value(), "/v3/api-docs 应可匿名访问（SecurityConfig 已放行）");

        JsonNode root = mapper.readTree(resp.getBody());
        assertEquals("spring-boot-demo API", root.path("info").path("title").asText());

        // 所有 controller 的 @Tag 都应出现
        List<String> tagNames = new ArrayList<>();
        root.path("tags").forEach(t -> tagNames.add(t.path("name").asText()));
        for (String expected : List.of(
                "认证管理",
                "用户管理",
                "角色管理",
                "权限管理",
                "角色权限分配（RBAC）")) {
            assertTrue(tagNames.contains(expected), "文档 tags 应包含: " + expected);
        }

        // 关键路径都应暴露
        JsonNode paths = root.path("paths");
        for (String expected : List.of(
                "/api/auth/register",
                "/api/auth/login",
                "/api/auth/logout",
                "/api/users",
                "/api/users/username/{username}",
                "/api/users/{id}",
                "/api/roles",
                "/api/permissions",
                "/api/users/{userId}/roles",
                "/api/roles/{roleId}/permissions",
                "/api/users/{userId}/permissions")) {
            assertTrue(paths.has(expected), "文档 paths 应包含: " + expected);
        }
    }

    @Test
    void operationsCarrySummaryDescriptions() throws Exception {
        ResponseEntity<String> resp = rest.exchange("/v3/api-docs", HttpMethod.GET, null, String.class);
        assertEquals(200, resp.getStatusCode().value());

        JsonNode root = mapper.readTree(resp.getBody());
        JsonNode getUsers = root.path("paths").path("/api/users");
        // GET 分页查询有 @Operation summary
        JsonNode getOp = getUsers.path("get");
        assertTrue(getOp.path("summary").asText().contains("分页"), "GET /api/users 应有分页说明");
        // 继承自 BaseController 的 @Operation 应生效
        JsonNode createOp = getUsers.path("post");
        assertEquals("创建", createOp.path("summary").asText());
        // 子类专属方法
        JsonNode usernameOp = root.path("paths").path("/api/users/username/{username}").path("get");
        assertTrue(usernameOp.path("summary").asText().contains("用户名"), "按用户名查询应有 @Operation");

        // 每个 API 路径至少有一个带 summary 的操作
        JsonNode paths = root.path("paths");
        for (var it = paths.fields(); it.hasNext(); ) {
            var entry = it.next();
            if (!entry.getKey().startsWith("/api/")) {
                continue;
            }
            entry.getValue().fields().forEachRemaining(op -> {
                if (op.getKey().equals("get") || op.getKey().equals("post")
                        || op.getKey().equals("put") || op.getKey().equals("delete")) {
                    assertFalse(op.getValue().path("summary").asText().isBlank(),
                            entry.getKey() + " " + op.getKey() + " 应有 @Operation summary");
                }
            });
        }
    }
}
