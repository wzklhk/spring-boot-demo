# ============================================================
# spring-boot-demo — 多阶段 Dockerfile（单 JAR，自包含前端）
#   前端由 frontend-maven-plugin 在 `mvn package` 阶段自动构建
#   （npm ci + vite build），产物打进 jar（classpath:/static/），
#   最终 JAR 内已含全部前端资源，无需挂载外部 vue/dist。
# ============================================================

# ---- Stage 1: 后端编译（含前端构建，产物一并打入 jar） ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# 关键: maven 镜像默认 MAVEN_CONFIG=/root/.m2，而 mvnw(3.2.0) 会把它拼进命令行
# 导致 "Unknown lifecycle phase /root/.m2"，必须清空
ENV MAVEN_CONFIG=""

COPY spring-boot/pom.xml spring-boot/pom.xml
COPY spring-boot/.mvn spring-boot/.mvn
COPY spring-boot/mvnw spring-boot/mvnw
COPY spring-boot/src spring-boot/src
# 前端源码（frontend-maven-plugin 在 mvn package 阶段执行 npm ci + vite build）
COPY vue vue

# sed 防御：Windows 上 autocrlf 可能把 mvnw 检出成 CRLF，shebang 变成 "#!/bin/sh\r"
# 导致 "/bin/sh: ./mvnw: not found"；这里先剥掉 \r 再执行（.gitattributes 已强制 LF，此为双保险）
RUN cd spring-boot \
    && sed -i 's/\r$//' mvnw \
    && chmod +x mvnw \
    && ./mvnw -q -DskipTests package

# ---- Stage 2: 运行 ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/spring-boot/target/spring-boot-demo-1.0.0.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
