#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEB_DIR="${ROOT_DIR}/ai-mcp-knowledge-web"

if [[ ! -d "${WEB_DIR}/node_modules" ]]; then
  echo "==> 未检测到前端依赖，开始安装"
  pushd "${WEB_DIR}" >/dev/null
  npm install
  popd >/dev/null
fi

echo "==> 启动前端 (Vite dev server)"
pushd "${WEB_DIR}" >/dev/null
npm run dev
popd >/dev/null
