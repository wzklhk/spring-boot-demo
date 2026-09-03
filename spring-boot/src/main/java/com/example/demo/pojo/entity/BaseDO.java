package com.example.demo.pojo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import java.time.LocalDateTime;

/**
 * 实体公共基类（DO 基类）—— 主键 + 创建/修改时间。
 *
 * <p>时间约定（统一世界时间 UTC）：
 * <ul>
 *   <li>存储：created_at / updated_at 由数据库 DEFAULT CURRENT_TIMESTAMP /
 *       ON UPDATE CURRENT_TIMESTAMP 生成，数据库会话时区固定为 UTC
 *       （dev H2 通过 INIT=SET TIME ZONE 'UTC'，prod MySQL 通过
 *       connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true）；</li>
 *   <li>输出：Jackson 将 LocalDateTime 一律按 UTC（ISO-8601 带 Z）序列化
 *       （见 {@code config/JacksonConfig}），前端/调用方解析后按本地时区显示。</li>
 * </ul>
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 创建时间（UTC），由数据库 DEFAULT CURRENT_TIMESTAMP 自动生成，应用侧不写入 */
    @Column(name = "created_at", updatable = false)
    @Generated(GenerationTime.INSERT)
    private LocalDateTime createdAt;

    /** 修改时间（UTC），由数据库 ON UPDATE CURRENT_TIMESTAMP 自动刷新，应用侧不写入 */
    @Column(name = "updated_at")
    @Generated(GenerationTime.ALWAYS)
    private LocalDateTime updatedAt;
}
