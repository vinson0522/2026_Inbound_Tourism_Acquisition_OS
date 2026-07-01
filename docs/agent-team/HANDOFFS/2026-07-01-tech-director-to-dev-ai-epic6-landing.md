# HANDOFF | 技术总监 → 开发（inbound-ai）

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-07-01 |
| **优先级** | High |
| **关联** | EPIC-6 M1 · **FR-502~504** · ADR-20260701-13 |

## 上下文

**当前状态**：`content_service` + `template_service` + RAG 可复用。

**相关文件**：
- `inbound-ai/app/services/content_service.py` — 参考
- `PRD_商业化版_V2.0.md` §20.3 落地页 `content_json` 模块
- `landing_page` DDL — `content_json`, `seo_meta_json`

**约束**：
- M1 单步 service；Prompt 读 `landing_generate_v1`
- RAG 必须带 `tenant_id` + `project_id`；chunk_ids 标注
- `needs_human_review: true`；`LANDING_MOCK_LLM` mock 模式

## 交付请求

**需要什么**：`POST /ai/landing/generate`

**验收标准**：
- [ ] Router + models + `landing_service.py`
- [ ] 响应：`title`, `content_json`（modules[]）, `seo_meta_json`, `form_config_json` 建议, `needs_human_review`
- [ ] `tests/test_landing_generate.py` mock 断言模块 key
- [ ] `uv run pytest tests/test_landing_generate.py -q` 通过

## 窗口激活 Prompt 摘要

```
角色：开发 inbound-ai。必读 EPIC-6 landing HANDOFF 与 content_service 模式。
任务：POST /ai/landing/generate；PRD §20.3 模块结构；mock 可测。
```

---

## Done（由 To 角色填写）

- **完成时间**：2026-07-01
- **结果摘要**：
  - `POST /ai/landing/generate` — router + `landing_service.py` + models
  - PRD §20.3 八模块 `content_json.modules[]`（hero … whatsapp_cta）
  - `seo_meta_json` / `form_config_json`；`needs_human_review: true`
  - `LANDING_MOCK_LLM` + `landing_generate_v1` template loader
  - `tests/test_landing_generate.py` — 6 passed
- **遗留**：Java Feign + Admin 预览（→ Java/UI HANDOFF）
