#!/usr/bin/env bash
# Comprueba que cada componente responda
ok=0
probar() { if curl -sf -o /dev/null "$2"; then echo "[OK]    $1  $2"; else echo "[FALLA] $1  $2"; ok=1; fi; }
probar "Motor - engine-rest     " http://127.0.0.1:8080/engine-rest/engine
probar "Motor - API de casos    " "http://127.0.0.1:8080/api/casos?limite=1"
probar "Recaudo                 " http://127.0.0.1:8083/api/salud
probar "Portal usuarios         " http://127.0.0.1:8081/ingresar
probar "Portal interno          " http://127.0.0.1:8082/ingresar
n=$(curl -sf "http://127.0.0.1:8080/engine-rest/external-task/count?active=true" 2>/dev/null || echo "?")
echo "Workers: revise su ventana/log (tareas externas activas sin atender: $n)"
exit $ok
