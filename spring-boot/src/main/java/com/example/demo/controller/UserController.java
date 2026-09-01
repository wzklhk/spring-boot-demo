package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.pojo.entity.User;
import com.example.demo.service.UserService;
import com.example.demo.pojo.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口 —— 统一 CRUD + 分页 + 条件分页由 BaseController 提供。
 * 不区分持久层实现：Service 面向 UserService 接口，默认注入 MyBatis 实现（@Primary）。
 */
@Tag(name = "用户管理",
        description = "统一 CRUD 接口：Controller 不区分持久层，Service 多态默认 MyBatis 实现（JPA 可 @Qualifier 切换）。\n"
                + "列表查询默认分页：GET /api/user?page=1&size=10（缺省第 1 页、每页 10 条）；"
                + "条件分页查询：POST /api/user/query，请求体传 UserVO（非空字段为等值条件）")
@RestController
@RequestMapping("/api/user")
public class UserController extends BaseController<User, Long, UserVO, UserVO> {

    private final UserService userService;

    public UserController(UserService userService) {
        super(userService);
        this.userService = userService;
    }

    /** 按用户名精确查询（单条返回，不分页） */
    @GetMapping("/username/{username}")
    @Operation(summary = "按用户名精确查询", description = "单条返回，不分页；用户不存在时返回业务错误（code=400）")
    public Result<User> getByUsername(@PathVariable String username) {
        return Result.success(userService.findByUsername(username));
    }
}