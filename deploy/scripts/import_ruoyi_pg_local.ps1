#Requires -Version 5.1
<#
.SYNOPSIS
  向本地 Docker PostgreSQL 导入若依系统表
.USAGE
  cd deploy
  .\scripts\import_ruoyi_pg_local.ps1
#>
$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$DeployRoot = Join-Path $Root "deploy"
$Container = "inbound-postgres"

$sqlFiles = @(
    (Join-Path $Root "inbound-core\script\sql\postgres\postgres_ry_vue_5.X.sql"),
    (Join-Path $Root "inbound-core\script\sql\postgres\postgres_ry_workflow.sql")
)

if (-not (docker ps --format "{{.Names}}" | Select-String -Pattern "^${Container}$" -Quiet)) {
    Write-Error "容器 $Container 未运行。请先: .\scripts\local_docker_bootstrap.ps1"
}

Write-Host "[check] before import"
docker exec $Container psql -U inbound -d inbound_growth -tAc `
    "SELECT to_regclass('public.sys_user'), count(*) FROM pg_tables WHERE schemaname='public';"

foreach ($sql in $sqlFiles) {
    if (-not (Test-Path $sql)) {
        Write-Warning "SKIP missing: $sql"
        continue
    }
    $name = Split-Path $sql -Leaf
    $remote = "/tmp/$name"
    Write-Host "[import] $name"
    docker cp $sql "${Container}:${remote}"
    docker exec $Container psql -U inbound -d inbound_growth -v ON_ERROR_STOP=1 -f $remote
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Import failed: $name"
    }
}

Write-Host ""
Write-Host "[check] after import"
docker exec $Container psql -U inbound -d inbound_growth -tAc `
    "SELECT to_regclass('public.sys_user'), count(*) FROM pg_tables WHERE schemaname='public';"
Write-Host "[done] RuoYi system tables imported"
