package com.example.demo.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Redis Pub/Sub 失效事件载体 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheInvalidationMessage {

    public static final String ACTION_EVICT = "EVICT";
    public static final String ACTION_EVICT_ALL = "EVICT_ALL";

    /** 缓存名（实体/读模型名，如 role / user-roles） */
    private String cacheName;

    /** 逻辑 key（如主键 ID）；EVICT_ALL 时可为空 */
    private String key;

    private String action;
}