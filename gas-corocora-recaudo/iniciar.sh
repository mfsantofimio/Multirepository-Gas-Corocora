#!/usr/bin/env bash
# GAS COROCORA - Servicio de Recaudo / Tesoreria (macOS / Linux)
cd "$(dirname "$0")" || exit 1
command -v node >/dev/null || { echo "[ERROR] Instale Node 22.13+"; exit 1; }
node -e "const [a,b]=process.versions.node.split('.').map(Number);process.exit(a>22||(a===22&&b>=13)?0:1)" \
  || { echo "[ERROR] Recaudo requiere Node 22.13+ (actual $(node -v))"; exit 1; }
echo "Consola de Recaudo: http://localhost:8083"
npm start
