package com.example.demo.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis 统一 Mapper 接口 —— 实体 CRUD + 分页 契约。
 * 具体实体 Mapper 继承本接口，SQL 在对应 XML 中实现（namespace = 子接口全名）。
 *
 * @param <T> 实体类型
 */
public interface BaseMapper<T> {

    /** 分页查询（offset 从 0 起） */
    List<T> selectPage(@Param("offset") int offset, @Param("size") int size);

    /** 总记录数 */
    long count();

    /** 按 ID 查询 */
    T selectById(@Param("id") Long id);

    /** 新增，返回受影响行数 */
    int insert(T entity);

    /** 更新，返回受影响行数 */
    int update(T entity);

    /** 按 ID 删除，返回受影响行数 */
    int deleteById(@Param("id") Long id);
}
