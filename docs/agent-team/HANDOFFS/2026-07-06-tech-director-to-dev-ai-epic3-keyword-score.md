# HANDOFF | 技术总监 → 开发（Python / inbound-ai）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-06 | EPIC-3 M2 · FR-203 · ADR-20260706-19 |

## 上下文

EPIC-3 M1 已有 `POST /ai/keywords/generate`。Admin 列表「机会分」列为占位（`score == null` → `—`）。M2 需真实评分写回 PG。

**相关文件**：
- `inbound-ai/app/routers/keywords.py` · `keywords_service.py`
- `inbound-ai/app/models/keywords.py`
- `PRD_商业化版_V2.0.md` FR-203
- ADR-19 五维权重

**约束**：Prompt/权重读 `template` 表（`keyword_score_v1`）；禁止硬编码 Prompt 字符串在 router。

## 交付请求

实现 **`POST /ai/keywords/score`**（内网 Token）。

**验收标准**：
- [ ] Request：`tenant_id`, `project_id`, `keyword_id`, `keyword`, `keyword_en`, `stage`, `market`, 可选 `brand_name`, `competitors[]`, 可选 `geo_score`
- [ ] Response：`score` (0–100) · `score_detail` 五维 + `weights_version` · `needs_human_review=false`
- [ ] 逻辑：优先 LLM 结构化输出五维 → 按 ADR-19 权重聚合；`KEYWORD_SCORE_MOCK_LLM=true` 或缺 Key 时 deterministic mock
- [ ] 可选 RAG top-3 注入 relevance 上下文（与 generate 一致）
- [ ] pytest：`tests/test_keywords_score.py` ≥4 cases（mock · 权重边界 · 缺字段 400 · stage 非法）
- [ ] README 或 `.env.example` 文档化 `KEYWORD_SCORE_MOCK_LLM`

## Prompt

```
角色：开发 inbound-ai。必读 keywords_service.py、ADR-19、template_service。
任务：POST /ai/keywords/score · 5 维加权 · mock 可测 · pytest。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-06
- **结果摘要**：`POST /ai/keywords/score` · ADR-19 五维加权 · template `keyword_score_v1` · mock · pytest 8 passed
- **遗留**：Java Feign + Admin 列表（并行 HANDOFF）
