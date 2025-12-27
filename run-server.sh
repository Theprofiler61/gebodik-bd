#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

PORT="${PORT:-15432}"
DATA_DIR="${DATA_DIR:-data}"
BUFFER_POOL="${BUFFER_POOL:-10}"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<EOF
run-server.sh — запуск DB сервера

Переменные окружения:
  PORT         (default: 15432)
  DATA_DIR      (default: data)
  BUFFER_POOL   (default: 10)

Пример:
  PORT=15432 DATA_DIR=data ./run-server.sh
EOF
  exit 0
fi

exec ./gradlew -q --console=plain run --args="--port ${PORT} --dataDir ${DATA_DIR} --bufferPool ${BUFFER_POOL}"


