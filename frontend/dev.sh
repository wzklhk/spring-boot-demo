#!/usr/bin/env bash
# ============================================================
# spring-boot-demo 前端 — 开发服务器启动脚本
#   Vite dev server (port 5173)，/api 代理转发到后端 8080
#
# 用法:
#   ./dev.sh          启动前端开发服务器
#   (需先启动后端: cd ../backend && ./start.sh)
# ============================================================
set -euo pipefail

cd "$(dirname "$0")"

# ---- 颜色 ----
RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

info() { echo -e "${CYAN}[INFO]${NC}  $1"; }
ok()   { echo -e "${GREEN}[OK]${NC}    $1"; }
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
fi

# ---- 启动 ----
info "启动前端开发服务器 → http://localhost:5173"
info "(/api 与 /h2-console 将代理到 http://localhost:8080)"
exec npm run dev
