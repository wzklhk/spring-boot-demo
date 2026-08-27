# spring-boot-demo 的 FastAPI 复刻版

用 FastAPI 完整复刻 `backend/`（Spring Boot 3.5 + JPA + MyBatis + JWT + RBAC）的接口，
路径、请求/响应结构与 Java 版**完全一致**，可直接对比验证。

## 技术对照

| Spring Boot (Java) | FastAPI (Python) |
|---|---|
| JPA EntityManager / Repository | SQLAlchemy 2.x ORM |
| MyBatis XML Mapper | SQLAlchemy 原生 SQL（`text()`） |
| H2 内存库 | SQLite（默认 `fastapi_demo.db`） |
| Spring Security + JwtService | FastAPI 依赖注入 + PyJWT + 黑名单 |
| BCryptPasswordEncoder | `bcrypt` |
| Result\<T\> 统一包装 | Pydantic `Result` 泛型模型 |
| knife4j /doc.html | FastAPI 自带 Swagger UI（/docs） |
| @Valid + GlobalExceptionHandler | Pydantic 校验 + 全局异常处理器 |

## 快速开始

```bash
cd fastapi-app
uv sync            # 安装依赖（或 uv venv && uv pip install -e .）
uv run python -m app.seed    # 可选：初始化 admin/123456 + RBAC 种子数据
uv run uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

- Swagger 文档：http://localhost:8000/docs
- OpenAPI JSON：http://localhost:8000/openapi.json
- 健康检查：http://localhost:8000/api/health

## 接口清单（与 Java 版一致）

**认证** `/api/auth`（无需 token）
- `POST /register` → 201，返回 token（注册即登录）
- `POST /login` → 200，返回 token
- `POST /logout`（需 token）→ token 加入黑名单
- `DELETE /account`（需 token）→ 注销当前账号

**用户** `/api/users`（JPA 版，需 token）
- `GET /` `GET /{id}` `POST /` `PUT /{id}` `DELETE /{id}`

**用户(MyBatis)** `/api/mybatis/users`（原生 SQL 版，功能等价，需 token）
- 同上 + `GET /username/{username}`

**角色** `/api/roles`、**权限** `/api/permissions`（需 token）
- 标准 CRUD；code 唯一校验

**RBAC 关联**（需 token）
- `GET/POST/DELETE /api/users/{userId}/roles[/{roleId}]`
- `GET/POST/DELETE /api/roles/{roleId}/permissions[/{permissionId}]`
- `GET /api/users/{userId}/permissions`（多表 join 聚合，含角色来源）

## 行为细节（与 Java 版逐条对齐）

- 响应统一 `{code, message, data, errors?}`；200/201/400/401 语义一致
- 密码 BCrypt 存储，永不出现在响应中；未传密码默认 `123456`
- JWT：`sub`=用户名、`uid`、`jti` 唯一；登出按 jti 进内存黑名单
- 创建时用户名/邮箱（用户）、code（角色/权限）查重 → 400
- 删除用户级联清理 user_roles；删除角色/权限级联清理关联表
- 校验失败 → 400 + `errors` 字段（Java 版 `MethodArgumentNotValidException` 行为）
- 登录密码错误 → 401 `用户名或密码错误`（不暴露内部细节）
- 配置：`JWT_SECRET` / `JWT_EXPIRATION` / `DATABASE_URL` 环境变量可覆盖（见 `app/config.py`）

## 目录结构

```
backend/fastapi-app/
├── pyproject.toml          # uv 项目定义
├── app/
│   ├── main.py             # 入口 + 全局异常处理
│   ├── config.py           # 配置（对应 application.yml）
│   ├── database.py         # 引擎/会话/建表（对应 JPA 基础设施）
│   ├── models.py           # ORM 模型（对应 JPA Entity）
│   ├── schemas.py          # Pydantic DTO/VO（对应 dto/vo + 校验）
│   ├── seed.py             # 可选种子数据
│   ├── core/
│   │   ├── result.py       # Result 包装（对应 common/Result）
│   │   ├── security.py     # JWT + BCrypt（对应 JwtService + PasswordEncoder）
│   │   └── deps.py         # 认证依赖（对应 JwtAuthenticationFilter）
│   ├── services/           # 业务层（对应 service 包）
│   │   ├── user_service.py          # JPA 等价（ORM）
│   │   ├── user_mybatis_service.py  # MyBatis 等价（原生 SQL）
│   │   ├── rbac_services.py         # 角色/权限
│   │   └── rbac_service.py          # 关联管理 + 聚合查询
│   └── routers/            # 路由层（对应 controller 包）
│       ├── auth.py  users.py  mybatis_users.py
│       ├── roles_permissions.py  rbac.py
└── README.md
```
