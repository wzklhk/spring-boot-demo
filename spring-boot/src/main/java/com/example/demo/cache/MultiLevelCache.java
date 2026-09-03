package com.example.demo.cache;

import java.util.List;
import java.util.function.Supplier;

/**
 * 统一多级缓存抽象（L1 Caffeine + L2 Redis）。
 * 业务层只依赖本接口，不感知具体缓存实现。
 */
public interface MultiLevelCache {

    /**
     * 读取单个对象：L1 → L2 → DB（Cache Aside，读后回填）。
     * dbLoader 返回 null 表示记录不存在（不缓存，避免污染）。
     */
    <T> T get(String cacheName, String key, Class<T> type, Supplier<T> dbLoader);

    /** 读取对象列表（JSON 数组形式存 L2） */
    <T> List<T> getList(String cacheName, String key, Class<T> elementType,
                        Supplier<List<T>> dbLoader);

    /** 失效单个 key：清本地 L1 + 删除 L2 + 广播其他 JVM 清 L1 */
    void evict(String cacheName, String key);

    /** 失效缓存名下的全部 key（本地 + L2 前缀扫描 + 广播） */
    void evictAll(String cacheName);

    /** 仅清本地 L1 单个 key（订阅端使用，避免循环广播） */
    void evictLocal(String cacheName, String key);

    /** 仅清本地 L1 全部（订阅端使用） */
    void evictAllLocal(String cacheName);

    /** 该缓存名是否启用（配置总开关 + 实体开关） */
    boolean isEntityEnabled(String cacheName);
}