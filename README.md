# Spring Boot Demo

一个**前后端分离**的全栈示例项目（monorepo 单仓库）：Spring Boot 3.5 后端 + Vue 3 前端 + FastAPI 复刻后端。支持 JPA/MyBatis 双持久层共存、JWT 鉴权、RBAC 权限管理、Docker 一键部署（应用 + MySQL）。

## 子工程

- **`spring-boot/`** — Spring Boot 3.5.16（Java 21）后端：RESTful API、JPA + MyBatis 双持久层、JWT 鉴权、RBAC、Knife4j 文档
- **`vue/`** — Vue 3 + Vite + Element Plus 前端（用户管理界面，hash 路由）
- **`fastapi-app/`** — FastAPI（Python 3.11 + uv）复刻版后端，接口与 Java 版完全一致
- **`deploy/`** — Docker 部署（应用 + MySQL，docker compose 编排）

## 快速开始

**本地开发**（dev，H2 内存库开箱即用）：

```bash
cd spring-boot && ./mvnw spring-boot:run   # 后端 → http://localhost:9090
cd vue && npm install && npm run dev       # 前端 → http://localhost:5173
```

**Docker 部署**（prod，MySQL）：

```bash
docker volume create spring-boot-demo-mysql-data   # 首次部署执行一次
cd vue && npm run build
cd deploy && docker compose up -d --build          # 应用 8080 + MySQL 3306
```

首次访问需注册/登录（默认管理员 **admin / 123456**）。

## 核心特性

- 统一响应 `{code, message, data}`、全局异常处理、参数校验
- JWT 登录鉴权（24h 有效、退出黑名单、BCrypt 密码）
- RBAC 五表权限模型（角色/权限 CRUD + 级联清理）
- JPA + MyBatis 双持久层共存（共享同一张表，接口等价）
- 建表由 `schema.sql` / `data.sql` 启动时幂等执行（不依赖 JPA ddl-auto）
- dev 彩色日志、前端 hash 路由

## 文档

技术栈、架构设计、完整部署步骤、API 接口清单、常见问题等详细内容见 **[项目 Wiki](https://github.com/wzklhk/spring-boot-demo/wiki)**。

## License

MIT
