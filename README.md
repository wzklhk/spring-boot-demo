# Spring Boot Demo

一个完整的 Spring Boot 示例项目，展示 RESTful API、JPA + MyBatis 双持久层共存、参数校验、全局异常处理等常用功能。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.16 | 基于 Jakarta EE，需 Java 17+ |
| Spring Data JPA | - | ORM 框架 |
| MyBatis | 3.0.4 | SQL Mapper 框架，与 JPA 共存 |
| H2 Database | - | 内存数据库，开箱即用 |
| Lombok | - | 简化 Java 代码 |
| Maven Wrapper | 3.2.0 | 无需预装 Maven |

## 双持久层设计（JPA + MyBatis）

本项目同时集成 JPA 和 MyBatis，两者共享同一个数据源和同一张 `users` 表：

- **JPA** 负责建表（`ddl-auto: update`），通过 `UserRepository`（Spring Data JPA）操作数据，接口前缀 `/api/users`。
- **MyBatis** 通过 `UserMapper`（XML Mapper，见 `src/main/resources/mapper/UserMapper.xml`）读写同一张表，接口前缀 `/api/mybatis/users`。
- 两套 API 功能完全等价，可通过 curl 交叉验证：JPA 写入的数据 MyBatis 能读到，反之亦然。
- 事务统一由 Spring 管理（`@Transactional` 同时覆盖 JPA 与 MyBatis 操作）。
- 注意：MyBatis 写入不触发 JPA 的 `@PrePersist` 回调，`UserMyBatisService` 中手动填充 `createdAt` / `updatedAt`。

## 项目结构

```
springboot-demo/
├── pom.xml                          # Maven 配置
├── mvnw / mvnw.cmd                 # Maven Wrapper 脚本
├── .mvn/wrapper/                    # Maven Wrapper 配置
└── src/
    ├── main/
    │   ├── java/com/example/demo/
    │   │   ├── DemoApplication.java          # 启动类（@MapperScan 扫描 MyBatis Mapper）
    │   │   ├── controller/
    │   │   │   ├── UserController.java       # REST 控制器（JPA 版）
    │   │   │   └── MyBatisUserController.java # REST 控制器（MyBatis 版）
    │   │   ├── service/
    │   │   │   ├── UserService.java          # 业务逻辑层（JPA）
    │   │   │   └── UserMyBatisService.java   # 业务逻辑层（MyBatis）
    │   │   ├── repository/
    │   │   │   └── UserRepository.java       # Spring Data JPA 仓库
    │   │   ├── mapper/
    │   │   │   └── UserMapper.java           # MyBatis Mapper 接口
    │   │   ├── entity/
    │   │   │   └── User.java                 # 实体类
    │   │   └── exception/
    │   │       └── GlobalExceptionHandler.java # 全局异常处理
    │   └── resources/
    │       ├── application.yml                # 应用配置
    │       └── mapper/
    │           └── UserMapper.xml            # MyBatis SQL 映射文件
    └── test/java/com/example/demo/
        └── DemoApplicationTests.java          # 测试类
```

## 快速开始

### 环境要求

- JDK 21+（LTS，已验证 Java 21）
- 无需预装 Maven（使用 Maven Wrapper）

### 运行项目

```bash
# 进入项目目录
cd springboot-demo

# 方式一：使用 Maven Wrapper 运行（推荐）
./mvnw spring-boot:run

# 方式二：先打包再运行
./mvnw package -DskipTests
java -jar target/springboot-demo-1.0.0.jar
```

启动成功后访问：http://localhost:8080

### H2 数据库控制台

- 地址：http://localhost:8080/h2-console
- JDBC URL：`jdbc:h2:mem:demodb`
- 用户名：`sa`
- 密码：（空）

## API 接口

两套等价接口：`/api/users`（JPA）与 `/api/mybatis/users`（MyBatis）。下方以 JPA 版为例，MyBatis 版路径前缀替换为 `/api/mybatis/users` 即可（额外提供 `GET /api/mybatis/users/username/{username}` 按用户名查询）。

### 1. 查询所有用户

```
GET /api/users
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "username": "zhangsan",
      "email": "zhangsan@example.com",
      "createdAt": "2026-08-20T10:00:00",
      "updatedAt": "2026-08-20T10:00:00"
    }
  ]
}
```

### 2. 根据 ID 查询用户

```
GET /api/users/{id}
```

### 3. 创建用户

```
POST /api/users
Content-Type: application/json

{
  "username": "zhangsan",
  "email": "zhangsan@example.com"
}
```

### 4. 更新用户

```
PUT /api/users/{id}
Content-Type: application/json

{
  "username": "zhangsan_updated",
  "email": "new@example.com"
}
```

### 5. 删除用户

```
DELETE /api/users/{id}
```

## curl 测试示例

```bash
# 创建用户
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","email":"zhangsan@example.com"}'

# 查询所有
curl http://localhost:8080/api/users

# 查询单个
curl http://localhost:8080/api/users/1

# 更新
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"username":"lisi","email":"lisi@example.com"}'

# 删除
curl -X DELETE http://localhost:8080/api/users/1
```

## 核心特性说明

### 1. 统一响应格式
所有接口返回统一的 JSON 结构：`{code, message, data}`

### 2. 参数校验
使用 `@Valid` + `jakarta.validation` 注解进行参数校验：
- `@NotBlank` - 非空校验
- `@Email` - 邮箱格式校验

校验失败时返回 400 状态码及具体字段错误信息。

### 3. 全局异常处理
`@RestControllerAdvice` 统一捕获异常，避免堆栈信息泄露。

### 4. 自动时间戳
实体类使用 `@PrePersist` / `@PreUpdate` 自动维护创建和更新时间。

### 5. 业务唯一性校验
Service 层校验用户名和邮箱的唯一性，避免重复注册。

## 常见问题

**Q: 首次运行很慢？**
A: 首次运行 Maven Wrapper 会下载 Maven 发行版和项目依赖，后续运行会快很多。

**Q: 如何切换到 MySQL？**
A: 修改 `application.yml` 中的 datasource 配置，并在 `pom.xml` 中添加 MySQL 驱动依赖即可。

**Q: 端口被占用怎么办？**
A: 修改 `application.yml` 中 `server.port` 为其他端口。

## License

MIT
