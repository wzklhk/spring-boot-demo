package com.example.demo.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis 专用的 JSON 序列化器（与 HTTP 输出解耦）。
 * LocalDateTime 按 ISO-8601 无时区偏移的本地值序列化，读取时可直接还原；
 * HTTP 层仍由 JacksonConfig 输出带 Z 的 UTC 字符串。
 */
@Component
public class CacheObjectMapper {

    private final ObjectMapper mapper;

    public CacheObjectMapper() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("缓存 JSON 序列化失败", e);
        }
    }

    public <T> T read(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("缓存 JSON 反序列化失败: " + type.getSimpleName(), e);
        }
    }

    public <T> List<T> readList(String json, Class<T> elementType) {
        JavaType javaType = mapper.getTypeFactory().constructCollectionType(List.class, elementType);
        try {
            return mapper.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("缓存 JSON 反序列化失败: List<" + elementType.getSimpleName() + ">", e);
        }
    }
}