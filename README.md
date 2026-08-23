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
| 后端 | H2 Database | - | 内存数据库，开箱即用 |
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
│       │   │   ├── controller/
│       │   │   │   ├── UserController.java         # REST 控制器（JPA 版）
│       │   │   │   └── MyBatisUserController.java  # REST 控制器（MyBatis 版）
│       │   │   ├── service/
│       │   │   │   ├── UserService.java            # 业务层（JPA）
│       │   │   │   └── UserMyBatisService.java     # 业务层（MyBatis）
│       │   │   ├── repository/UserRepository.java  # Spring Data JPA 仓库
│       │   │   ├── mapper/UserMapper.java          # MyBatis Mapper 接口
│       │   │   ├── entity/User.java                # 实体类
│       │   │   └── exception/GlobalExceptionHandler.java
│       │   └── resources/
│       │       ├── application.yml                 # 应用配置
│       │       ├── mapper/UserMapper.xml           # MyBatis SQL 映射
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

浏览器访问 **http://localhost:5173**，前端请求自动代理到后端，改前端代码即时热更新。

### 方式二：生产模式（单 jar 部署）

```bash
# 1. 构建前端（产物输出到 backend/src/main/resources/static/）
cd frontend
npm run build

# 2. 打包后端（static 已包含前端，形成单个可部署 jar）
cd ../backend
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvnw -DskipTests package

# 3. 运行
java -jar target/springboot-demo-1.0.0.jar
```

浏览器访问 **http://localhost:8080**，同一个端口同时提供前端页面和 API。

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
| DELETE | `/api/users/{id}` | `/api/mybatis/users/{id}` | 删除用户 |

## 核心特性

- **统一响应格式**：所有接口返回 `{code, message, data}`
- **参数校验**：`@Valid` + Jakarta Validation（`@NotBlank` / `@Email`），失败返回 400 及字段错误
- **全局异常处理**：`@RestControllerAdvice` 统一捕获，避免堆栈泄露
- **自动时间戳**：JPA 实体 `@PrePersist` / `@PreUpdate`；MyBatis 服务层手动填充
- **业务唯一性校验**：用户名、邮箱唯一（两套持久层各自实现）
- **前端主题**：深色 header（#1a1a2e）+ Vue 绿（#41b883）主色 + 左侧导航 + Element Plus 中文语言包

## 常见问题

**Q: 前端 dev server 请求 404？**
A: 确认后端已在 8080 启动，Vite 代理将 `/api` 转发到 `http://localhost:8080`。

**Q: 如何切换到 MySQL？**
A: 修改 `backend/src/main/resources/application.yml` 中的 datasource 配置，并在 `backend/pom.xml` 添加 MySQL 驱动。

**Q: 端口被占用？**
A: 后端端口在 `application.yml` 的 `server.port` 修改；前端端口在 `vite.config.js` 的 `server.port` 修改。

**Q: 前端构建产物被 git 忽略？**
A: 是的。`backend/src/main/resources/static/` 是构建产物（gitignore），源码只需提交 `frontend/`，部署时执行 `npm run build` 即可。

## License

MIT
