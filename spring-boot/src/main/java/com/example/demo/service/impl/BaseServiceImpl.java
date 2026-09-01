package com.example.demo.service.impl;

import com.example.demo.common.PageResult;
import com.example.demo.service.BaseService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JPA 版统一 Service 实现 —— 基于 JpaRepository 的泛型 CRUD + 统一分页查询。
 *
 * 子类继承后自动获得通用能力；特殊业务（唯一性校验、密码加密、级联清理等）覆写对应方法。
 * 统一分页查询采用 Spring Data Query By Example：入参 VO 的非空字段作为等值条件，
 * VO 为空（全字段 null）时匹配全部记录，退化为普通分页查询；结果映射为 VO 返回
 * （子类可覆写 toVO / newEntity / newVO 定制映射）。
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @param <R>  JpaRepository 子接口
 * @param <Q>  查询入参类型（实体 VO）
 * @param <V>  查询返回类型（实体 VO）
 */
public abstract class BaseServiceImpl<T, ID, R extends JpaRepository<T, ID>, Q, V>
        implements BaseService<T, ID, Q, V> {

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
    public PageResult<V> query(Q query, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        // Query By Example：非空字段作为等值条件；全空 VO 匹配全部 → 普通分页
        Example<T> example = Example.of(toEntity(query),
                ExampleMatcher.matching().withIgnoreNullValues());
        Page<T> result = repository.findAll(example, PageRequest.of(safePage - 1, safeSize));
        List<V> list = result.getContent().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotalElements(), safePage, safeSize);
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

    /** 子类提供空实体实例（查询探针用） */
    protected abstract T newEntity();

    /** 子类提供空 VO 实例（结果映射用） */
    protected abstract V newVO();

    /** 入参 VO → 查询探针：默认按同名属性拷贝（非空字段作为等值条件），子类可覆写 */
    protected T toEntity(Q query) {
        T probe = newEntity();
        BeanUtils.copyProperties(query, probe);
        return probe;
    }

    /** 实体 → VO：默认按同名属性拷贝，子类可覆写定制（如聚合字段） */
    protected V toVO(T entity) {
        V vo = newVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
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