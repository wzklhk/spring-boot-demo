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

        // 关键路径都应暴露（单数资源名）
        JsonNode paths = root.path("paths");
        for (String expected : List.of(
                "/api/auth/register",
                "/api/auth/login",
                "/api/auth/logout",
                "/api/user/query",
                "/api/user/username/{username}",
                "/api/user/{id}",
                "/api/role/query",
                "/api/permission/query",
                "/api/user/{userId}/role",
                "/api/role/{roleId}/permission",
                "/api/user/{userId}/permission")) {
            assertTrue(paths.has(expected), "文档 paths 应包含: " + expected);
        }
    }

    /** 分组接口 /v3/api-docs/{group} 可直接访问（Knife4j UI 依赖），分组名不含 "/" */
    @Test
    void namedGroupApiDocsAreAccessible() throws Exception {
        for (String group : List.of("1-认证模块", "2-用户与权限管理")) {
            ResponseEntity<String> resp = rest.exchange(
                    "/v3/api-docs/" + group, HttpMethod.GET, null, String.class);
            assertEquals(200, resp.getStatusCode().value(), "分组 /v3/api-docs/" + group + " 应可访问");
            JsonNode root = mapper.readTree(resp.getBody());
            assertTrue(root.path("paths").size() > 0, "分组 " + group + " 应包含接口");
        }
        // RBAC 子资源路径归属"2-用户与权限管理"组（/api/user/** 模式覆盖）
        ResponseEntity<String> resp = rest.exchange(
                "/v3/api-docs/2-用户与权限管理", HttpMethod.GET, null, String.class);
        JsonNode paths = mapper.readTree(resp.getBody()).path("paths");
        assertTrue(paths.has("/api/user/{userId}/role"), "RBAC 子资源路径应在用户与权限管理组内");
        assertTrue(paths.has("/api/user/{userId}/permission"), "聚合查询路径应在用户与权限管理组内");
    }

    @Test
    void operationsCarrySummaryDescriptions() throws Exception {
        ResponseEntity<String> resp = rest.exchange("/v3/api-docs", HttpMethod.GET, null, String.class);
        assertEquals(200, resp.getStatusCode().value());

        JsonNode root = mapper.readTree(resp.getBody());
        JsonNode getUser = root.path("paths").path("/api/user");
        // 继承自 BaseController 的 @Operation 应生效
        JsonNode createOp = getUser.path("post");
        assertEquals("创建", createOp.path("summary").asText());
        // 统一分页查询（普通分页 + 条件分页合并，POST /query —— 独立路径节点）
        JsonNode queryOp = root.path("paths").path("/api/user/query").path("post");
        assertTrue(queryOp.path("summary").asText().contains("分页"),
                "POST /api/user/query 应有 @Operation summary");
        assertTrue(queryOp.path("description").asText().contains("普通分页"),
                "POST /api/user/query 应说明空 VO 即普通分页");
        // 子类专属方法
        JsonNode usernameOp = root.path("paths").path("/api/user/username/{username}").path("get");
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