@echo off
rem Clona todos los repositorios de Gas Corocora como carpetas hermanas del hub.
rem Uso:  scripts\clonar.bat <usuario-u-organizacion-de-GitHub>
setlocal
set "OWNER=%~1"
if "%OWNER%"=="" set "OWNER=%GITHUB_OWNER%"
if "%OWNER%"=="" (
  echo Uso: scripts\clonar.bat usuario-u-organizacion-de-GitHub
  exit /b 1
)
where git >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Instale Git: https://git-scm.com
  exit /b 1
)
pushd "%~dp0..\.."
for /f "usebackq delims=" %%r in ("%~dp0..\repos.txt") do (
  if exist "%%r\.git" (
    echo [=] %%r ya existe: git pull
    git -C "%%r" pull --ff-only
  ) else (
    echo [+] Clonando %%r
    git clone "https://github.com/%OWNER%/%%r.git"
  )
)
echo Listo. Repositorios en: %CD%
popd
