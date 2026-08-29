#!/usr/bin/env bash
# ============================================================
# spring-boot-demo 前端 — 编译脚本
#   npm run build → vue/dist/（生产构建产物）
#   部署时由 docker-compose 挂载 dist/ → 容器 /app/frontend
#   （见 deploy/docker-compose.app.yaml），改前端只需重新构建，
#   无需重建后端镜像/重启容器
#
# 用法:
#   ./build.sh         构建生产包
# ============================================================
set -euo pipefail

cd "$(dirname "$0")"
VUE_DIR="$(pwd)"
DIST_DIR="$VUE_DIR/dist"

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
ok "构建完成 → $DIST_DIR/"

if [ ! -f "$DIST_DIR/index.html" ]; then
    warn "未找到 $DIST_DIR/index.html，请检查 vite 配置"
    exit 1
fi
ok "index.html 已就位；部署时 compose 会挂载 $DIST_DIR → 容器 /app/frontend"
