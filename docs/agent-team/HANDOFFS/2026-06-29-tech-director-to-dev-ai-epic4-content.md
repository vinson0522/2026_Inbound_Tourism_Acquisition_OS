# HANDOFF | 技术总监 → 开发（inbound-ai）

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-4 M1 · **FR-301/302** · ADR-20260629-12 |

## 上下文

**当前状态**：`keywords_service` + `template_service` 模式可复用；RAG search 已通。

**相关文件**：
- `inbound-ai/app/services/keywords_service.py` — 参考结构
- `inbound-ai/app/services/template_service.py`
- `PRD_商业化版_V2.0.md` §20.2 内容输出字段
- `generated_content` DDL — script, hook, storyboard_json, needs_human_review

**约束**：
- M1 **单步** service（非完整 LangGraph）；M2 再引 LangGraph Agent
- Prompt 从 `template` 表读 `content_script_v1` 或 config fallback
- 输出 `needs_human_review: true`；RAG 注入标注 chunk_id
- mock 模式：`CONTENT_MOCK_LLM` 或无 Key

## 交付请求

**需要什么**：`POST /ai/content/generate`

**请求体（建议）**：
```json
{
  "tenant_id": 1,
  "project_id": 1,
  "keyword_id": 1,
  "keyword_text": "...",
  "platform": "youtube_shorts",
  "duration_sec": 30,
  "tone": "friendly",
  "language": "en",
  "target_market": "US",
  "use_rag": true,
  "trace_id": "..."
}
```

**验收标准**：
- [x] Router + models + `content_service.py`
- [x] 响应：`hook`, `script`, `voiceover`, `on_screen_text`, `cta`, `storyboard_json[]`（scene/duration/visual/note）
- [x] `needs_human_review: true`；可选 `chunk_ids[]`
- [x] `tests/test_content_generate.py` mock 断言结构
- [x] `uv run pytest tests/test_content_generate.py -q` 通过

## 窗口激活 Prompt 摘要

```
角色：开发 inbound-ai。必读 EPIC-4 content HANDOFF 与 keywords_service 模式。
任务：POST /ai/content/generate；mock 可测；Prompt 读 template。
```

---

## Done（由 To 角色填写）

- **完成时间**：2026-07-01
- **结果摘要**：
  - **API**：`POST /ai/content/generate` · 内网 Token · camelCase 请求别名
  - **输出**：`hook` / `script` / `voiceover` / `on_screen_text` / `cta` / `storyboard_json[]` + `title` / `target_audience` / `hashtags` / `landing_page_suggestion`（DDL 对齐）
  - **Mock**：`CONTENT_MOCK_LLM` 或无 Key → `capture_method=content-mock`；可选 RAG top-3
  - **Prompt**：`template_service.load_content_script_prompt` → `content_script_v1` + config fallback
  - **测试**：`tests/test_content_generate.py` **6 passed**（C8 `23a46f6`）
- **遗留**：M2 LangGraph Agent；MQ `ai.content`；分镜导出 FR-303
