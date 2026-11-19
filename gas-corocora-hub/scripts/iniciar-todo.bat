@echo off
rem Arranca los 5 componentes en el orden correcto, cada uno en su propia ventana.
rem Requiere que los repositorios esten como carpetas hermanas del hub (scripts\clonar.bat).
setlocal
title GAS COROCORA - Arranque general
set "RAIZ=%~dp0..\.."
for %%r in (gas-corocora-motor gas-corocora-recaudo gas-corocora-workers gas-corocora-portal-usuarios gas-corocora-portal-interno) do (
  if not exist "%RAIZ%\%%r" (
    echo [ERROR] Falta la carpeta %%r junto al hub. Ejecute scripts\clonar.bat primero.
    pause
    exit /b 1
  )
)
echo 1/5 Servicio de Recaudo (8083)
start "GC Recaudo 8083" cmd /c ""%RAIZ%\gas-corocora-recaudo\iniciar.bat""
echo 2/5 Motor Camunda (8080) - la primera vez Maven descarga dependencias
start "GC Motor 8080" cmd /c ""%RAIZ%\gas-corocora-motor\iniciar.bat""
echo     Esperando a que el motor responda...
:esperarMotor
powershell -NoProfile -Command "try { Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/engine-rest/engine -TimeoutSec 3 | Out-Null; exit 0 } catch { exit 1 }"
if errorlevel 1 (
  timeout /t 5 /nobreak >nul
  goto esperarMotor
)
echo [OK] Motor arriba
echo 3/5 Workers JavaScript
start "GC Workers" cmd /c ""%RAIZ%\gas-corocora-workers\iniciar.bat""
echo 4/5 Portal de usuarios (8081)
start "GC Portal usuarios 8081" cmd /c ""%RAIZ%\gas-corocora-portal-usuarios\iniciar.bat""
echo 5/5 Portal interno (8082)
start "GC Portal interno 8082" cmd /c ""%RAIZ%\gas-corocora-portal-interno\iniciar.bat""
echo.
echo Cuando los portales digan "Started ..." ejecute scripts\verificar.bat
echo   Cockpit          http://localhost:8080/camunda   (demo / demo)
echo   Portal usuarios  http://localhost:8081           (1122334455 / demo123)
echo   Portal interno   http://localhost:8082           (lector, facturador, analista... / demo)
echo   Recaudo          http://localhost:8083
echo Para detener: cierre cada ventana.
pause
