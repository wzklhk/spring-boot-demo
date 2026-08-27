-- ============================================================
-- MySQL 建表脚本（prod 环境）
-- 由 spring.sql.init 在启动时自动执行（spring.sql.init.mode: always），
-- 命名遵循 Spring Boot 约定：schema-{platform}.sql（platform=mysql）
-- 幂等：全部使用 IF NOT EXISTS，可重复执行（首次启动建表，后续启动跳过）
-- 字段与 JPA 实体（User/Role/Permission/UserRole/RolePermission）严格对应
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(100) NOT NULL,
    created_at  DATETIME,
    updated_at  DATETIME,
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    created_at  DATETIME,
    updated_at  DATETIME,
    CONSTRAINT uk_roles_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permissions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(128) NOT NULL,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    created_at  DATETIME,
    updated_at  DATETIME,
    CONSTRAINT uk_permissions_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    role_id    BIGINT NOT NULL,
    created_at DATETIME,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permissions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at    DATETIME,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
