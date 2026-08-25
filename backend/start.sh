#!/usr/bin/env bash
# ============================================================
# spring-boot-demo 后端 — 编译 & 启动脚本 (Spring Boot)
#   JDK21 + Maven Wrapper 编译，然后 java -jar 启动
#
# 用法:
#   ./start.sh         编译 + 启动（默认）
#   ./start.sh build   仅编译（./mvnw -DskipTests package）
#   ./start.sh run     仅启动（跳过编译，需已有 jar）
# ============================================================
set -euo pipefail

cd "$(dirname "$0")"
BACKEND_DIR="$(pwd)"

# ---- 本机已知可用 JDK：SB 3.5.x + Java 21 ----
JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
if [ ! -d "$JAVA_HOME" ]; then
    JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"   # 找不到则仍用默认值，下面统一报错
fi

# ---- 颜色 ----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info() { echo -e "${CYAN}[INFO]${NC}  $1"; }
ok()   { echo -e "${GREEN}[OK]${NC}    $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $1"; }
err()  { echo -e "${RED}[ERR]${NC}   $1"; }

# ---- JDK 检查 ----
if [ ! -x "$JAVA_HOME/bin/java" ]; then
    err "未找到 JDK 21: $JAVA_HOME"
    err "请先安装: sudo apt-get install -y openjdk-21-jdk-headless"
    exit 1
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
ok "java=$(java -version 2>&1 | head -1)"

# ---- 找到最新构建产物 ----
latest_jar() {
    ls -t target/*.jar 2>/dev/null | grep -v sources | head -1 || true
}

# ---- 编译 ----
build() {
    info "编译中 (./mvnw -DskipTests package，首次运行较慢)..."
    ./mvnw -DskipTests package
    if [ -z "$(latest_jar)" ]; then
        err "编译失败：target/ 下未生成 jar"
        exit 1
    fi
    ok "编译完成 → $(latest_jar)"
}

# ---- 启动 ----
run() {
    local jar
    jar="$(latest_jar)"
    if [ -z "$jar" ]; then
        err "target/ 下没有 jar，请先执行 ./start.sh build（或本机编译）"
        exit 1
    fi
    info "启动 Spring Boot (${jar##*/})..."
    info "页面:    http://localhost:8080"
    info "API:     http://localhost:8080/api/users"
    info "H2 控制台: http://localhost:8080/h2-console  (jdbc:h2:mem:demodb, sa/空密码)"
    exec java -jar "$jar"
}

# ---- 主流程 ----
case "${1:-}" in
    build) build ;;
    run)   run ;;
    help|--help|-h)
        sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'
        ;;
    *)     build && run ;;
esac
