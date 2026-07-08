# deploy/scripts — 运维与 smoke 脚本索引

> 本地 ADR-09 路径见 [`../LOCAL_DOCKER.md`](../LOCAL_DOCKER.md) · 远程服务器见 [`../README.md`](../README.md)

---

## 一键回归（维护轨默认）

| 脚本 | 说明 |
|------|------|
| [`run_smoke_regression.ps1`](run_smoke_regression.ps1) | **10/10** 串联 smoke（mock LLM · 无需三方 Key） |

**前提**：`postgres` · `redis` · `rabbitmq` · `ai-api` healthy · Java `:8080` · 建议 `gotenberg`（PDF 项）

```powershell
cd deploy
docker compose -f docker-compose.yml -f docker-compose.local-d.yml up -d postgres redis rabbitmq ai-api gotenberg
# 终端 2：inbound-core spring-boot:run
.\scripts\run_smoke_regression.ps1
```

会话内强制：`DIAGNOSE_MOCK_LLM=true` · `EMBED_MOCK=true`（与 `docker-compose.local-d.yml` ai-api 默认一致）。

---

## 10/10 回归清单（`run_smoke_regression.ps1`）

| # | 脚本 | FR / EPIC | 依赖 |
|---|------|-----------|------|
| 1 | `test_projects_api.py` | FR-001 项目 | Java · Redis |
| 2 | `test_diagnostic_e2e.py` | EPIC-2 GEO | Java · ai-api mock diagnose |
| 3 | `test_embed_e2e.py` | EPIC-10 embed | Java · ai-api `EMBED_MOCK=true` |
| 4 | `test_diagnostic_report_export.py` | FR-106 报告 | Java · runId=2 · DOCX 默认 |
| 5 | `test_diagnostic_trends.py` | FR-108 趋势 | Java · ≥2 SUCCESS run |
| 6 | `test_ai_health.py` | ai-api | `:8090` |
| 7 | `test_knowledge_rag_search.py` | FR-005 RAG | Java · asset#1 |
| 8 | `test_keywords_api.py` | EPIC-3 关键词 | Java · ai-api |
| 9 | `test_content_api.py` | EPIC-4 内容 | Java · ai-api |
| 10 | `test_tenant_isolation.py` | **FR-807 租户** | Java · tenant B seed · admin→403 |

---

## FR-807 租户 smoke

| 脚本 | 说明 |
|------|------|
| [`test_tenant_isolation.py`](test_tenant_isolation.py) | tenant A (`admin`) 读 tenant B 资源 → **403**（4 端点） |

**Seed**：`002_seed_demo.sql` + 存量库 `006_fr807_tenant_mapping.sql`

| 账号 | 用户 | 密码 | RuoYi tenant |
|------|------|------|--------------|
| Tenant A（测试登录） | `admin` | `admin123` | `000000` |
| Tenant B（对拍） | `tenantb` | `admin123` | `000001` |

| 环境变量 | 默认 | 说明 |
|----------|------|------|
| `TENANT_B_PROJECT_ID` | `8` | tenant B 项目 id |
| `TENANT_B_RUN_ID` | `100` | tenant B 诊断 run id |

```powershell
python deploy/scripts/test_tenant_isolation.py --verbose
```

---

## 营销门户 smoke（Sprint #5 A1 · 非 10/10）

| 脚本 | 说明 | 在 10/10？ |
|------|------|:----------:|
| [`test_marketing_contact.py`](test_marketing_contact.py) | 营销联系表单 `POST /api/v1/public/marketing-contact`：happy 200 + leadId · 缺姓名/缺联系方式拒绝 · 无 auth/无配额消耗 | 否 |

```powershell
python deploy/scripts/test_marketing_contact.py
```

---

## 探针 / 扩展 smoke（非 10/10 · fixture 默认绿）

| 脚本 | 说明 | 在 10/10？ |
|------|------|:----------:|
| [`test_probe_extension_e2e.py`](test_probe_extension_e2e.py) | EPIC-11 扩展 poll → mock hook 结果 | 否 |
| [`test_probe_calibration.py`](test_probe_calibration.py) | EPIC-11 M2 adapter + calibration | 否 |

---

## Opt-in · B-23 三方 live（挂起 · 手动执行）

> **不纳入** `run_smoke_regression.ps1`。无 Perplexity 登录 / Gemini 配额时 **跳过**，不阻塞 MVP。

| 脚本 | 说明 | 恢复条件 |
|------|------|----------|
| [`verify_perplexity_live.py`](verify_perplexity_live.py) | 真 Chrome + MV3 扩展 + Perplexity hook | B-23 · perplexity.ai 可登录 |
| （流程）Gemini grounded E2E | `DIAGNOSE_MOCK_LLM=false` + 真实 Key | B-23 · Gemini 配额 |

Fixture / mock 验收仍用 `test_probe_extension_e2e.py` 与 `DIAGNOSE_MOCK_LLM=true`。

---

## EPIC 扩展 smoke（按需单跑 · 不在 10/10）

| 脚本 | EPIC / FR |
|------|-----------|
| `test_landing_api.py` | EPIC-6 M1 landing CRUD |
| `test_landing_publish_e2e.py` | EPIC-6 M2 publish + leads |
| `test_landing_astro_e2e.py` | EPIC-6 M2 Astro `:4321` |
| `test_public_leads_api.py` | EPIC-7 M1 public leads |
| `test_leads_crm.py` | EPIC-7 M2 CRM |
| `test_leads_whatsapp_ai.py` | EPIC-7 M3 WhatsApp + AI |
| `test_reports_api.py` | EPIC-8 M1 reports |
| `test_reports_monthly.py` | EPIC-8 M2 monthly |
| `test_billing_quota.py` | EPIC-9 M1 quota |
| `test_billing_period_reset.py` | EPIC-9 M2 subscription |
| `test_material_breakdown.py` | EPIC-5 viral |
| `test_keywords_score.py` | EPIC-3 M2 score |
| `test_diagnostic_schedule.py` | EPIC-2 M3 schedule |

---

## 本地 Docker 运维

| 脚本 | 说明 |
|------|------|
| [`local_docker_bootstrap.ps1`](local_docker_bootstrap.ps1) | 一键启动 compose 最小集 |
| [`local_docker_cleanup.ps1`](local_docker_cleanup.ps1) | 分级清理 Level A–E |
| [`import_ruoyi_pg_local.ps1`](import_ruoyi_pg_local.ps1) | 本机导入若依 PG 表 |
| [`configure_llm_keys.py`](configure_llm_keys.py) | 远程服务器写入 LLM Key |
| [`admin_ui_walkthrough.py`](admin_ui_walkthrough.py) | Admin API 走查（非断言 smoke） |

---

## 远程服务器（staging `18.139.209.10`）

| 脚本 | 说明 |
|------|------|
| [`server_infra_deploy.py`](server_infra_deploy.py) | SSH 上传 deploy + bootstrap |
| [`server_infra_verify.py`](server_infra_verify.py) | 远程 compose 健康检查 |
| [`server_ai_deploy.py`](server_ai_deploy.py) | 上传 inbound-ai + 重建 ai-api |
| [`import_ruoyi_pg.py`](import_ruoyi_pg.py) | 远程导入若依表 |
| [`export_infra_credentials.py`](export_infra_credentials.py) | 拉取 `.env` → `INFRA_ACCESS.local.md` |
| [`fix_minio_bucket.py`](fix_minio_bucket.py) | MinIO bucket 一次性修复 |
| [`port_probe.py`](port_probe.py) / `port_probe_deep.py` / `port_probe_internal.py` | 端口探测 |
| [`server_audit.py`](server_audit.py) / `server_audit_quick.py` | SSH 审计 |

---

*Last updated: 2026-07-11*
