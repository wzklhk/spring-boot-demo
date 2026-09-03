package com.example.demo.cache;

import lombok.Data;

import java.time.Duration;

/** 单个实体/读模型的缓存配置 */
@Data
public class EntityCacheProperties {

    private boolean enabled = true;

    /** L1（Caffeine）最大条目数 */
    private long maximumSize = 1000;

    /** L1 TTL */
    private Duration ttl = Duration.ofMinutes(5);

    /** L2（Redis）TTL */
    private Duration l2Ttl = Duration.ofMinutes(30);
}