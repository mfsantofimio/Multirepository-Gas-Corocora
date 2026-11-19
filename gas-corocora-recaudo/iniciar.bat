@echo off
title GAS COROCORA - Servicio de Recaudo / Tesoreria (puerto 8083)
cd /d "%~dp0"
echo ==========================================================
echo   GAS COROCORA - Servicio de Recaudo / Tesoreria
echo ==========================================================
where node >nul 2>nul
if errorlevel 1 goto sinNode
node -e "const [a,b]=process.versions.node.split('.').map(Number);process.exit(a>22||(a===22&&b>=13)?0:1)"
if errorlevel 1 goto nodeViejo
echo [OK] Node:
node -v
netstat -ano | findstr /r /c:":8083 .*LISTENING" >nul
if not errorlevel 1 goto puertoOcupado
echo No necesita npm install: usa solo modulos nativos de Node (node:sqlite, node:http).
echo Consola de Recaudo: http://localhost:8083
echo.
call npm.cmd start
pause
exit /b 0

:sinNode
echo [ERROR] No se encontro Node.js. Instale Node 22.13 o superior (https://nodejs.org)
pause
exit /b 1

:nodeViejo
echo [ERROR] Recaudo usa node:sqlite y requiere Node 22.13 o superior. Version actual:
node -v
pause
exit /b 1

:puertoOcupado
echo [ERROR] El puerto 8083 ya esta ocupado (otra ventana de Recaudo).
netstat -ano | findstr /r /c:":8083 .*LISTENING"
pause
exit /b 1
