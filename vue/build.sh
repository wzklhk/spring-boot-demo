#!/usr/bin/env bash
# ============================================================
# spring-boot-demo 前端 — 手动编译脚本（可选）
#   通常无需手动执行：Maven 构建（frontend-maven-plugin）会自动完成
#   npm ci + vite build（见 spring-boot/pom.xml），产物随后端一并打进 jar
#   （classpath:/static/），JAR 自包含，不再依赖外部 vue/dist 挂载。
#   仅当你只想单独构建/检查前端产物时才用本脚本。
#
# 用法:
#   ./build.sh         构建生产包
# ============================================================
set -euo pipefail

cd "$(dirname "$0")"
OUT_DIR="$PWD/../spring-boot/src/main/resources/static"

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

# ---- 依赖检查 ----
if ! command -v node &>/dev/null || ! command -v npm &>/dev/null; then
    err "Node.js / npm 未安装"
    exit 1
fi
ok "node=$(node -v), npm=$(npm -v)"

# ---- 安装依赖 ----
info "安装前端依赖 (npm ci)..."
npm ci
ok "依赖安装完成"

# ---- 构建 ----
info "开始编译 (npm run build)..."
npm run build
ok "构建完成 → $OUT_DIR/"

if [ ! -f "$OUT_DIR/index.html" ]; then
    warn "未找到 $OUT_DIR/index.html，请检查 vite 配置"
    exit 1
fi
ok "index.html 已就位；产物将随后端打进 jar（classpath:/static/）"
