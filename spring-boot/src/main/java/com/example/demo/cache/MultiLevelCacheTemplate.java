package com.example.demo.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 多级缓存模板：L1 Caffeine → L2 Redis → DB。
 * 规则：
 * - 读：Cache Aside，miss 时 DB 加载并回填 L2/L1；
 * - 写：由 CacheInvalidationService 在事务提交后调用 evict/evictAll（本类不参与写 DB）；
 * - 降级：Redis 异常 fail-open 直读 DB，并本地熔断 L2 一段时间；
 * - 多 JVM：L2 删除后广播 Redis Pub/Sub，其他 JVM 只清本地 L1。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultiLevelCacheTemplate implements MultiLevelCache {

    private static final String REDIS_PREFIX = "sb:";
    private static final long REDIS_BREAKER_MILLIS = 30_000;
    private static final int REDIS_ERROR_THRESHOLD = 5;

    private final CacheProperties properties;
    private final CacheObjectMapper cacheObjectMapper;
    private final CacheMetrics metrics;
    private final StringRedisTemplate redisTemplate;

    /** cacheName -> Caffeine 本地缓存 */
    private final ConcurrentMap<String, Cache<String, Object>> localCaches = new ConcurrentHashMap<>();

    /** 连续 Redis 错误计数 */
    private final AtomicInteger consecutiveRedisErrors = new AtomicInteger();
    /** 熔断截止时间（毫秒时间戳，0 表示未熔断） */
    private final AtomicLong redisDisabledUntil = new AtomicLong();

    @Override
    public boolean isEntityEnabled(String cacheName) {
        return properties.isEnabled() && properties.entity(cacheName) != null;
    }

    @Override
    public <T> T get(String cacheName, String key, Class<T> type, Supplier<T> dbLoader) {
        EntityCacheProperties cfg = enabledEntity(cacheName);
        if (cfg == null) {
            return dbLoader.get();
        }
        Cache<String, Object> l1 = l1Cache(cacheName, cfg);
        if (l1 == null) {
            return loadFromL2(cacheName, key, type, dbLoader, cfg);
        }
        Object hit = l1.getIfPresent(key);
        if (hit != null) {
            metrics.recordL1Hit();
            return type.cast(hit);
        }
        metrics.recordL1Miss();
        // Caffeine get(key, loader)：同一 JVM 内并发请求合并为一次加载（single flight）
        Object loaded = l1.get(key, k -> loadFromL2(cacheName, key, type, dbLoader, cfg));
        return type.cast(loaded);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String cacheName, String key, Class<T> elementType,
                               Supplier<List<T>> dbLoader) {
        EntityCacheProperties cfg = enabledEntity(cacheName);
        if (cfg == null) {
            return dbLoader.get();
        }
        Cache<String, Object> l1 = l1Cache(cacheName, cfg);
        if (l1 == null) {
            return loadListFromL2(cacheName, key, elementType, dbLoader, cfg);
        }
        Object hit = l1.getIfPresent(key);
        if (hit != null) {
            metrics.recordL1Hit();
            return (List<T>) hit;
        }
        metrics.recordL1Miss();
        Object loaded = l1.get(key, k -> loadListFromL2(cacheName, key, elementType, dbLoader, cfg));
        return (List<T>) loaded;
    }

    @Override
    public void evict(String cacheName, String key) {
        evictLocal(cacheName, key);
        EntityCacheProperties cfg = enabledEntity(cacheName);
        if (cfg == null || !properties.getL2().isEnabled()) {
            return;
        }
        metrics.recordEvict();
        deleteRedisKey(cacheName, key);
        publish(new CacheInvalidationMessage(cacheName, key, CacheInvalidationMessage.ACTION_EVICT));
    }

    @Override
    public void evictAll(String cacheName) {
        evictAllLocal(cacheName);
        EntityCacheProperties cfg = enabledEntity(cacheName);
        if (cfg == null || !properties.getL2().isEnabled()) {
            return;
        }
        metrics.recordEvictAll();
        deleteRedisKeysByPrefix(cacheName);
        publish(new CacheInvalidationMessage(cacheName, null, CacheInvalidationMessage.ACTION_EVICT_ALL));
    }

    @Override
    public void evictLocal(String cacheName, String key) {
        Cache<String, Object> cache = localCaches.get(cacheName);
        if (cache != null) {
            cache.invalidate(key);
        }
    }

    @Override
    public void evictAllLocal(String cacheName) {
        Cache<String, Object> cache = localCaches.get(cacheName);
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    // ── 私有实现 ──────────────────────────────────────────────

    private EntityCacheProperties enabledEntity(String cacheName) {
        if (!properties.isEnabled()) {
            return null;
        }
        return properties.entity(cacheName);
    }

    /** L1 未启用时返回 null */
    private Cache<String, Object> l1Cache(String cacheName, EntityCacheProperties cfg) {
        if (!properties.getL1().isEnabled()) {
            return null;
        }
        return localCaches.computeIfAbsent(cacheName,
                name -> Caffeine.newBuilder()
                        .maximumSize(cfg.getMaximumSize())
                        .expireAfterWrite(cfg.getTtl())
                        .recordStats()
                        .build());
    }

    private <T> T loadFromL2(String cacheName, String key, Class<T> type,
                             Supplier<T> dbLoader, EntityCacheProperties cfg) {
        T cached = readFromL2(cacheName, key, type, cfg);
        if (cached != null) {
            return cached;
        }
        metrics.recordDbLoad();
        T value = dbLoader.get();
        if (value != null) {
            writeToL2(cacheName, key, value, cfg);
        }
        return value;
    }

    private <T> List<T> loadListFromL2(String cacheName, String key, Class<T> elementType,
                                       Supplier<List<T>> dbLoader, EntityCacheProperties cfg) {
        List<T> cached = readListFromL2(cacheName, key, elementType, cfg);
        if (cached != null) {
            return cached;
        }
        metrics.recordDbLoad();
        List<T> value = dbLoader.get();
        if (value != null) {
            writeToL2(cacheName, key, value, cfg);
        }
        return value;
    }

    private <T> T readFromL2(String cacheName, String key, Class<T> type, EntityCacheProperties cfg) {
        if (!l2Enabled(cfg)) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(redisKey(cacheName, key));
            if (json == null) {
                metrics.recordL2Miss();
                return null;
            }
            metrics.recordL2Hit();
            return cacheObjectMapper.read(json, type);
        } catch (IllegalStateException e) {
            // 反序列化失败：按 miss 处理并清掉脏数据
            metrics.recordL2Miss();
            deleteRedisKey(cacheName, key);
            return null;
        } catch (RuntimeException e) {
            onRedisFailure(e);
            return null;
        }
    }

    private <T> List<T> readListFromL2(String cacheName, String key, Class<T> elementType,
                                       EntityCacheProperties cfg) {
        if (!l2Enabled(cfg)) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(redisKey(cacheName, key));
            if (json == null) {
                metrics.recordL2Miss();
                return null;
            }
            metrics.recordL2Hit();
            return cacheObjectMapper.readList(json, elementType);
        } catch (IllegalStateException e) {
            metrics.recordL2Miss();
            deleteRedisKey(cacheName, key);
            return null;
        } catch (RuntimeException e) {
            onRedisFailure(e);
            return null;
        }
    }

    private void writeToL2(String cacheName, String key, Object value, EntityCacheProperties cfg) {
        if (!l2Enabled(cfg)) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(redisKey(cacheName, key),
                    cacheObjectMapper.write(value), cfg.getL2Ttl());
        } catch (RuntimeException e) {
            onRedisFailure(e);
        }
    }

    private void deleteRedisKey(String cacheName, String key) {
        try {
            redisTemplate.delete(redisKey(cacheName, key));
        } catch (RuntimeException e) {
            onRedisFailure(e);
        }
    }

    private void deleteRedisKeysByPrefix(String cacheName) {
        String pattern = REDIS_PREFIX + cacheName + ":*";
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
            cursor.forEachRemaining(keys::add);
        } catch (RuntimeException e) {
            onRedisFailure(e);
            return;
        }
        if (!keys.isEmpty()) {
            try {
                redisTemplate.delete(keys);
            } catch (RuntimeException e) {
                onRedisFailure(e);
            }
        }
    }

    private void publish(CacheInvalidationMessage message) {
        try {
            redisTemplate.convertAndSend(properties.getChannel(), cacheObjectMapper.write(message));
        } catch (RuntimeException e) {
            onRedisFailure(e);
        }
    }

    private boolean l2Enabled(EntityCacheProperties cfg) {
        return properties.getL2().isEnabled() && redisAvailable();
    }

    private boolean redisAvailable() {
        long disabledUntil = redisDisabledUntil.get();
        if (disabledUntil == 0L) {
            return true;
        }
        if (System.currentTimeMillis() < disabledUntil) {
            return false;
        }
        redisDisabledUntil.set(0L);
        return true;
    }

    private void onRedisFailure(RuntimeException e) {
        metrics.recordRedisError();
        log.warn("Redis 访问失败，缓存自动降级为 DB: {}", e.toString());
        if (consecutiveRedisErrors.incrementAndGet() >= REDIS_ERROR_THRESHOLD) {
            redisDisabledUntil.set(System.currentTimeMillis() + REDIS_BREAKER_MILLIS);
            consecutiveRedisErrors.set(0);
            log.warn("Redis 连续失败达到阈值，L2 熔断 {} ms", REDIS_BREAKER_MILLIS);
        }
    }

    private String redisKey(String cacheName, String key) {
        return REDIS_PREFIX + cacheName + ":" + key;
    }
}