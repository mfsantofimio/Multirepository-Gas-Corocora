#!/usr/bin/env bash
# GAS COROCORA - Motor Camunda 7 (Spring Boot) (macOS / Linux)
cd "$(dirname "$0")" || exit 1
command -v java >/dev/null || { echo "[ERROR] Instale JDK 17+"; exit 1; }
java -version 2>&1 | head -1
if (command -v lsof >/dev/null && lsof -i :8080 -sTCP:LISTEN >/dev/null); then
  echo "[ERROR] El puerto 8080 esta ocupado (otro Camunda u otra ventana de este proyecto). Cierrelo y reintente."; lsof -i :8080 -sTCP:LISTEN; exit 1
fi
command -v mvn >/dev/null || { echo "[ERROR] Instale Maven o ejecute la clase *Application desde el IDE"; exit 1; }
echo "Cuando vea 'Started Application' abra http://localhost:8080/camunda   (demo / demo)"
mvn spring-boot:run
