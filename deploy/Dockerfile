# ============================================================
# spring-boot-demo — 多阶段 Dockerfile（仅后端）
#   前端产物不打进 jar：cd vue && npm run build 输出 vue/dist，
#   由 docker-compose 挂载到容器 /app/frontend（Spring 通过
#   spring.web.resources.static-locations 的 file: 路径读取，见 application-prod.yml）
# ============================================================

# ---- Stage 1: 后端编译 ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# 关键: maven 镜像默认 MAVEN_CONFIG=/root/.m2，而 mvnw(3.2.0) 会把它拼进命令行
# 导致 "Unknown lifecycle phase /root/.m2"，必须清空
ENV MAVEN_CONFIG=""

COPY spring-boot/pom.xml spring-boot/pom.xml
COPY spring-boot/.mvn spring-boot/.mvn
COPY spring-boot/mvnw spring-boot/mvnw
COPY spring-boot/src spring-boot/src

RUN cd spring-boot \
    && chmod +x mvnw \
    && ./mvnw -q -DskipTests package

# ---- Stage 2: 运行 ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/spring-boot/target/spring-boot-demo-1.0.0.jar /app/app.jar

# 前端静态资源挂载点（compose 挂载 vue/dist 到此，见 deploy/docker-compose.app.yml）
RUN mkdir -p /app/frontend

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
