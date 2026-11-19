@echo off
title GAS COROCORA - Workers JavaScript (External Tasks)
cd /d "%~dp0"
echo ==========================================================
echo   GAS COROCORA - Workers JavaScript (External Tasks)
echo ==========================================================
where node >nul 2>nul
if errorlevel 1 goto sinNode
echo [OK] Node:
node -v
if not exist node_modules (
  echo Instalando dependencias, solo la primera vez...
  call npm.cmd install
  if errorlevel 1 goto sinNpm
)
echo.
echo Esperando a que el motor responda en http://127.0.0.1:8080/engine-rest ...
:esperar
powershell -NoProfile -Command "try { Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/engine-rest/engine -TimeoutSec 3 | Out-Null; exit 0 } catch { exit 1 }"
if errorlevel 1 (
  timeout /t 5 /nobreak >nul
  goto esperar
)
echo [OK] Motor arriba. Iniciando workers...
echo.
call npm.cmd start
pause
exit /b 0

:sinNode
echo [ERROR] No se encontro Node.js. Instale la version LTS desde https://nodejs.org
pause
exit /b 1

:sinNpm
echo [ERROR] Fallo npm install. Revise su conexion a internet.
pause
exit /b 1
