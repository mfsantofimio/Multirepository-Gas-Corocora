#!/usr/bin/env bash
# GAS COROCORA - Portal interno (funcionarios) (macOS / Linux)
cd "$(dirname "$0")" || exit 1
command -v java >/dev/null || { echo "[ERROR] Instale JDK 17+"; exit 1; }
java -version 2>&1 | head -1
if (command -v lsof >/dev/null && lsof -i :8082 -sTCP:LISTEN >/dev/null); then
  echo "[ERROR] El puerto 8082 esta ocupado (otra ventana del portal interno). Cierrelo y reintente."; lsof -i :8082 -sTCP:LISTEN; exit 1
fi
command -v mvn >/dev/null || { echo "[ERROR] Instale Maven o ejecute la clase *Application desde el IDE"; exit 1; }
echo "Cuando vea 'Started PortalInternoApplication' abra http://localhost:8082   (usuarios de rol, clave demo)"
mvn spring-boot:run
