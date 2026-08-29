package com.example.demo.config;

import com.github.xiaoymin.knife4j.core.conf.GlobalConstants;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jSetting;
import com.github.xiaoymin.knife4j.spring.extension.Knife4jOpenApiCustomizer;
import com.github.xiaoymin.knife4j.spring.extension.OpenApiExtensionResolver;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * knife4j 4.5.0 × springdoc 2.8.x 兼容层（2026-08 实测）。
 *
 * <p>问题：springdoc 2.7+ 移除了 {@code SpringDocConfigProperties.getGroupConfigs()}（List 返回版本），
 * 而 knife4j 4.5.0 的 {@code Knife4jOpenApiCustomizer.addOrderExtension()} 依赖它 →
 * 生成 /v3/api-docs 时 NoSuchMethodError。
 *
 * <p>解法：knife4j 自动配置对 {@code knife4jOpenApiCustomizer} bean 标了
 * {@code @ConditionalOnMissingBean}（按返回类型匹配），这里用同类型 bean 覆盖它，
 * 完整保留原逻辑（x-openapi 设置扩展 + Markdown 文档），仅跳过 addOrderExtension
 * （其作用只是按 @ApiSupport(order) 给 tag 加 x-order 排序，跳过只影响 UI 排序）。
 */
@Configuration
// 仅当 knife4j 启用时才注入该兼容层（prod 中 knife4j.enable=false → 整个配置跳过）：
// 否则 Knife4jProperties bean 不存在（knife4j 自动配置按 knife4j.enable 条件装配），
// 这里无条件依赖它会直接导致 prod 启动失败（APPLICATION FAILED TO START）
@ConditionalOnProperty(name = "knife4j.enable", havingValue = "true", matchIfMissing = true)
public class Knife4jCompatConfig {

    @Bean
    public Knife4jOpenApiCustomizer knife4jOpenApiCustomizer(Knife4jProperties knife4jProperties,
                                                             SpringDocConfigProperties docProperties) {
        return new Knife4jOpenApiCustomizer(knife4jProperties, docProperties) {
            @Override
            public void customise(OpenAPI openApi) {
                if (knife4jProperties.isEnable()) {
                    Knife4jSetting setting = knife4jProperties.getSetting();
                    OpenApiExtensionResolver openApiExtensionResolver =
                            new OpenApiExtensionResolver(setting, knife4jProperties.getDocuments());
                    openApiExtensionResolver.start();
                    Map<String, Object> objectMap = new HashMap<>();
                    objectMap.put(GlobalConstants.EXTENSION_OPEN_SETTING_NAME, setting);
                    objectMap.put(GlobalConstants.EXTENSION_OPEN_MARKDOWN_NAME,
                            openApiExtensionResolver.getMarkdownFiles());
                    openApi.addExtension(GlobalConstants.EXTENSION_OPEN_API_NAME, objectMap);
                    // 原 addOrderExtension() 依赖已移除的 getGroupConfigs()，跳过（仅影响 UI 排序）
                }
            }
        };
    }
}
