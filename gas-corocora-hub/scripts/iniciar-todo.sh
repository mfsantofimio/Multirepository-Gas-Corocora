#!/usr/bin/env bash
# Arranca los 5 componentes en orden, en segundo plano, con logs en gas-corocora-hub/logs/.
# Detener: ./scripts/detener-todo.sh
set -e
HUB="$(cd "$(dirname "$0")/.." && pwd)"; RAIZ="$(cd "$HUB/.." && pwd)"; LOGS="$HUB/logs"; mkdir -p "$LOGS"
for r in gas-corocora-motor gas-corocora-recaudo gas-corocora-workers gas-corocora-portal-usuarios gas-corocora-portal-interno; do
  [ -d "$RAIZ/$r" ] || { echo "[ERROR] Falta $r junto al hub. Ejecute scripts/clonar.sh"; exit 1; }
done
lanzar() { echo "$2"; (cd "$RAIZ/$1" && nohup bash -c "$3" > "$LOGS/$1.log" 2>&1 & echo $! > "$LOGS/$1.pid"); }
esperar() { echo -n "    esperando $1 "; until curl -sf "$1" >/dev/null; do echo -n "."; sleep 5; done; echo " OK"; }
lanzar gas-corocora-recaudo "1/5 Recaudo (8083)" "npm start"
lanzar gas-corocora-motor "2/5 Motor (8080)" "mvn -q spring-boot:run"
esperar http://127.0.0.1:8080/engine-rest/engine
lanzar gas-corocora-workers "3/5 Workers" "[ -d node_modules ] || npm install; npm start"
lanzar gas-corocora-portal-usuarios "4/5 Portal usuarios (8081)" "mvn -q spring-boot:run"
lanzar gas-corocora-portal-interno "5/5 Portal interno (8082)" "mvn -q spring-boot:run"
esperar http://127.0.0.1:8081/ingresar
esperar http://127.0.0.1:8082/ingresar
"$HUB/scripts/verificar.sh"
