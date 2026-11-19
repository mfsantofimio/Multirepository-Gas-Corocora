#!/usr/bin/env bash
# Clona todos los repositorios de Gas Corocora como carpetas hermanas del hub.
# Uso:  ./scripts/clonar.sh <usuario-u-organizacion-de-GitHub>
set -e
OWNER="${1:-${GITHUB_OWNER:-}}"
[ -n "$OWNER" ] || { echo "Uso: $0 <usuario-u-organizacion-de-GitHub>"; exit 1; }
DESTINO="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$DESTINO"
while read -r repo; do
  [ -z "$repo" ] && continue
  if [ -d "$repo/.git" ]; then echo "[=] $repo ya existe: git pull"; git -C "$repo" pull --ff-only || true
  else echo "[+] Clonando $repo"; git clone "https://github.com/$OWNER/$repo.git"; fi
done < "$(dirname "$0")/../repos.txt"
echo "Listo. Repositorios en: $DESTINO"
