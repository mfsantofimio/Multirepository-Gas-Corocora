#!/usr/bin/env bash
# GAS COROCORA - Portal de usuarios externos (macOS / Linux)
cd "$(dirname "$0")" || exit 1
command -v java >/dev/null || { echo "[ERROR] Instale JDK 17+"; exit 1; }
java -version 2>&1 | head -1
if (command -v lsof >/dev/null && lsof -i :8081 -sTCP:LISTEN >/dev/null); then
  echo "[ERROR] El puerto 8081 esta ocupado (otra ventana del portal de usuarios). Cierrelo y reintente."; lsof -i :8081 -sTCP:LISTEN; exit 1
fi
command -v mvn >/dev/null || { echo "[ERROR] Instale Maven o ejecute la clase *Application desde el IDE"; exit 1; }
echo "Cuando vea 'Started PortalUsuariosApplication' abra http://localhost:8081   (documento 1122334455 / demo123)"
mvn spring-boot:run
