package com.example.demo.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** 订阅 Redis 失效广播：其他 JVM 写库后，本 JVM 只清理本地 L1，不触发二次广播 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationListener implements MessageListener {

    private final MultiLevelCache cache;
    private final CacheObjectMapper cacheObjectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            CacheInvalidationMessage invalidation = cacheObjectMapper.read(payload, CacheInvalidationMessage.class);
            if (CacheInvalidationMessage.ACTION_EVICT_ALL.equals(invalidation.getAction())) {
                cache.evictAllLocal(invalidation.getCacheName());
            } else {
                cache.evictLocal(invalidation.getCacheName(), invalidation.getKey());
            }
        } catch (RuntimeException e) {
            log.warn("处理缓存失效广播失败: {}", e.toString());
        }
    }
}