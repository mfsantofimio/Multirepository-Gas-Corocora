@echo off
rem Comprueba que cada componente responda
setlocal
call :probar "Motor - engine-rest" "http://127.0.0.1:8080/engine-rest/engine"
call :probar "Motor - API de casos" "http://127.0.0.1:8080/api/casos?limite=1"
call :probar "Recaudo" "http://127.0.0.1:8083/api/salud"
call :probar "Portal usuarios" "http://127.0.0.1:8081/ingresar"
call :probar "Portal interno" "http://127.0.0.1:8082/ingresar"
echo Workers: revise su ventana (deben decir "Workers de GAS COROCORA escuchando").
pause
exit /b 0

:probar
powershell -NoProfile -Command "try { Invoke-WebRequest -UseBasicParsing '%~2' -TimeoutSec 5 | Out-Null; exit 0 } catch { exit 1 }"
if errorlevel 1 (echo [FALLA] %~1  %~2) else (echo [OK]    %~1  %~2)
exit /b 0
