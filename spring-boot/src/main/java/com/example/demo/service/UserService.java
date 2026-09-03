package com.example.demo.service;

import com.example.demo.pojo.entity.User;
import com.example.demo.pojo.vo.UserVO;

/**
 * 用户服务接口 —— 面向接口编程，Controller 只依赖本接口。
 * 多态实现：{@link com.example.demo.service.impl.UserServiceImpl}（JPA）
 * 与 {@link com.example.demo.service.impl.UserMyBatisServiceImpl}（MyBatis，默认 @Primary）。
 */
public interface UserService extends BaseService<User, Long, UserVO, UserVO> {

    /** 按用户名精确查询 */
    User findByUsername(String username);

    /** 修改用户密码：校验旧密码通过后写入新密码（BCrypt 加密存储） */
    void changePassword(String username, String oldPassword, String newPassword);
}