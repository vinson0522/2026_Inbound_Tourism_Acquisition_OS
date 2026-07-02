# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-06 | EPIC-3 M2 · FR-203 · ADR-20260706-19 |

## 上下文

关键词 CRUD + generate 已在 `ruoyi-project` · `KeywordOpportunityServiceImpl`。M2 增加评分 API，Feign 调 Python `/ai/keywords/score`，事务内更新 `score` / `score_detail_json`。

**相关文件**：
- `KeywordOpportunityServiceImpl.java` · `KeywordOpportunityController`（或 project 下 keywords 路由）
- `inbound-ai` AiServiceClient（新增 score 方法）
- `database/ddl/001_schema.sql` — `keyword_opportunity.score` · `score_detail_json`

**约束**：租户隔离 · 项目归属校验 · **不**在 generate 流程自动扣额（M2 单独 endpoint；额度键若需可复用 `keywords_per_month` 或 M2 暂不计费 — 见 ADR-19）

## 交付请求

**验收标准**：
- [x] `POST /api/v1/projects/{projectId}/keywords/{keywordId}/score` — 单条刷新
- [x] `POST /api/v1/projects/{projectId}/keywords/score-batch` — body: `{ "keywordIds": [1,2,3] }` 或空=当前项目 ACTIVE 全量（上限 50）
- [x] 组装 AI 请求：keyword 字段 + 项目 brand + 竞品名列表 + 可选最近 SUCCESS diagnostic `geo_score`
- [x] 写库：`score` · `score_detail_json` · `updated_at`
- [x] Feign：`AiServiceClient.keywordsScore(...)` → `/ai/keywords/score`
- [x] smoke：`deploy/scripts/test_keywords_score.py` — login → 取 project 1 某 keyword → score → GET list 断言 score 非 null
- [x] 列表 API 已有 score 字段；sort `orderByColumn=score&isAsc=desc` 使用 `NULLS LAST`（已评分优先）

## Prompt

```
角色：开发 Java。必读 KeywordOpportunityServiceImpl、AiServiceClient、ADR-19。
任务：单条+批量 score API · Feign Python · smoke test_keywords_score.py。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-06
- **结果摘要**：`KeywordOpportunityController` 单条/批量 score · `KeywordOpportunityServiceImpl.scoreEntity` Feign → `/ai/keywords/score` · 写 `score`/`score_detail_json` · 列表默认与 `orderByColumn=score` 均 `DESC NULLS LAST` · smoke `test_keywords_score.py` ✅
- **遗留**：M2 暂不计费（ADR-19）；Admin score UI → P1 HANDOFF
