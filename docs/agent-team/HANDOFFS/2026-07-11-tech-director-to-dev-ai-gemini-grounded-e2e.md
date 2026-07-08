# HANDOFF | 技术总监 → 开发（Python / AI）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 Python | 2026-07-11 | B-03b · **⏸ 挂起 B-23** · [重排](2026-07-11-tech-director-sprint2-reprioritize-no-third-party.md) |

> **2026-07-11 技术总监：本任务挂起。** 本地 smoke 默认 `DIAGNOSE_MOCK_LLM=true` 9/9 已绿。**勿反复试 Gemini 配额**直至 Key/配额恢复。

## 上下文

本地 smoke 9/9 默认 `DIAGNOSE_MOCK_LLM=true`（docker `docker-compose.local-d.yml`）。**真 Gemini grounded-api** 曾因 free tier 配额 FAILED（ai-api 日志 QuotaFailure）。

**相关文件**：
- `inbound-ai/app/services/diagnose_service.py` — grounding 校验 · mock 分支
- `deploy/docker-compose.local-d.yml` — `DIAGNOSE_MOCK_LLM`
- `deploy/scripts/test_diagnostic_e2e.py`
- `deploy/scripts/run_smoke_regression.ps1` — 维护默认 mock

## 交付请求

- [ ] 文档：`deploy/LOCAL_DOCKER.md` 或 `inbound-ai/README.md` 增补 **真 GEO E2E** 小节（env · 配额 · 失败排查）
- [ ] 尝试：`DIAGNOSE_MOCK_LLM=false` · `GEMINI_API_KEY` 有效 · docker ai-api recreate
- [ ] 跑 `python deploy/scripts/test_diagnostic_e2e.py` → 期望 **SUCCESS** + geo_score + grounded citations
- [ ] 若配额仍用尽：**不 hack** · HANDOFF Done 附 ai-api 日志摘要 + 建议重试时间
- [ ] 可选：`deploy/scripts/test_diagnostic_e2e_real.py` 包装脚本（显式 mock off · 长 poll）

## 约束

- GEO 诊断禁止裸 Chat Completions 冒充 grounded（CLAUDE.md §5.3）
- mock 路径保留为本地默认 smoke；真 E2E 为 **opt-in**

## Prompt

```
角色：开发 Python。必读 diagnose_service、LOCAL_DOCKER、HANDOFF 2026-07-11-tech-director-to-dev-ai-gemini-grounded-e2e.md、MEMORY B-03b。
任务：真 Gemini grounded E2E 文档+验证 · 或 quota 阻塞证据。
```

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
