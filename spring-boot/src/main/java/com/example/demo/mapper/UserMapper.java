package com.example.demo.mapper;

import com.example.demo.pojo.entity.User;
import com.example.demo.pojo.vo.UserVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis Mapper —— 与 JPA 的 UserRepository 共存，操作同一张 user 表。
 * 继承 BaseMapper 获得统一 CRUD + 分页契约（SQL 见 src/main/resources/mapper/UserMapper.xml）。
 */
public interface UserMapper extends BaseMapper<User> {

    /** 全量查询（部分内部场景仍需要） */
    List<User> findAll();

    User findById(@Param("id") Long id);

    User findByUsername(@Param("username") String username);

    int countByUsername(@Param("username") String username);

    int countByEmail(@Param("email") String email);

    /** 修改密码：仅更新 password 字段（BCrypt 密文由 Service 层生成） */
    void updatePassword(@Param("id") Long id, @Param("password") String password);

    /** 条件分页查询：VO 非空字段作为等值条件（SQL 见 UserMapper.xml） */
    List<User> selectByCondition(@Param("vo") UserVO vo,
                                 @Param("offset") int offset,
                                 @Param("size") int size);

    /** 条件查询总记录数 */
    long countByCondition(@Param("vo") UserVO vo);
}