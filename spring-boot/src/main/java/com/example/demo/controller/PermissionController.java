package com.example.demo.controller;

import com.example.demo.entity.Permission;
import com.example.demo.service.PermissionService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限管理接口（RBAC）—— 统一 CRUD + 分页由 BaseController 提供。
 */
@RestController
@RequestMapping("/api/permissions")
public class PermissionController extends BaseController<Permission, Long> {

    public PermissionController(PermissionService permissionService) {
        super(permissionService);
    }
}
