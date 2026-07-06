# Full smoke 10/10 regression (ADR-09 local Docker + Java :8080 + ai-api :8090)
# Usage: .\deploy\scripts\run_smoke_regression.ps1
#
# Prerequisites (see deploy/LOCAL_DOCKER.md §2.9 and deploy/scripts/README.md):
#   docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.local-d.yml up -d postgres redis rabbitmq ai-api gotenberg
#   inbound-core on :8080
#   import_ruoyi_pg_local.ps1 (+ 006_fr807_tenant_mapping.sql if tenant B missing)
# Mock: DIAGNOSE_MOCK_LLM=true EMBED_MOCK=true (local-d.yml defaults + set below)
$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $Root

$env:EMBED_MOCK = "true"
$env:DIAGNOSE_MOCK_LLM = "true"

$scripts = @(
    "test_projects_api.py",
    "test_diagnostic_e2e.py",
    "test_embed_e2e.py",
    "test_diagnostic_report_export.py",
    "test_diagnostic_trends.py",
    "test_ai_health.py",
    "test_knowledge_rag_search.py",
    "test_keywords_api.py",
    "test_content_api.py",
    "test_tenant_isolation.py"
)

$failed = @()
foreach ($s in $scripts) {
    Write-Host "`n=== $s ===" -ForegroundColor Cyan
    python "deploy/scripts/$s"
    if ($LASTEXITCODE -ne 0) {
        $failed += $s
    }
}

Write-Host "`n=== SMOKE SUMMARY ===" -ForegroundColor Yellow
$pass = $scripts.Count - $failed.Count
Write-Host "$pass/$($scripts.Count) passed"
if ($failed.Count -gt 0) {
    Write-Host "FAILED: $($failed -join ', ')" -ForegroundColor Red
    Write-Host "Hint: ensure ai-api has DIAGNOSE_MOCK_LLM=true and diagnose worker (docker-compose.local-d.yml)"
    exit 1
}
Write-Host "All smoke checks passed." -ForegroundColor Green
exit 0
