package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.service.UserMyBatisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MyBatis 版用户接口 —— 与 /api/users（JPA）功能等价，用于对比验证。
 * 统一 CRUD + 分页由 BaseController 提供，/username/{username} 为 MyBatis 专属查询。
 */
@RestController
@RequestMapping("/api/mybatis/users")
public class MyBatisUserController extends BaseController<User, Long> {

    private final UserMyBatisService userMyBatisService;

    public MyBatisUserController(UserMyBatisService userMyBatisService) {
        super(userMyBatisService);
        this.userMyBatisService = userMyBatisService;
    }

    @GetMapping("/username/{username}")
    public Result<User> getByUsername(@PathVariable String username) {
        return Result.success(userMyBatisService.findByUsername(username));
    }
}
