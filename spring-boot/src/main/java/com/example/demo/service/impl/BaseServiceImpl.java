package com.example.demo.service.impl;

import com.example.demo.common.PageResult;
import com.example.demo.service.BaseService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA 版统一 Service 实现 —— 基于 JpaRepository 的泛型 CRUD + 分页。
 *
 * 子类继承后自动获得通用能力；特殊业务（唯一性校验、密码加密、级联清理等）覆写对应方法。
 * 查询默认分页：page 从 1 起、size 默认 10（由上层 BaseController 参数缺省值保证）。
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @param <R>  JpaRepository 子接口
 */
public abstract class BaseServiceImpl<T, ID, R extends JpaRepository<T, ID>> implements BaseService<T, ID> {

    protected final R repository;

    protected BaseServiceImpl(R repository) {
        this.repository = repository;
    }

    /** 实体中文名（用于异常信息），子类可覆写，如「用户」 */
    protected String entityName() {
        return "记录";
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<T> page(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        Page<T> result = repository.findAll(PageRequest.of(safePage - 1, safeSize));
        return PageResult.of(result.getContent(), result.getTotalElements(), safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public T getById(ID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException(entityName() + "不存在，ID: " + id));
    }

    @Override
    @Transactional
    public T create(T entity) {
        return repository.save(entity);
    }

    @Override
    @Transactional
    public T update(ID id, T entity) {
        T existing = getById(id);
        copyNonNullProperties(entity, existing);
        return repository.save(existing);
    }

    @Override
    @Transactional
    public void delete(ID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException(entityName() + "不存在，ID: " + id);
        }
        repository.deleteById(id);
    }

    /** 将 source 的非 null 属性复制到 target（通用更新策略：未传字段保持原值） */
    protected void copyNonNullProperties(Object source, Object target) {
        BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
    }

    private String[] getNullPropertyNames(Object source) {
        Set<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(source.getClass())) {
            String name = pd.getName();
            if ("class".equals(name)) {
                continue;
            }
            try {
                Method getter = pd.getReadMethod();
                if (getter != null && getter.invoke(source) == null) {
                    emptyNames.add(name);
                }
            } catch (Exception ignored) {
                // 不可读属性跳过
            }
        }
        return emptyNames.toArray(new String[0]);
    }
}
