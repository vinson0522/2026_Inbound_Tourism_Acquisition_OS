#Requires -Version 5.1
<#
.SYNOPSIS
  分级清理本地 Docker 开发环境
.PARAMETER Level
  A = 仅 stop 容器
  B = down + 删除 D 盘 inbound-growth 数据（默认，推荐）
  C = B + 删除常见镜像 + prune
.USAGE
  cd deploy
  .\scripts\local_docker_cleanup.ps1
  .\scripts\local_docker_cleanup.ps1 -Level C
#>
param(
    [ValidateSet("A", "B", "C")]
    [string]$Level = "B",
    [string]$DataRoot = "D:\Dev\SDKs\Docker\inbound-growth",
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$DeployRoot = Split-Path -Parent $PSScriptRoot
Set-Location $DeployRoot

$compose = @("-f", "docker-compose.yml", "-f", "docker-compose.local-d.yml")

function Confirm-Action([string]$msg) {
    if ($Force) { return }
    $r = Read-Host "$msg [y/N]"
    if ($r -notmatch '^[yY]') {
        Write-Host "已取消"
        exit 0
    }
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Warning "docker 未安装，仅尝试删除数据目录"
}

switch ($Level) {
    "A" {
        Confirm-Action "停止容器（保留 $DataRoot 数据）?"
        if (Get-Command docker -ErrorAction SilentlyContinue) {
            docker compose @compose stop
        }
        Write-Host "[done] 级别 A — 容器已停止"
    }
    "B" {
        Confirm-Action "停止容器并删除 $DataRoot 下全部数据?"
        if (Get-Command docker -ErrorAction SilentlyContinue) {
            docker compose @compose down --remove-orphans 2>$null
        }
        if (Test-Path $DataRoot) {
            Remove-Item -Recurse -Force $DataRoot
            Write-Host "[done] 已删除 $DataRoot"
        } else {
            Write-Host "[skip] 数据目录不存在: $DataRoot"
        }
        Write-Host "[done] 级别 B — 项目数据已重置。重建: .\scripts\local_docker_bootstrap.ps1"
    }
    "C" {
        Confirm-Action "级别 C：删除数据 + 清理 inbound 相关镜像?"
        if (Get-Command docker -ErrorAction SilentlyContinue) {
            docker compose @compose down --remove-orphans 2>$null
        }
        if (Test-Path $DataRoot) {
            Remove-Item -Recurse -Force $DataRoot
        }
        if (Get-Command docker -ErrorAction SilentlyContinue) {
            docker images --format "{{.Repository}}:{{.Tag}}" | Where-Object {
                $_ -match '^(deploy-|inbound-|pgvector/pgvector|redis:|rabbitmq:)'
            } | ForEach-Object {
                docker rmi $_ 2>$null
            }
            docker image prune -f | Out-Null
        }
        Write-Host "[done] 级别 C — 数据与镜像已清理"
    }
}

Write-Host ""
Write-Host "更深清理见 LOCAL_DOCKER.md §5 级别 D/E（Docker Desktop 全盘 / 卸载）"
