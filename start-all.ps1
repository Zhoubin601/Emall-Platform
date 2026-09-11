# start-all.ps1 - 一键启动所有服务 (MySQL, Redis, Backend, Client, Admin)
$ErrorActionPreference = "Continue"

$toAdd = @(
    'C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin',
    'C:\tools\apache-maven-3.9.9\bin',
    'C:\Program Files\MySQL\MySQL Server 8.4\bin',
    'C:\Users\Zhou_bibi\AppData\Local\Microsoft\WinGet\Packages\taizod1024.redis-windows-fork_Microsoft.Winget.Source_8wekyb3d8bbwe\Redis-8.10.1-Windows-x64-msys2',
    'C:\Users\Zhou_bibi\AppData\Local\Microsoft\WinGet\Packages\Docker.DockerCLI_Microsoft.Winget.Source_8wekyb3d8bbwe\docker'
)
foreach ($p in $toAdd) {
    if ($env:Path -notlike "*$p*") {
        $env:Path = "$env:Path;$p"
    }
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  正在启动 Emall 全栈电商平台所有服务... " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. 启动 MySQL
Write-Host "[1/5] 启动 MySQL (Port 3306)..." -ForegroundColor Yellow
Start-Process -FilePath "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" -ArgumentList '--datadir="C:\tools\mysql-data" --port=3306 --console' -WindowStyle Hidden

# 2. 启动 Redis
Write-Host "[2/5] 启动 Redis (Port 6379)..." -ForegroundColor Yellow
Start-Process -FilePath "C:\Users\Zhou_bibi\AppData\Local\Microsoft\WinGet\Packages\taizod1024.redis-windows-fork_Microsoft.Winget.Source_8wekyb3d8bbwe\Redis-8.10.1-Windows-x64-msys2\redis-server.exe" -ArgumentList '--port 6379' -WindowStyle Hidden

# 3. 启动 Java Spring Boot 后端
Write-Host "[3/5] 启动 Java 后端 (Port 8080)..." -ForegroundColor Yellow
$env:JWT_SECRET = 'supersecretkeyformallbackendproject2026emallplatform'
$env:DB_URL = 'jdbc:mysql://localhost:3306/emall_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = ''
$env:REDIS_HOST = 'localhost'
$env:REDIS_PORT = '6379'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:5173,http://localhost:5174'
$env:UPLOAD_STORAGE_DIR = "$PSScriptRoot\mall-backend\uploads"
$env:MAIL_HOST = 'smtp.qq.com'
$env:MAIL_PORT = '587'
$env:MAIL_USERNAME = '3185130953@qq.com'
$env:MAIL_PASSWORD = 'eldbutadkmjydcgg'
Start-Process -FilePath "java" -ArgumentList "-jar target\mall-backend-1.0-SNAPSHOT.jar" -WorkingDirectory "$PSScriptRoot\mall-backend" -WindowStyle Hidden

# 4. 启动前台商城 (emall-client)
Write-Host "[4/5] 启动前台商城 (Port 5173)..." -ForegroundColor Yellow
Start-Process -FilePath "cmd.exe" -ArgumentList "/c npm run dev" -WorkingDirectory "$PSScriptRoot\emall-client" -WindowStyle Hidden

# 5. 启动后台管理 (emall-admin)
Write-Host "[5/5] 启动后台管理系统 (Port 5174)..." -ForegroundColor Yellow
Start-Process -FilePath "cmd.exe" -ArgumentList "/c npm run dev" -WorkingDirectory "$PSScriptRoot\emall-admin" -WindowStyle Hidden

Start-Sleep -Seconds 3
Write-Host "=========================================" -ForegroundColor Green
Write-Host "  全部服务已成功启动！" -ForegroundColor Green
Write-Host "  - 前台商城:  http://localhost:5173" -ForegroundColor Green
Write-Host "  - 管理后台:  http://localhost:5174" -ForegroundColor Green
Write-Host "  - 后端接口:  http://localhost:8080" -ForegroundColor Green
Write-Host "  - MySQL:     localhost:3306 (库: emall_db, 用户: root, 密码: 无)" -ForegroundColor Green
Write-Host "  - Redis:     localhost:6379" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
