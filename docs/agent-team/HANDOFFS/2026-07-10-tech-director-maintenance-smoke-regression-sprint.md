# 维护轨 Sprint | smoke 9/9 回归（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-10 |
| **优先级** | High |
| **关联** | 完整版路线图关闭后维护 · B-21 |
| **前置** | C23 `cfda3a7` · 路线图 #1–#7 ✅ |

## 背景

EPIC-2 M3 关闭后进入维护轨。技术总监复核 smoke 9/9（2026-07-10）：

| # | 脚本 | 结果 | 说明 |
|---|------|:----:|------|
| 1 | `test_projects_api.py` | ✅ | project id=4 |
| 2 | `test_diagnostic_e2e.py` | ⏳ | 未跑（~240s · 依赖 MQ mock LLM） |
| 3 | `test_embed_e2e.py` | ❌ | direct embed HTTP 500 |
| 4 | `test_diagnostic_report_export.py` | ❌ | runId=2 非 SUCCESS · DOCX 返回 JSON 83B |
| 5 | `test_diagnostic_trends.py` | ✅ | 2 点 ASC |
| 6 | `test_ai_health.py` | ✅ | litellm=no_key |
| 7 | `test_knowledge_rag_search.py` | ❌ | RAG 500 |
| 8 | `test_keywords_api.py` | ✅ | insertedCount=3 |
| 9 | `test_content_api.py` | ✅ | taskId=7 |

**附加**：`test_diagnostic_schedule.py` ✅ runId=13（C23 验收）

**当前**：**6/9 确认通过** + schedule ✅ · 3 项失败均与 **AI 服务 embed/RAG** 或 **历史 run 状态** 相关，非 C23 回归。

## 目标

1. 恢复 smoke **9/9 全绿**（本机 Docker + Java :8080 + inbound-ai :8090）
2. 并行：扩展 **真 Perplexity hook**（EPIC-11 维护 · B-03b 相关）

## 任务拆分

| # | 角色 | HANDOFF | 验收 |
|---|------|---------|------|
| **1** | **开发 Python** | [→ embed/RAG 修复](2026-07-10-tech-director-to-dev-ai-maintenance-smoke-fix.md) | embed + rag smoke ✅ |
| **2** | **开发 Java** | [→ 报告导出前置 run](2026-07-10-tech-director-to-dev-java-maintenance-report-smoke.md) | report export smoke ✅ |
| **3** | **开发 Extension** | [→ Perplexity hook](2026-07-10-tech-director-to-dev-extension-perplexity-hook.md) | 探针 E2E 真 citations |

## 窗口派发（复制到各 Cursor 窗口）

| # | 窗口 | HANDOFF | 激活 Prompt（Custom Instructions 首行） |
|---|------|---------|----------------------------------------|
| **1** | **开发 Python** | [→ AI smoke fix](2026-07-10-tech-director-to-dev-ai-maintenance-smoke-fix.md) | `角色：开发 Python。必读 MEMORY.md B-21、deploy/scripts/test_embed_e2e.py、test_knowledge_rag_search.py。任务：修复 embed direct 500 与 RAG search 500；验收两脚本通过。` |
| **2** | **开发 Java** | [→ report smoke](2026-07-10-tech-director-to-dev-java-maintenance-report-smoke.md) | `角色：开发 Java。必读 test_diagnostic_report_export.py、DiagnosticRun runId=2 状态。任务：确保 smoke 有可导出 SUCCESS run（seed 或 mock 触发）；test_diagnostic_report_export 通过。` |
| **3** | **开发 Extension** | [→ Perplexity hook](2026-07-10-tech-director-to-dev-extension-perplexity-hook.md) | `角色：开发 Extension。必读 inbound-probe-extension perplexity adapter、MEMORY B-03b。任务：真 Perplexity 页面 hook 抓取 citations；test_probe_extension_e2e 或手工证据。` |

**并行**：#1 与 #2 可并行；#3 独立。

## 完成后

- smoke：`deploy/scripts/run_smoke_regression.ps1` ✅ **9/9**（2026-07-10 · diagnostic_e2e runId=7 geo=85）
- B-21 ✅ 关闭 · B-22 ✅ **C24** `6f4738a` · `fix(ai,deploy,extension): restore smoke 9/9 regression defaults`
