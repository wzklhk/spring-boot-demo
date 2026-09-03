package com.example.demo.cache;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/** 缓存访问统计（L1/L2/DB/Redis 错误等），供测试与后续监控接入 */
@Component
public class CacheMetrics {

    private final AtomicLong l1Hit = new AtomicLong();
    private final AtomicLong l1Miss = new AtomicLong();
    private final AtomicLong l2Hit = new AtomicLong();
    private final AtomicLong l2Miss = new AtomicLong();
    private final AtomicLong dbLoad = new AtomicLong();
    private final AtomicLong redisError = new AtomicLong();
    private final AtomicLong evict = new AtomicLong();
    private final AtomicLong evictAll = new AtomicLong();

    public void recordL1Hit() { l1Hit.incrementAndGet(); }
    public void recordL1Miss() { l1Miss.incrementAndGet(); }
    public void recordL2Hit() { l2Hit.incrementAndGet(); }
    public void recordL2Miss() { l2Miss.incrementAndGet(); }
    public void recordDbLoad() { dbLoad.incrementAndGet(); }
    public void recordRedisError() { redisError.incrementAndGet(); }
    public void recordEvict() { evict.incrementAndGet(); }
    public void recordEvictAll() { evictAll.incrementAndGet(); }

    public long getL1Hit() { return l1Hit.get(); }
    public long getL1Miss() { return l1Miss.get(); }
    public long getL2Hit() { return l2Hit.get(); }
    public long getL2Miss() { return l2Miss.get(); }
    public long getDbLoad() { return dbLoad.get(); }
    public long getRedisError() { return redisError.get(); }
    public long getEvict() { return evict.get(); }
    public long getEvictAll() { return evictAll.get(); }

    public String summary() {
        return "CacheMetrics{L1Hit=" + l1Hit + ", L1Miss=" + l1Miss
                + ", L2Hit=" + l2Hit + ", L2Miss=" + l2Miss
                + ", dbLoad=" + dbLoad + ", redisError=" + redisError
                + ", evict=" + evict + ", evictAll=" + evictAll + "}";
    }
}