#!/usr/bin/env bash
# ============================================================
# spring-boot-demo 前端 — 编译脚本
#   安装依赖（如需要）+ 构建生产包
#   → backend/src/main/resources/static（由 Spring Boot 单 jar 托管）
#
# 用法:
#   ./build.sh         构建生产包
# ============================================================
set -euo pipefail

cd "$(dirname "$0")"
FRONTEND_DIR="$(pwd)"
BACKEND_STATIC="$FRONTEND_DIR/../backend/src/main/resources/static"

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
if [ ! -d node_modules ]; then
    info "安装前端依赖 (npm install)..."
    npm install
    ok "依赖安装完成"
else
    info "node_modules 已存在，跳过安装"
fi

# ---- 构建 ----
info "开始编译 (npm run build)..."
npm run build
ok "构建完成 → $BACKEND_STATIC/"

if [ ! -f "$BACKEND_STATIC/index.html" ]; then
    warn "未找到 $BACKEND_STATIC/index.html，请检查 vite 配置"
    exit 1
fi
ok "index.html 已就位，重新打包后端 jar 后即可生效"
