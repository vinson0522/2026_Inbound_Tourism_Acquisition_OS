# HANDOFF | 技术总监 → 开发（Java）

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-07-01 |
| **优先级** | High |
| **关联** | EPIC-6 M1 · **FR-501~505** · ADR-20260701-13 |

## 上下文

**当前状态**：`ContentTaskServiceImpl` / `KeywordOpportunity` 模式可复用；`landing_page` DDL 已存在。

**相关文件**：
- `database/ddl/001_schema.sql` — `landing_page`, `landing_page_status`
- `ContentTaskController` — Feign + tenant.excludes 参考
- Python `/ai/landing/generate`

## 交付请求

**需要什么**：落地页 CRUD + AI 生成 API。

**验收标准**：
- [ ] `LandingPageController`（建议 `ruoyi-project`）
- [ ] `GET/POST/DELETE /api/v1/projects/{projectId}/landing-pages`；`GET .../{pageId}`
- [ ] `POST .../landing-pages/{pageId}/generate` → Feign → 更新 `content_json`/`seo_meta_json`/`form_config_json`
- [ ] slug 项目内唯一（DDL `uq_landing_page_project_slug`）；keyword 归属校验
- [ ] `tenant.excludes` 追加 `landing_page`（若需）
- [ ] smoke：`deploy/scripts/test_landing_api.py`

## 窗口激活 Prompt 摘要

```
角色：开发 inbound-core。必读 landing_page DDL 与 AI landing HANDOFF。
任务：landing CRUD + generate。本机 Docker ADR-09。
```

---

## Done（由 To 角色填写）

- **完成时间**：2026-07-01
- **结果摘要**：
  - `LandingPageController` — `GET/POST/DELETE /api/v1/projects/{projectId}/landing-pages` + `GET/{pageId}` + `POST/{pageId}/generate`
  - `LandingPageServiceImpl` + Feign `AiServiceClient.landingGenerate()`
  - `tenant.excludes` 追加 `landing_page`；`PgLandingPageStatusTypeHandler`
  - slug 项目内唯一（自动 slugify + 去重）；keyword 归属校验
  - `deploy/scripts/test_landing_api.py` ✅
- **遗留**：Admin 落地页列表/预览（→ UI HANDOFF）
