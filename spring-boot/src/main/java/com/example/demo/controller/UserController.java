package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口（JPA 版）—— 统一 CRUD + 分页由 BaseController 提供。
 */
@RestController
@RequestMapping("/api/users")
public class UserController extends BaseController<User, Long> {

    public UserController(UserService userService) {
        super(userService);
    }
}
