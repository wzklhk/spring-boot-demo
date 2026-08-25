# ============================================================
# spring-boot-demo — 多阶段 Dockerfile（仅后端）
#   前端产物不打进 jar：cd frontend && npm run build 输出 frontend/dist，
#   由 docker-compose 挂载到容器 /app/frontend（Spring 通过
#   spring.web.resources.static-locations 的 file: 路径读取，见 application-prod.yml）
# ============================================================

# ---- Stage 1: 后端编译 ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# 关键: maven 镜像默认 MAVEN_CONFIG=/root/.m2，而 mvnw(3.2.0) 会把它拼进命令行
# 导致 "Unknown lifecycle phase /root/.m2"，必须清空
ENV MAVEN_CONFIG=""

COPY backend/pom.xml backend/pom.xml
COPY backend/.mvn backend/.mvn
COPY backend/mvnw backend/mvnw
COPY backend/src backend/src

RUN cd backend \
    && chmod +x mvnw \
    && ./mvnw -q -DskipTests package

# ---- Stage 2: 运行 ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/backend/target/spring-boot-demo-1.0.0.jar /app/app.jar

# 前端静态资源挂载点（compose 挂载 frontend/dist 到此，见 deploy/docker-compose.app.yml）
RUN mkdir -p /app/frontend

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
