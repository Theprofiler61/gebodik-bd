#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

HOST="${HOST:-127.0.0.1}"
PORT="${PORT:-15432}"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<EOF
run-cli.sh — запуск CLI клиента

Переменные окружения:
  HOST         (default: 127.0.0.1)
  PORT         (default: 15432)

Примеры:
  ./run-cli.sh
  HOST=127.0.0.1 PORT=15432 ./run-cli.sh
EOF
  exit 0
fi

exec ./gradlew -q --console=plain runCli --args="--host ${HOST} --port ${PORT}"


