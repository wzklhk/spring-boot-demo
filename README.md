# Spring Boot Demo

一个**前后端分离**的全栈示例项目（monorepo 单仓库）：

- **`backend/`** — Spring Boot 3.5.16（Java 21），RESTful API、JPA + MyBatis 双持久层共存、参数校验、全局异常处理
- **`frontend/`** — Vue 3 + Vite + Element Plus，用户管理界面（JPA / MyBatis 两套 API 均可操作）

## 技术栈

| 端 | 技术 | 版本 | 说明 |
|----|------|------|------|
| 后端 | Spring Boot | 3.5.16 | 基于 Jakarta EE，需 Java 21 |
| 后端 | Spring Data JPA | - | ORM 框架，负责建表 |
| 后端 | MyBatis | 3.0.4 | SQL Mapper 框架，与 JPA 共存 |
| 后端 | H2 Database | - | 内存数据库（dev profile，开箱即用） |
| 后端 | MySQL | 8.x | 生产数据库（prod profile，Docker 部署自带） |
| 后端 | Lombok | - | 简化 Java 代码 |
| 前端 | Vue | 3.x | 组合式 API |
| 前端 | Vite | 6.x | 构建工具 + 开发代理 |
| 前端 | Element Plus | 2.x | UI 组件库（Vue 3 版 Element UI） |

## 项目结构

```
spring-boot-demo/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml                       # Maven 配置
│   ├── mvnw / mvnw.cmd               # Maven Wrapper
│   ├── .mvn/wrapper/
│   └── src/
│       ├── main/
│       │   ├── java/com/example/demo/
│       │   │   ├── DemoApplication.java            # 启动类（@MapperScan）
│       │   │   ├── common/Result.java              # 统一响应包装 {code, message, data, errors}
│       │   │   ├── auth/                           # JWT 认证（Spring Security）
│       │   │   │   ├── AuthController.java         # 注册/登录/退出/注销账号
│       │   │   │   ├── SecurityConfig.java         # 安全配置（BCrypt/放行规则/401 JSON）
│       │   │   │   ├── JwtService.java             # JWT 生成/解析/黑名单
│       │   │   │   ├── JwtAuthenticationFilter.java
│       │   │   │   ├── CustomUserDetailsService.java
│       │   │   │   ├── dto/                        # LoginRequest / RegisterRequest
│       │   │   │   └── vo/AuthResponse.java        # {token, user} 响应
│       │   │   ├── controller/
│       │   │   │   ├── UserController.java         # 用户接口（JPA 版）
│       │   │   │   ├── MyBatisUserController.java  # 用户接口（MyBatis 版）
│       │   │   │   ├── RoleController.java         # 角色 CRUD（RBAC）
│       │   │   │   ├── PermissionController.java   # 权限 CRUD（RBAC）
│       │   │   │   └── RbacController.java         # 用户-角色/角色-权限/聚合查询（RBAC）
│       │   │   ├── service/
│       │   │   │   ├── UserService.java            # 业务层（JPA）
│       │   │   │   ├── UserMyBatisService.java     # 业务层（MyBatis）
│       │   │   │   ├── RoleService.java            # 角色业务（含级联清理）
│       │   │   │   ├── PermissionService.java      # 权限业务（含级联清理）
│       │   │   │   └── RbacService.java            # 关联分配/移除 + 聚合查询
│       │   │   ├── repository/                     # Spring Data JPA 仓库
│       │   │   │   ├── UserRepository.java
│       │   │   │   ├── RoleRepository.java
│       │   │   │   ├── PermissionRepository.java
│       │   │   │   ├── UserRoleRepository.java
│       │   │   │   └── RolePermissionRepository.java
│       │   │   ├── mapper/                         # MyBatis Mapper 接口
│       │   │   │   ├── UserMapper.java
│       │   │   │   └── RbacMapper.java             # 用户权限 join 聚合查询
│       │   │   ├── entity/
│       │   │   │   ├── User.java                   # 用户实体
│       │   │   │   ├── Role.java                   # 角色实体（RBAC）
│       │   │   │   ├── Permission.java             # 权限实体（RBAC）
│       │   │   │   ├── UserRole.java               # 用户-角色关联（RBAC）
│       │   │   │   └── RolePermission.java         # 角色-权限关联（RBAC）
│       │   │   ├── vo/UserPermissionVO.java        # 用户权限视图对象（含角色来源）
│       │   │   └── exception/GlobalExceptionHandler.java
│       │   └── resources/
│       │       ├── application.yml                 # 公共配置（profile 拆分见下）
│       │       ├── application-dev.yml             # 开发环境（H2）
│       │       ├── application-prod.yml            # 生产环境（MySQL）
│       │       ├── mapper/
│       │       │   ├── UserMapper.xml              # MyBatis SQL 映射
│       │       │   └── RbacMapper.xml              # RBAC 多表 join SQL
│       │       └── static/                         # 前端构建产物（gitignore）
│       └── test/...
└── frontend/                         # Vue 3 前端
    ├── package.json
    ├── vite.config.js                # dev 代理 /api → 8080；build 输出到 backend static
    ├── index.html
    └── src/
        ├── main.js                   # 入口（Element Plus 全量注册 + 中文语言包）
        ├── App.vue                   # 布局：深色 header + 左侧导航 + footer
        ├── router/index.js
        ├── api/user.js               # axios 封装（统一处理 {code,message,data}）
        └── views/
            ├── HomeView.vue          # 首页（项目介绍）
            ├── UsersView.vue         # 用户管理（JPA）
            └── MybatisUsersView.vue  # 用户管理（MyBatis，含按用户名查询）
```

## 双持久层设计（JPA + MyBatis）

JPA 与 MyBatis 共享同一 H2 数据源和同一张 `users` 表：

- **JPA** 负责建表（`ddl-auto: update`），通过 `UserRepository` 操作数据，接口前缀 `/api/users`
- **MyBatis** 通过 `UserMapper`（XML Mapper）读写同一张表，接口前缀 `/api/mybatis/users`
- 两套 API 功能完全等价，可在前端页面分别操作、交叉验证
- 事务统一由 Spring 管理（`@Transactional` 同时覆盖 JPA 与 MyBatis）
- 注意：MyBatis 写入不触发 JPA 的 `@PrePersist` 回调，`UserMyBatisService` 手动填充 `createdAt` / `updatedAt`

## 快速开始

### 环境要求

- JDK 21+（构建后端时需 `JAVA_HOME` 指向 JDK 21）
- Node.js 18+（前端）
- Docker 24+ / Docker Compose v2（方式三 Docker 部署用）

### Profile 配置说明

后端按环境拆分三份配置（Spring Boot Profile 机制）：

| 文件 | 环境 | 说明 |
|------|------|------|
| `application.yml` | 公共 | 端口、应用名、MyBatis 公共配置；`spring.profiles.default: dev` |
| `application-dev.yml` | 开发 | H2 内存库 + SQL 日志全开 + H2 控制台 |
| `application-prod.yml` | 生产 | MySQL + 连接池 + 关闭调试 + INFO 日志 |

- 本地直接运行（方式一/二）默认激活 **dev**，无需任何参数
- 生产环境激活：环境变量 `SPRING_PROFILES_ACTIVE=prod`，或启动参数 `--spring.profiles.active=prod`
- 数据源连接信息可用环境变量覆盖：`SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`

### 方式一：开发模式（前后端分离，热更新）

```bash
# 终端 1：启动后端（8080）
cd backend
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvnw spring-boot:run

# 终端 2：启动前端 dev server（5173，代理 /api → 8080）
cd frontend
npm install
npm run dev
```

浏览器访问 **http://localhost:5173**，前端请求自动代理到后端，改前端代码即时热更新。**首次访问需注册/登录**（JWT 鉴权，未登录自动跳转登录页）。

> 也可以不启 Vite dev server：dev profile 的 Spring 直接托管 `frontend/dist`（`static-locations: file:../frontend/dist/`），先 `cd frontend && npm run build` 再只起后端，访问 **http://localhost:8080** 即可看到页面。

### 方式二：生产模式（单 jar 部署）

前端产物**不打进 jar**，由 Spring 通过 `file:` 路径从 jar 外读取：

```bash
# 1. 构建前端（产物输出到 frontend/dist/）
cd frontend
npm run build

# 2. 打包后端（jar 仅含后端，不含前端静态资源）
cd ../backend
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvnw -DskipTests package

# 3. 运行（工作目录 backend/，Spring 从 ../frontend/dist 托管前端页面）
java -jar target/spring-boot-demo-1.0.0.jar
```

浏览器访问 **http://localhost:8080**，同一个端口同时提供前端页面和 API（页面来自 `frontend/dist`，改前端重新 `npm run build` 即生效，无需重启后端）。

### 方式三：Docker 部署（推荐）

无需本机安装 JDK / Node。所有部署文件集中在 `deploy/` 目录，每个组件一个独立 compose 文件（自包含：服务/端口/卷/健康检查），由总入口 `docker-compose.yaml`（`include` 聚合）编排——**既可一键部署全部组件，也可单独部署单个组件**：

```
deploy/
├── docker-compose.yaml          # 总入口：include 聚合所有组件
├── docker-compose.mysql.yml     # MySQL（公共组件，宿主机 3306）
├── docker-compose.app.yml       # 应用（宿主机 8081）
└── Dockerfile                   # 应用多阶段构建（build context = 项目根）
```

```bash
# 1. 构建前端（输出 frontend/dist，部署时挂载进容器 /app/frontend）
cd frontend
npm run build

# 2. 一键部署全部组件（在 deploy/ 目录执行）
cd deploy
docker compose up -d --build
```

浏览器访问 **http://localhost:8081**。

> 应用容器自动激活 **prod profile**（`static-locations: file:/app/frontend/,classpath:/static/`，前端从挂载卷读取，jar 内不含前端）；MySQL 数据持久化在外部命名卷 `spring-boot-demo-mysql-data`（`external: true`，compose 不管理其生命周期，容器重建不丢数据）。
>
> **改前端只需**：`cd frontend && npm run build`，刷新浏览器即生效，无需重建后端镜像/重启容器。
>
> **端口规划**：生产宿主机 应用 `8081`（容器内 8080）；MySQL 用标准端口 `3306`（公共组件，本地开发/其他项目共用）；本地开发 后端 `8080`（H2，无数据库端口）/ 前端 Vite `5173`。

常用命令（在 `deploy/` 目录执行，不写 `-f` 即默认用 `docker-compose.yaml`）：

```bash
docker compose up -d --build   # 一键部署全部
docker compose ps              # 查看所有容器
docker compose logs -f app     # 跟踪应用日志
docker compose down            # 停止并移除全部容器（数据卷保留）
```

> ⚠️ `down -v` 不会删除 MySQL 外部卷（`external: true`），如需清空数据手动执行 `docker volume rm spring-boot-demo-mysql-data`。

单独部署单个组件（在 `deploy/` 目录执行）：

```bash
docker compose -f docker-compose.mysql.yml up -d   # 只启动 MySQL
docker compose -f docker-compose.mysql.yml down    # 停止 MySQL 容器
docker compose -f docker-compose.app.yml up -d     # 只启动应用
docker compose -f docker-compose.app.yml down      # 停止应用容器
```

**新增组件三步**（如 Redis、RabbitMQ 等中间件）：
1. 在 `deploy/` 新建 `docker-compose.<组件>.yml`（自包含：服务/端口/卷/健康检查，端口映射到宿主机）
2. 在 `deploy/docker-compose.yaml` 的 `include` 列表加一行
3. `cd deploy && docker compose up -d` 即可，也可单独 `-f docker-compose.<组件>.yml up -d`

> **组件间通信**：不建共享网络，所有组件通过「宿主机地址:端口」互通——公共组件映射标准端口到宿主机（MySQL 3306、Redis 6379…），应用容器内用 `host.docker.internal` 访问宿主机（compose 的 `extra_hosts: host-gateway` 已配好，见 app yaml）。组件 yaml 互不引用、无强耦合；即使中间件不在 Docker 里（宿主机原生进程），应用也能照常连接。
>
> 单独 `-f` 部署时其他组件的容器会报 orphan 警告，属正常，勿加 `--remove-orphans`（会误删）。

工作原理（`deploy/Dockerfile` 多阶段构建）：

1. **node:20-alpine** — 执行 `npm run build`，前端产物输出到 `backend/src/main/resources/static/`
2. **maven:3.9-eclipse-temurin-21** — 注入前端产物后 `./mvnw package`，打成单 jar
3. **eclipse-temurin:21-jre-alpine** — 仅运行 `java -jar`，镜像最小化

注意事项：

- **使用 DBeaver / Navicat 等客户端连接 MySQL**：宿主机 `localhost:3306`，库 `springboot_demo`，用户 `springboot` / 密码 `springboot123`（或 root / `root123`）。认证插件已改为 `mysql_native_password`——MySQL 8 默认 `caching_sha2_password` 会让 JDBC 客户端报 `Public Key Retrieval is not allowed`；若连接其他 MySQL 实例遇到该错误，在连接设置里加 `allowPublicKeyRetrieval=true` 即可（本项目 app 的 JDBC URL 已带此参数）
- **生产模式使用 MySQL**：数据保存在外部卷 `spring-boot-demo-mysql-data`，容器重启/重建不丢数据；compose 不删外部卷，清空数据需手动 `docker volume rm spring-boot-demo-mysql-data`
- 数据库账号密码为 demo 默认值（`root123` / `springboot123`），正式部署务必在 `deploy/docker-compose.mysql.yml` 中修改
- 首次构建需下载 Maven 依赖、npm 包与 MySQL 镜像，耗时较长属正常
- 两个 compose 文件均已配置 `healthcheck`（应用探测首页 `/`，MySQL 探测 `mysqladmin ping`）与 `restart: unless-stopped`
- 端口冲突时修改对应 compose 文件中 `ports` 的宿主机侧端口（当前生产映射：app `8081:8080`、mysql `3306:3306`）

### H2 数据库控制台

- 地址：http://localhost:8080/h2-console
- JDBC URL：`jdbc:h2:mem:demodb`
- 用户名：`sa`，密码：（空）

## API 接口

两套等价接口：`/api/users`（JPA）与 `/api/mybatis/users`（MyBatis），统一响应格式 `{code, message, data}`。

| 方法 | JPA 路径 | MyBatis 路径 | 说明 |
|------|----------|--------------|------|
| GET | `/api/users` | `/api/mybatis/users` | 查询所有用户 |
| GET | `/api/users/{id}` | `/api/mybatis/users/{id}` | 按 ID 查询 |
| GET | - | `/api/mybatis/users/username/{username}` | 按用户名查询（MyBatis 专属） |
| POST | `/api/users` | `/api/mybatis/users` | 创建用户 `{username, email}` |
| PUT | `/api/users/{id}` | `/api/mybatis/users/{id}` | 更新用户 |
| DELETE | `/api/users/{id}` | `/api/mybatis/users/{id}` | 删除用户（级联清理用户-角色） |

### 认证接口（JWT 登录鉴权）

密码使用 **BCrypt 加密**存储（永不明文/不随接口返回）。登录/注册成功返回 `{token, tokenType, expiresIn, user}`，后续请求在 `Authorization: Bearer <token>` 头携带 token；除 `/api/auth/login|register|logout` 外，所有 `/api/**` 接口都需要有效 token（401 返回 Result 包装的 JSON）。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 `{username, email, password}`（密码 ≥6 位，注册即登录） |
| POST | `/api/auth/login` | 登录 `{username, password}`，返回 JWT |
| POST | `/api/auth/logout` | 退出登录（token 加入黑名单立即失效） |
| DELETE | `/api/auth/account` | 注销账号（删除当前用户及角色关联，需登录） |

### RBAC 权限管理接口（用户分权）

经典 RBAC 五表：`users` / `roles` / `permissions` + 关联表 `user_roles` / `role_permissions`。所有接口返回统一 `Result` 包装 `{code, message, data}`。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/roles` | 角色列表 |
| GET | `/api/roles/{id}` | 角色详情 |
| POST | `/api/roles` | 创建角色 `{code, name, description}`（code 唯一） |
| PUT | `/api/roles/{id}` | 更新角色 |
| DELETE | `/api/roles/{id}` | 删除角色（级联清理关联） |
| GET | `/api/permissions` | 权限列表 |
| GET | `/api/permissions/{id}` | 权限详情 |
| POST | `/api/permissions` | 创建权限 `{code, name, description}`（code 唯一） |
| PUT | `/api/permissions/{id}` | 更新权限 |
| DELETE | `/api/permissions/{id}` | 删除权限（级联清理关联） |
| GET | `/api/users/{userId}/roles` | 查询用户的角色列表 |
| POST | `/api/users/{userId}/roles/{roleId}` | 给用户分配角色（幂等） |
| DELETE | `/api/users/{userId}/roles/{roleId}` | 移除用户角色 |
| GET | `/api/roles/{roleId}/permissions` | 查询角色的权限列表 |
| POST | `/api/roles/{roleId}/permissions/{permissionId}` | 给角色分配权限（幂等） |
| DELETE | `/api/roles/{roleId}/permissions/{permissionId}` | 移除角色权限 |
| GET | `/api/users/{userId}/permissions` | 用户的全部权限（MyBatis 多表 join，含角色来源） |

权限编码约定：`资源:动作`，如 `user:create` / `role:manage`。删除用户/角色/权限时自动清理关联数据；重复分配幂等返回成功。

## 核心特性

- **统一响应格式**：所有接口返回 `Result` 包装 `{code, message, data}`（`common/Result.java`）
- **JWT 登录鉴权**：注册/登录返回 JWT（24h 有效），`/api/**` 需 `Authorization: Bearer` 头；退出登录 token 进黑名单；密码 BCrypt 加密存储，永不泄露
- **RBAC 用户分权**：角色/权限 CRUD + 用户-角色、角色-权限分配（幂等）+ 用户权限聚合查询（MyBatis join），删除自动级联清理关联
- **参数校验**：`@Valid` + Jakarta Validation（`@NotBlank` / `@Email`），失败返回 400 及字段错误
- **全局异常处理**：`@RestControllerAdvice` 统一捕获，避免堆栈泄露
- **自动时间戳**：JPA 实体 `@PrePersist` / `@PreUpdate`；MyBatis 服务层手动填充
- **业务唯一性校验**：用户名、邮箱唯一（两套持久层各自实现）
- **前端主题**：深色 header（#1a1a2e）+ Vue 绿（#41b883）主色 + 左侧导航 + Element Plus 中文语言包

## 常见问题

**Q: 前端 dev server 请求 404？**
A: 确认后端已在 8080 启动，Vite 代理将 `/api` 转发到 `http://localhost:8080`。

**Q: 如何切换到 MySQL？**
A: 激活 prod profile 即可：环境变量 `SPRING_PROFILES_ACTIVE=prod` 或启动参数 `--spring.profiles.active=prod`，连接信息用 `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` 覆盖（默认连本机 3306 的 root/root）。详见「Profile 配置说明」。

**Q: 端口被占用？**
A: 后端端口在 `application.yml` 的 `server.port` 修改；前端端口在 `vite.config.js` 的 `server.port` 修改。

**Q: 前端构建产物被 git 忽略？**
A: 是的。`backend/src/main/resources/static/` 是构建产物（gitignore），源码只需提交 `frontend/`，部署时执行 `npm run build` 即可。

## License

MIT
