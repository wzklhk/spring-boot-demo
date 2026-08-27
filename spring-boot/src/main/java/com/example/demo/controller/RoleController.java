package com.example.demo.controller;

import com.example.demo.entity.Role;
import com.example.demo.service.RoleService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色管理接口（RBAC）—— 统一 CRUD + 分页由 BaseController 提供。
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController extends BaseController<Role, Long> {

    public RoleController(RoleService roleService) {
        super(roleService);
    }
}
