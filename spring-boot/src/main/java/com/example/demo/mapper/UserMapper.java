package com.example.demo.mapper;

import com.example.demo.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis Mapper —— 与 JPA 的 UserRepository 共存，操作同一张 users 表。
 * 继承 BaseMapper 获得统一 CRUD + 分页契约（SQL 见 src/main/resources/mapper/UserMapper.xml）。
 */
public interface UserMapper extends BaseMapper<User> {

    /** 全量查询（部分内部场景仍需要） */
    List<User> findAll();

    User findById(@Param("id") Long id);

    User findByUsername(@Param("username") String username);

    int countByUsername(@Param("username") String username);

    int countByEmail(@Param("email") String email);
}
