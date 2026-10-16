@echo off
setlocal EnableExtensions

title Emall Platform Launcher

set "ROOT_DIR=%~dp0"
set "BACKEND_DIR=%ROOT_DIR%mall-backend"
set "CLIENT_DIR=%ROOT_DIR%emall-client"
set "ADMIN_DIR=%ROOT_DIR%emall-admin"
set "ENV_FILE=%BACKEND_DIR%\.env"

echo.
echo ========================================
echo       Emall Platform - Start All
echo ========================================
echo.

call :require_command docker "Docker is not installed or is not on PATH."
if errorlevel 1 goto :failed

docker compose version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker Compose v2 is required.
    goto :failed
)

call :require_command node "Node.js is not installed or is not on PATH."
if errorlevel 1 goto :failed
call :require_command npm "npm is not installed or is not on PATH."
if errorlevel 1 goto :failed

call :ensure_docker
if errorlevel 1 goto :failed

call :prepare_env
if errorlevel 1 goto :failed

call :install_dependencies "%CLIENT_DIR%" "client"
if errorlevel 1 goto :failed
call :install_dependencies "%ADMIN_DIR%" "admin"
if errorlevel 1 goto :failed

echo [1/4] Starting MySQL, Redis, and the Spring Boot backend...
pushd "%BACKEND_DIR%"
docker compose up -d --build
set "COMPOSE_EXIT=%ERRORLEVEL%"
popd
if not "%COMPOSE_EXIT%"=="0" (
    echo [ERROR] Docker services failed to start.
    goto :failed
)

call :port_open 5173
if errorlevel 1 (
    echo [2/4] Starting the client at http://localhost:5173 ...
    start "Emall Client - 5173" /D "%CLIENT_DIR%" cmd /k "npm run dev -- --host 127.0.0.1 --strictPort"
) else (
    echo [2/4] Port 5173 is already running; keeping the current client process.
)

call :port_open 5174
if errorlevel 1 (
    echo [3/4] Starting the admin site at http://localhost:5174 ...
    start "Emall Admin - 5174" /D "%ADMIN_DIR%" cmd /k "npm run dev -- --host 127.0.0.1 --strictPort"
) else (
    echo [3/4] Port 5174 is already running; keeping the current admin process.
)

echo [4/4] Waiting for all services to become available...
call :wait_for_port 8080 180 "backend"
if errorlevel 1 goto :services_not_ready
call :wait_for_port 5173 60 "client"
if errorlevel 1 goto :services_not_ready
call :wait_for_port 5174 60 "admin"
if errorlevel 1 goto :services_not_ready

echo.
echo All services are ready:
echo   Client : http://localhost:5173
echo   Admin  : http://localhost:5174
echo   API    : http://localhost:8080
echo.
echo Opening the client and admin sites in your default browser...
start "" "http://localhost:5173"
start "" "http://localhost:5174"
exit /b 0

:require_command
where %~1 >nul 2>&1
if errorlevel 1 (
    echo [ERROR] %~2
    exit /b 1
)
exit /b 0

:ensure_docker
docker info >nul 2>&1
if not errorlevel 1 exit /b 0

set "DOCKER_DESKTOP=%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
if not exist "%DOCKER_DESKTOP%" (
    echo [ERROR] Docker Engine is not running. Start Docker Desktop and try again.
    exit /b 1
)

echo Docker Engine is not running. Starting Docker Desktop...
start "Docker Desktop" "%DOCKER_DESKTOP%"
for /L %%I in (1,1,60) do (
    docker info >nul 2>&1
    if not errorlevel 1 exit /b 0
    timeout /t 2 /nobreak >nul
)

echo [ERROR] Docker Engine did not become ready within 120 seconds.
exit /b 1

:prepare_env
if not exist "%ENV_FILE%" (
    if not exist "%BACKEND_DIR%\.env.example" (
        echo [ERROR] Missing mall-backend\.env.example.
        exit /b 1
    )
    copy /Y "%BACKEND_DIR%\.env.example" "%ENV_FILE%" >nul
    echo Created mall-backend\.env from .env.example.
)

for /F %%I in ('powershell -NoProfile -Command "[guid]::NewGuid().ToString('N')"') do set "GENERATED_DB_PASSWORD=%%I"
for /F %%I in ('powershell -NoProfile -Command "([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))"') do set "GENERATED_JWT_SECRET=%%I"
set "EMALL_ENV_FILE=%ENV_FILE%"

powershell -NoProfile -ExecutionPolicy Bypass -Command "$p=$env:EMALL_ENV_FILE; $c=[IO.File]::ReadAllText($p); $nl=[Environment]::NewLine; if($c -match '(?m)^DB_PASSWORD=\s*$'){$c=[regex]::Replace($c,'(?m)^DB_PASSWORD=\s*$','DB_PASSWORD='+$env:GENERATED_DB_PASSWORD)}elseif($c -notmatch '(?m)^DB_PASSWORD='){$c+=$nl+'DB_PASSWORD='+$env:GENERATED_DB_PASSWORD+$nl}; if($c -match '(?m)^JWT_SECRET=\s*$'){$c=[regex]::Replace($c,'(?m)^JWT_SECRET=\s*$','JWT_SECRET='+$env:GENERATED_JWT_SECRET)}elseif($c -notmatch '(?m)^JWT_SECRET='){$c+=$nl+'JWT_SECRET='+$env:GENERATED_JWT_SECRET+$nl}; [IO.File]::WriteAllText($p,$c,(New-Object Text.UTF8Encoding($false)))"
if errorlevel 1 (
    echo [ERROR] Could not prepare mall-backend\.env.
    exit /b 1
)
exit /b 0

:install_dependencies
if exist "%~1\node_modules" (
    echo Dependencies for %~2 are already installed.
    exit /b 0
)

echo Installing %~2 dependencies with npm ci...
pushd "%~1"
call npm ci
set "NPM_EXIT=%ERRORLEVEL%"
popd
if not "%NPM_EXIT%"=="0" (
    echo [ERROR] Failed to install %~2 dependencies.
    exit /b 1
)
exit /b 0

:port_open
powershell -NoProfile -Command "$c=New-Object Net.Sockets.TcpClient; try{$t=$c.ConnectAsync('127.0.0.1',[int]%~1); if(-not $t.Wait(500)){exit 1}; exit 0}catch{exit 1}finally{$c.Dispose()}" >nul 2>&1
exit /b %ERRORLEVEL%

:wait_for_port
for /L %%I in (1,1,%~2) do (
    call :port_open %~1
    if not errorlevel 1 (
        echo %~3 is ready on port %~1.
        exit /b 0
    )
    timeout /t 1 /nobreak >nul
)
echo [ERROR] Timed out waiting for %~3 on port %~1.
exit /b 1

:services_not_ready
echo.
echo One or more services did not become ready.
echo Run the following command to inspect backend logs:
echo   cd /d "%BACKEND_DIR%" ^&^& docker compose logs backend
goto :failed

:failed
echo.
echo Startup stopped. Review the error above, then run this file again.
pause
exit /b 1
