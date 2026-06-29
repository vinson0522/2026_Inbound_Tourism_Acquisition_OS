#Requires -Version 5.1
<#
.SYNOPSIS
  一键启动 D 盘本地 Docker 基础设施（EPIC-2 最小集）
.USAGE
  cd deploy
  .\scripts\local_docker_bootstrap.ps1
#>
param(
    [string]$DataRoot = "D:\Dev\SDKs\Docker\inbound-growth"
)

$ErrorActionPreference = "Stop"
$DeployRoot = Split-Path -Parent $PSScriptRoot
Set-Location $DeployRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "未找到 docker 命令。请先安装 Docker Desktop，见 LOCAL_DOCKER.md §2"
}

@(
    "$DataRoot\postgres",
    "$DataRoot\redis",
    "$DataRoot\rabbitmq"
) | ForEach-Object {
    New-Item -ItemType Directory -Force -Path $_ | Out-Null
}

$envFile = Join-Path $DeployRoot ".env"
$example = Join-Path $DeployRoot ".env.local.example"
if (-not (Test-Path $envFile) -and (Test-Path $example)) {
    Copy-Item $example $envFile
    Write-Host "[ok] 已从 .env.local.example 创建 deploy/.env — 请填入 GEMINI_API_KEY"
}

if (-not $env:INBOUND_DOCKER_DATA) {
    $env:INBOUND_DOCKER_DATA = ($DataRoot -replace '\\', '/')
}

Write-Host "[start] INBOUND_DOCKER_DATA=$($env:INBOUND_DOCKER_DATA)"
docker compose -f docker-compose.yml -f docker-compose.local-d.yml up -d postgres redis rabbitmq ai-api gotenberg

Write-Host ""
Write-Host "等待 healthcheck..."
Start-Sleep -Seconds 8
docker compose -f docker-compose.yml -f docker-compose.local-d.yml ps

Write-Host ""
Write-Host "下一步:"
Write-Host "  1. 首次: .\scripts\import_ruoyi_pg_local.ps1"
Write-Host "  2. 验通: curl.exe http://localhost:8090/health ; curl.exe http://localhost:3002/health"
Write-Host "  3. 起 Java/Admin，见 LOCAL_DOCKER.md §2.8"
