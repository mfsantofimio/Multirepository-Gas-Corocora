#!/usr/bin/env bash
# Detiene lo que inicio iniciar-todo.sh
LOGS="$(cd "$(dirname "$0")/.." && pwd)/logs"
for f in "$LOGS"/*.pid; do [ -f "$f" ] || continue; pid=$(cat "$f"); pkill -P "$pid" 2>/dev/null; kill "$pid" 2>/dev/null && echo "detenido $(basename "$f" .pid)"; rm -f "$f"; done
