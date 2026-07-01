# HANDOFF | 技术总监 → 开发（Java）

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-4 M1 · **FR-301/302** · ADR-20260629-12 |

## 上下文

**当前状态**：关键词模块在 `ruoyi-project`；`content_task` / `generated_content` DDL 已存在。

**相关文件**：
- `database/ddl/001_schema.sql` — `content_task`, `generated_content`, `content_task_status`
- `KeywordOpportunityController` — 参考 CRUD + Feign 模式
- Python `/ai/content/generate`（AI HANDOFF）

**约束**：
- Java：CRUD、调 AI、双表落库；**不**拼 Prompt
- 创建任务时校验 `keyword_id` 归属同一 `project_id`
- 生成：M1 **同步** Feign；status `DRAFT` → 生成后仍 DRAFT + `needs_human_review`
- 租户：`tenant.excludes` 若需追加 `content_task` / `generated_content`

## 交付请求

**需要什么**：内容任务 CRUD + 脚本生成 API。

**验收标准**：
- [ ] `ContentTaskController`（建议并入 `ruoyi-project` 或独立 `ruoyi-content`）
- [ ] `GET/POST/DELETE /api/v1/projects/{projectId}/content-tasks` — 分页、status 筛选
- [ ] `GET .../content-tasks/{taskId}` — 含最新 `generated_content`
- [ ] `POST .../content-tasks/{taskId}/generate` — Feign → insert/update `generated_content`
- [ ] Entity/Vo 对齐 DDL；MapStruct
- [ ] smoke：`deploy/scripts/test_content_api.py`

## 窗口激活 Prompt 摘要

```
角色：开发 inbound-core。必读 content_task DDL 与 AI content HANDOFF。
任务：content CRUD + generate；keyword 归属校验。本机 Docker ADR-09。
```

---

## Done（由 To 角色填写）

- **完成时间**：2026-07-01
- **结果摘要**：
  - `ContentTaskController` 并入 `ruoyi-project`：`GET/POST/DELETE /api/v1/projects/{projectId}/content-tasks`、详情含 `generatedContent`、`POST .../generate` → Feign `/ai/content/generate`
  - `ruoyi-ai-client`：`ContentGenerateRequest/Data`、`StoryboardScene`、`AiServiceClient.contentGenerate()`
  - `tenant.excludes` 追加 `content_task`、`generated_content`；`PgContentTaskStatusTypeHandler` + `PgJsonbListMapTypeHandler`
  - `deploy/scripts/test_content_api.py` ✅（C8 `23a46f6` · Docker ai-api + Java :8080）
- **遗留**：M1 生成后 task status 仍 `DRAFT`（ADR-12）
