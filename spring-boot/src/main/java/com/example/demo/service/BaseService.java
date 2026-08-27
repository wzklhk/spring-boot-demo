package com.example.demo.service;

import com.example.demo.common.PageResult;

/**
 * 统一 Service 接口 —— 实体 CRUD + 分页查询。
 * JPA 版（BaseServiceImpl）与 MyBatis 版（UserMyBatisService）均实现本接口，
 * 上层 BaseController 对两种持久层无差别调用。
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
public interface BaseService<T, ID> {

    /** 分页查询（page 从 1 起，size 缺省 10） */
    PageResult<T> page(int page, int size);

    /** 按 ID 查询 */
    T getById(ID id);

    /** 创建 */
    T create(T entity);

    /** 更新（ID 指定记录，entity 携带新值） */
    T update(ID id, T entity);

    /** 删除 */
    void delete(ID id);
}
