# stop-all.ps1 - 一键停止所有服务
Write-Host "正在停止所有相关进程..." -ForegroundColor Yellow
Get-Process -Name mysqld, redis-server -ErrorAction SilentlyContinue | Stop-Process -Force
Get-Process -Name java, node -ErrorAction SilentlyContinue | Stop-Process -Force
Write-Host "所有服务已停止。" -ForegroundColor Green
