#!/usr/bin/env bash
# GAS COROCORA - Workers JavaScript (macOS / Linux)
cd "$(dirname "$0")" || exit 1
command -v node >/dev/null || { echo "[ERROR] Instale Node.js LTS"; exit 1; }
[ -d node_modules ] || npm install || exit 1
echo "Esperando al motor en http://127.0.0.1:8080/engine-rest ..."
until curl -sf http://127.0.0.1:8080/engine-rest/engine >/dev/null; do sleep 5; done
npm start
