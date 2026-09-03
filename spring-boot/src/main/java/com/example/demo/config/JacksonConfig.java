package com.example.demo.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 时间序列化统一为世界时间（UTC）输出。
 *
 * <p>实体/VO 中的 LocalDateTime 一律按 UTC 存储（见 {@code pojo.entity.BaseDO}），
 * 这里让所有 LocalDateTime 以 ISO-8601 带 Z 的形式输出
 * （如 {@code 2026-09-02T04:08:08Z}），前端/调用方解析后按自身时区显示本地时间，
 * 避免“库里的 UTC 时间被当成本地时间原样展示”的问题。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer utcLocalDateTimeCustomizer() {
        return builder -> builder.serializerByType(LocalDateTime.class, new UtcLocalDateTimeSerializer());
    }

    /** 将按 UTC 存储的 LocalDateTime 序列化为带 Z 的 ISO-8601 字符串 */
    static class UtcLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(FORMATTER.format(value.atOffset(ZoneOffset.UTC)));
        }
    }
}
