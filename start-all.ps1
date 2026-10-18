# start-all.ps1 - 一键启动所有服务 (MySQL, Redis, Backend, Client, Admin)
$ErrorActionPreference = "Continue"

$candidateDirs = @(
    'C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin',
    'C:\tools\apache-maven-3.9.9\bin',
    'C:\Program Files\MySQL\MySQL Server 8.4\bin',
    'C:\Program Files\MySQL\MySQL Server 8.0\bin'
)
if ($env:LOCALAPPDATA) {
    $wingetPkgs = Join-Path $env:LOCALAPPDATA 'Microsoft\WinGet\Packages'
    if (Test-Path $wingetPkgs) {
        Get-ChildItem -Path $wingetPkgs -Directory -ErrorAction SilentlyContinue | ForEach-Object {
            $candidateDirs += $_.FullName
            Get-ChildItem -Path $_.FullName -Directory -ErrorAction SilentlyContinue | ForEach-Object {
                $candidateDirs += $_.FullName
            }
        }
    }
}
foreach ($p in $candidateDirs) {
    if ((Test-Path $p) -and ($env:Path -notlike "*$p*")) {
        $env:Path = "$env:Path;$p"
    }
}

# 加载 mall-backend/.env 环境变量（如果存在）
$envFile = Join-Path $PSScriptRoot "mall-backend\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
            $parts = $line.Split('=', 2)
            $key = $parts[0].Trim()
            $val = $parts[1].Trim()
            if ($val) {
                [System.Environment]::SetEnvironmentVariable($key, $val, "Process")
            }
        }
    }
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  正在启动 Emall 全栈电商平台所有服务... " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. 启动 MySQL
Write-Host "[1/5] 检查并启动 MySQL (Port 3306)..." -ForegroundColor Yellow
$mysqldCmd = Get-Command mysqld.exe -ErrorAction SilentlyContinue
if ($mysqldCmd) {
    $mysqlArgs = if (Test-Path 'C:\tools\mysql-data') { '--datadir="C:\tools\mysql-data" --port=3306 --console' } else { '--port=3306 --console' }
    Start-Process -FilePath $mysqldCmd.Source -ArgumentList $mysqlArgs -WindowStyle Hidden
} else {
    Write-Host "  -> 未检测到本机 mysqld.exe，若已通过系统服务或 Docker 启动可忽略。" -ForegroundColor DarkGray
}

# 2. 启动 Redis
Write-Host "[2/5] 检查并启动 Redis (Port 6379)..." -ForegroundColor Yellow
$redisCmd = Get-Command redis-server.exe -ErrorAction SilentlyContinue
if ($redisCmd) {
    Start-Process -FilePath $redisCmd.Source -ArgumentList '--port 6379' -WindowStyle Hidden
} elseif (Test-Path (Join-Path $PSScriptRoot "redis-mock.js")) {
    Write-Host "  -> 未检测到 redis-server.exe，自动启用内置 redis-mock.js (Port 6379)..." -ForegroundColor DarkCyan
    Start-Process -FilePath "node" -ArgumentList "redis-mock.js" -WorkingDirectory $PSScriptRoot -WindowStyle Hidden
}

# 3. 启动后端服务 (优先 Spring Boot JAR，若未构建则回退至全功能 mock-server.js)
Write-Host "[3/5] 启动后端服务 (Port 8080)..." -ForegroundColor Yellow
if (-not $env:JWT_SECRET) { $env:JWT_SECRET = 'supersecretkeyformallbackendproject2026emallplatform' }
if (-not $env:DB_URL) { $env:DB_URL = 'jdbc:mysql://localhost:3306/emall_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai' }
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = 'root' }
if (-not $env:REDIS_HOST) { $env:REDIS_HOST = 'localhost' }
if (-not $env:REDIS_PORT) { $env:REDIS_PORT = '6379' }
if (-not $env:CORS_ALLOWED_ORIGINS) { $env:CORS_ALLOWED_ORIGINS = 'http://localhost:5173,http://localhost:5174' }
$env:UPLOAD_STORAGE_DIR = "$PSScriptRoot\mall-backend\uploads"
if (-not $env:MAIL_HOST) { $env:MAIL_HOST = 'smtp.qq.com' }
if (-not $env:MAIL_PORT) { $env:MAIL_PORT = '587' }
if (-not $env:MAIL_USERNAME) { $env:MAIL_USERNAME = '3185130953@qq.com' }
if (-not $env:MAIL_PASSWORD) { $env:MAIL_PASSWORD = 'your_smtp_auth_code_here' }

$jarPath = Join-Path $PSScriptRoot "mall-backend\target\mall-backend-1.0-SNAPSHOT.jar"
$javaCmd = Get-Command java.exe -ErrorAction SilentlyContinue
if ($javaCmd -and (Test-Path $jarPath)) {
    Start-Process -FilePath $javaCmd.Source -ArgumentList "-jar target\mall-backend-1.0-SNAPSHOT.jar" -WorkingDirectory "$PSScriptRoot\mall-backend" -WindowStyle Hidden
} else {
    Write-Host "  -> 未检测到已编译的后端 JAR 或 Java 环境，自动启动零依赖 mock-server.js (Port 8080)..." -ForegroundColor DarkCyan
    Start-Process -FilePath "node" -ArgumentList "mock-server.js" -WorkingDirectory $PSScriptRoot -WindowStyle Hidden
}

# 4. 启动前台商城 (emall-client)
Write-Host "[4/5] 启动前台商城 (Port 5173)..." -ForegroundColor Yellow
Start-Process -FilePath "cmd.exe" -ArgumentList "/c npm run dev" -WorkingDirectory "$PSScriptRoot\emall-client" -WindowStyle Hidden

# 5. 启动后台管理 (emall-admin)
Write-Host "[5/5] 启动后台管理系统 (Port 5174)..." -ForegroundColor Yellow
Start-Process -FilePath "cmd.exe" -ArgumentList "/c npm run dev" -WorkingDirectory "$PSScriptRoot\emall-admin" -WindowStyle Hidden

Start-Sleep -Seconds 3
Write-Host "=========================================" -ForegroundColor Green
Write-Host "  全部服务已成功触发启动！" -ForegroundColor Green
Write-Host "  - 前台商城:  http://localhost:5173" -ForegroundColor Green
Write-Host "  - 管理后台:  http://localhost:5174" -ForegroundColor Green
Write-Host "  - 后端接口:  http://localhost:8080" -ForegroundColor Green
Write-Host "  - MySQL:     localhost:3306 (库: emall_db)" -ForegroundColor Green
Write-Host "  - Redis:     localhost:6379" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
