package com.example.demo;

import com.example.demo.service.UserService;
import com.example.demo.service.impl.UserMyBatisServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DemoApplicationTests {

    @Autowired
    private UserService userService;

    @Test
    void contextLoads() {
    }

    /** Service 面向接口多态注入：默认实现应为 MyBatis（@Primary） */
    @Test
    void userServiceDefaultsToMyBatisImpl() {
        assertTrue(userService instanceof UserMyBatisServiceImpl,
                "UserService 默认注入应为 UserMyBatisServiceImpl（@Primary）");
    }
}