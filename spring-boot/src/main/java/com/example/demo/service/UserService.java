package com.example.demo.service;

import com.example.demo.entity.User;

/**
 * 用户服务接口 —— 面向接口编程，Controller 只依赖本接口。
 * 多态实现：{@link com.example.demo.service.impl.UserServiceImpl}（JPA）
 * 与 {@link com.example.demo.service.impl.UserMyBatisServiceImpl}（MyBatis，默认 @Primary）。
 */
public interface UserService extends BaseService<User, Long> {

    /** 按用户名精确查询 */
    User findByUsername(String username);
}
