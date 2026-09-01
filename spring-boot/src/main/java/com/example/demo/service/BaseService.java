package com.example.demo.service;

import com.example.demo.common.PageResult;

/**
 * 统一 Service 接口 —— 实体 CRUD + 统一分页查询。
 * JPA 版（BaseServiceImpl）与 MyBatis 版（UserMyBatisService）均实现本接口，
 * 上层 BaseController 对两种持久层无差别调用。
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @param <Q>  查询入参类型（实体 VO）
 * @param <V>  查询返回类型（实体 VO）
 */
public interface BaseService<T, ID, Q, V> {

    /** 统一分页查询：请求体 VO 非空字段作为等值条件；VO 为空时退化为普通分页查询（page 从 1 起） */
    PageResult<V> query(Q query, int page, int size);

    /** 按 ID 查询 */
    T getById(ID id);

    /** 创建 */
    T create(T entity);

    /** 更新（ID 指定记录，entity 携带新值） */
    T update(ID id, T entity);

    /** 删除 */
    void delete(ID id);
}