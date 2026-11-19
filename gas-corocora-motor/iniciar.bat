@echo off
setlocal
title GAS COROCORA - Motor Camunda 7 (Spring Boot)
cd /d "%~dp0"
echo ==========================================================
echo   GAS COROCORA - Motor Camunda 7 (Spring Boot)
echo ==========================================================
echo.

rem ---------------------------------------------------------- 1. Java 17+
set "JDK_DIR="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JDK_DIR=%JAVA_HOME%"
if not defined JDK_DIR for /f "delims=" %%d in ('dir /b /ad /o-n "%USERPROFILE%\.jdks" 2^>nul') do (
  if not defined JDK_DIR if exist "%USERPROFILE%\.jdks\%%d\bin\java.exe" set "JDK_DIR=%USERPROFILE%\.jdks\%%d"
)
if defined JDK_DIR (
  set "JAVA_HOME=%JDK_DIR%"
  set "PATH=%JDK_DIR%\bin;%PATH%"
)
where java >nul 2>nul
if errorlevel 1 goto sinJava
java -version 2>&1 | findstr /r /c:"version .1\.[0-9]" /c:"version .9[^0-9]" /c:"version .1[0-6]\." >nul
if not errorlevel 1 goto javaVieja
echo [OK] Java:
java -version 2>&1 | findstr /i "version"
if defined JAVA_HOME echo      JAVA_HOME=%JAVA_HOME%

rem ---------------------------------------------------------- 2. Puerto 8080
netstat -ano | findstr /r /c:":8080 .*LISTENING" >nul
if not errorlevel 1 goto puertoOcupado
echo [OK] Puerto 8080 libre

rem ---------------------------------------------------------- 3. Maven
set "MVN_CMD="
for /f "delims=" %%m in ('where mvn.cmd 2^>nul') do if not defined MVN_CMD set "MVN_CMD=%%m"
if not defined MVN_CMD for /d %%i in ("%ProgramFiles%\JetBrains\IntelliJ IDEA*" "%LOCALAPPDATA%\Programs\IntelliJ IDEA*") do (
  if not defined MVN_CMD if exist "%%~i\plugins\maven\lib\maven3\bin\mvn.cmd" set "MVN_CMD=%%~i\plugins\maven\lib\maven3\bin\mvn.cmd"
)
set "MVN_LOCAL=%USERPROFILE%\.m2\gascorocora-maven\apache-maven-3.9.9"
if not defined MVN_CMD if exist "%MVN_LOCAL%\bin\mvn.cmd" set "MVN_CMD=%MVN_LOCAL%\bin\mvn.cmd"
if not defined MVN_CMD call :descargarMaven
if not defined MVN_CMD goto sinMaven
echo [OK] Maven: %MVN_CMD%
echo.
echo La primera vez descarga dependencias (puede tardar varios minutos).
echo Cuando vea "Started Application" abra:  http://localhost:8080/camunda   (demo / demo)
echo Luego inicie Recaudo, los workers y los portales (ver gas-corocora-hub).
echo.
call "%MVN_CMD%" spring-boot:run
echo.
echo El servidor se detuvo. Si hubo un error, revise las lineas de arriba.
pause
exit /b 0

:descargarMaven
echo [..] No se encontro Maven. Descargando Apache Maven 3.9.9 (una sola vez)...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $d=Join-Path $env:USERPROFILE '.m2\gascorocora-maven'; New-Item -ItemType Directory -Force $d | Out-Null; $z=Join-Path $d 'maven.zip'; [Net.ServicePointManager]::SecurityProtocol='Tls12'; Invoke-WebRequest -UseBasicParsing 'https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip' -OutFile $z; Expand-Archive -Force $z $d; Remove-Item $z"
if exist "%MVN_LOCAL%\bin\mvn.cmd" set "MVN_CMD=%MVN_LOCAL%\bin\mvn.cmd"
exit /b 0

:sinJava
echo [ERROR] No se encontro Java 17 o superior. Instale un JDK (https://adoptium.net)
pause
exit /b 1

:javaVieja
echo [ERROR] Su Java es anterior a la 17:
java -version
echo         Instale JDK 17+ o configure JAVA_HOME hacia uno.
pause
exit /b 1

:puertoOcupado
echo [ERROR] El puerto 8080 ya esta ocupado (otro Camunda u otra ventana de este proyecto).
echo         Cierrelo y vuelva a ejecutar este archivo. Proceso que usa el puerto:
netstat -ano | findstr /r /c:":8080 .*LISTENING"
pause
exit /b 1

:sinMaven
echo [ERROR] No se pudo obtener Maven. Abra la carpeta en IntelliJ y ejecute
echo         src\main\java\com\gascorocora\Application.java con el boton Run.
pause
exit /b 1
