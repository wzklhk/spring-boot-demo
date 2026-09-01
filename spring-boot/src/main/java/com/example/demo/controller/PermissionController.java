package com.example.demo.controller;

import com.example.demo.pojo.entity.Permission;
import com.example.demo.service.PermissionService;
import com.example.demo.pojo.vo.PermissionVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限管理接口（RBAC）—— 统一 CRUD + 分页 + 条件分页由 BaseController 提供。
 */
@Tag(name = "权限管理",
        description = "RBAC 权限管理：统一 CRUD 接口，列表默认分页（GET /api/permission?page=1&size=10）；"
                + "条件分页查询：POST /api/permission/query，请求体传 PermissionVO（非空字段为等值条件）。\n"
                + "创建/更新会校验编码（code）与名称（name）唯一；删除级联清理角色-权限关联")
@RestController
@RequestMapping("/api/permission")
public class PermissionController extends BaseController<Permission, Long, PermissionVO, PermissionVO> {

    public PermissionController(PermissionService permissionService) {
        super(permissionService);
    }
}