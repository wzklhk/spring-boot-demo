package com.example.demo.controller;

import com.example.demo.entity.Role;
import com.example.demo.service.RoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色管理接口（RBAC）—— 统一 CRUD + 分页由 BaseController 提供。
 */
@Tag(name = "角色管理",
        description = "RBAC 角色管理：统一 CRUD 接口，列表默认分页（GET /api/roles?page=1&size=10）。\n"
                + "创建/更新会校验编码（code）与名称（name）唯一；删除级联清理用户-角色、角色-权限关联")
@RestController
@RequestMapping("/api/roles")
public class RoleController extends BaseController<Role, Long> {

    public RoleController(RoleService roleService) {
        super(roleService);
    }
}
