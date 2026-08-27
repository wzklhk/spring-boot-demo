package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口 —— 统一 CRUD + 分页由 BaseController 提供。
 * 不区分持久层实现：Service 面向 UserService 接口，默认注入 MyBatis 实现（@Primary）。
 */
@RestController
@RequestMapping("/api/users")
public class UserController extends BaseController<User, Long> {

    private final UserService userService;

    public UserController(UserService userService) {
        super(userService);
        this.userService = userService;
    }

    /** 按用户名精确查询（单条返回，不分页） */
    @GetMapping("/username/{username}")
    public Result<User> getByUsername(@PathVariable String username) {
        return Result.success(userService.findByUsername(username));
    }
}
