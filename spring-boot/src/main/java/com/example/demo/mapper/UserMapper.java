package com.example.demo.mapper;

import com.example.demo.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis Mapper —— 与 JPA 的 UserRepository 共存，操作同一张 users 表。
 * SQL 定义见 src/main/resources/mapper/UserMapper.xml
 */
public interface UserMapper {

    List<User> findAll();

    User findById(@Param("id") Long id);

    User findByUsername(@Param("username") String username);

    int countByUsername(@Param("username") String username);

    int countByEmail(@Param("email") String email);

    int insert(User user);

    int update(User user);

    int deleteById(@Param("id") Long id);
}
