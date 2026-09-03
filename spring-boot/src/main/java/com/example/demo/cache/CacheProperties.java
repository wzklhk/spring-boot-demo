package com.example.demo.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 多级缓存配置（前缀 cache.*）。
 * 支持：全局开关、L1/L2 独立开关、失效 channel、按实体独立配置。
 */
@Data
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    /** 全局总开关：false 时所有缓存逻辑直接退化为 DB 访问 */
    private boolean enabled = true;

    private L1 l1 = new L1();

    private L2 l2 = new L2();

    /** Redis Pub/Sub 失效广播 channel（多 JVM L1 同步） */
    private String channel = "sb:cache:invalidation";

    /** 按实体/读模型名称的配置（role / permission / user / user-roles / ...） */
    private Map<String, EntityCacheProperties> entities = new LinkedHashMap<>();

    /** 返回实体的缓存配置；实体未配置或已禁用时返回 null（表示不缓存该实体） */
    public EntityCacheProperties entity(String name) {
        EntityCacheProperties cfg = entities.get(name);
        return (cfg != null && cfg.isEnabled()) ? cfg : null;
    }

    @Data
    public static class L1 {
        private boolean enabled = true;
    }

    @Data
    public static class L2 {
        private boolean enabled = true;
    }
}