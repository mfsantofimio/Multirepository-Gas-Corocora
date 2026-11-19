#!/usr/bin/env bash
# Crea un repositorio en GitHub por cada componente y sube el codigo (rama main).
# Uso:  ./scripts/publicar-github.sh <usuario-u-organizacion> [public|private]
# Con GitHub CLI (gh auth login) crea los repositorios; sin gh, creelos vacios en github.com y el script hace el push.
set -e
OWNER="${1:?Uso: $0 <usuario-u-organizacion> [public|private]}"; VIS="${2:-public}"
RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
while read -r repo; do
  [ -z "$repo" ] && continue
  cd "$RAIZ/$repo"
  [ -d .git ] || git init -b main
  git add -A
  git diff --cached --quiet || git commit -m "Gas Corocora - 3.a entrega: $repo"
  if command -v gh >/dev/null && ! gh repo view "$OWNER/$repo" >/dev/null 2>&1; then
    gh repo create "$OWNER/$repo" --"$VIS" --source . --remote origin --push
  else
    git remote get-url origin >/dev/null 2>&1 || git remote add origin "https://github.com/$OWNER/$repo.git"
    git push -u origin main
  fi
done < "$RAIZ/gas-corocora-hub/repos.txt"
echo "Listo. Reemplace TU-USUARIO por $OWNER en gas-corocora-hub/README.md (seccion Repositorios) y vuelva a subir el hub."
