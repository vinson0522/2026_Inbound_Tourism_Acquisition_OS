# HANDOFF | 技术总监 → 开发（inbound-ai）

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-3 M1 · **FR-201** · ADR-20260629-11 |

## 上下文

**当前状态**：`inbound-ai` 已有 LLM gateway、RAG search；无 keywords router。

**相关文件**：
- `inbound-ai/app/routers/llm.py`, `services/llm_gateway.py`
- `inbound-ai/app/deps.py` — `verify_internal_token`
- `PRD_商业化版_V2.0.md` §9 — 八阶段、机会词字段
- `template` 表 — Prompt **应从 DB 读**；M1 可 seed 一条 `keyword_generate_v1` 或 config 占位 + TODO

**约束**：
- 输出 `needs_human_review: true`（PRD AI 内容规范）
- 可选 RAG：`tenant_id` + `project_id` 检索 top-3 chunks 注入
- **不算** FR-203 完整评分；返回 `suggested_score: null` 或 0–100 占位

## 交付请求

**需要什么**：`POST /ai/keywords/generate` — 按项目上下文生成八阶段机会词。

**请求体（建议）**：
```json
{
  "tenant_id": 1,
  "project_id": 1,
  "market": "US",
  "locale": "en",
  "stages": ["INSPIRATION", "..."],
  "words_per_stage": 5,
  "use_rag": true,
  "trace_id": "..."
}
```

**验收标准**：
- [x] Router + Pydantic models + service
- [x] 响应：`stages[]` → `{ stage, keywords[]: { text, rationale?, chunk_ids? } }`
- [x] 内网 Token 校验；无 Key 时 mock 模式（与 diagnose 一致）
- [x] `tests/test_keywords_generate.py` — mock LLM 断言结构 + 八阶段 key 存在
- [x] README 或 `.env.example` 补充说明

## 质量 / 证据

**必须提供**：`uv run pytest tests/test_keywords_generate.py -q` 通过

**交给下一棒**：[开发 Java EPIC-3](2026-06-29-tech-director-to-dev-java-epic3-keywords.md)

## 窗口激活 Prompt 摘要

```
角色：开发 inbound-ai。必读 HANDOFF epic3-keywords-ai 与 llm_gateway 模式。
任务：实现 POST /ai/keywords/generate；mock 可测；Prompt 勿硬编码长文本（seed template 或短占位）。
```

---

## Done（由 To 角色填写）

- **完成时间**：2026-06-29
- **结果摘要**：
  - `POST /ai/keywords/generate` — Router + Pydantic + `keywords_service`
  - 请求：`tenantId`/`projectId`/`market`/`stages`/`wordsPerStage`/`useRag`/`traceId`
  - 响应：`stages[]` + `needs_human_review`；`suggested_score=null`（ADR-11）
  - `KEYWORDS_MOCK_LLM` 或 **无 LLM Key** → mock；可选 RAG top-3
  - Prompt：`template_service` 读 `template.name=keyword_generate_v1`，fallback 短占位 config
  - `uv run pytest tests/test_keywords_generate.py -q` → **7 passed**
- **遗留**：DB seed `keyword_generate_v1` template；FR-203 评分；MQ 异步生成
