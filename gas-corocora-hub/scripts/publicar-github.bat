@echo off
rem Crea un repositorio en GitHub por cada componente y sube el codigo (rama main).
rem Uso:  scripts\publicar-github.bat usuario-u-organizacion [public^|private]
rem Con GitHub CLI (gh auth login) crea los repositorios; sin gh, creelos vacios en github.com y el script hace el push.
setlocal
set "OWNER=%~1"
set "VIS=%~2"
if "%VIS%"=="" set "VIS=public"
if "%OWNER%"=="" (
  echo Uso: scripts\publicar-github.bat usuario-u-organizacion [public^|private]
  exit /b 1
)
set "RAIZ=%~dp0..\.."
for /f "usebackq delims=" %%r in ("%~dp0..\repos.txt") do call :publicar %%r
echo Listo. Reemplace TU-USUARIO por %OWNER% en gas-corocora-hub\README.md y vuelva a subir el hub.
pause
exit /b 0

:publicar
echo ==== %1
pushd "%RAIZ%\%1"
if not exist .git git init -b main
git add -A
git diff --cached --quiet || git commit -m "Gas Corocora - 3.a entrega: %1"
where gh >nul 2>nul
if not errorlevel 1 (
  gh repo view "%OWNER%/%1" >nul 2>nul
  if errorlevel 1 (
    gh repo create "%OWNER%/%1" --%VIS% --source . --remote origin --push
    popd
    exit /b 0
  )
)
git remote get-url origin >nul 2>nul || git remote add origin "https://github.com/%OWNER%/%1.git"
git push -u origin main
popd
exit /b 0
