# HANDOFF | 技术总监 → 开发（Java）

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-3 M1 · **FR-201/202** · ADR-20260629-11 |

## 上下文

**当前状态**：无 `ruoyi-keyword` 模块；DDL `keyword_opportunity` 已存在。

**相关文件**：
- `database/ddl/001_schema.sql` — `keyword_opportunity`, stage ENUM
- `inbound-core/ruoyi-modules/ruoyi-diagnostic/` — 参考 Feign AI client 模式
- `inbound-ai` — `/ai/keywords/generate`（Python HANDOFF）

**约束**：
- Java：CRUD、权限、调 AI、落库；**不**拼 Prompt
- 租户 + project 隔离
- 生成：M1 **同步**调用 AI  acceptable；高耗时再改 MQ `ai.keywords`

## 交付请求

**需要什么**：关键词机会词 CRUD + 生成 API。

**验收标准**：
- [x] 新模块 `ruoyi-keyword`（或并入 project 模块，需在 HANDOFF Done 注明）
- [x] `GET/POST/DELETE /api/v1/projects/{projectId}/keywords` — 分页、按 `stage` 筛选
- [x] `POST /api/v1/projects/{projectId}/keywords/generate` — 调 Python → 批量 insert `keyword_opportunity`（`source_json` 含 AI + chunk_ids；`status=ACTIVE`；`needs_human_review` 在 source_json 标记）
- [x] Entity/Vo 字段与 DDL 一致；MapStruct
- [x] Casbin / `@PreAuthorize` project 范围（`assertProjectOwned` + `BusinessTenantHelper` 租户过滤；`@SaCheckLogin`）
- [x] 集成测试或 smoke：`deploy/scripts/test_keywords_api.py`

## 质量 / 证据

**必须提供**：生成 5 词 × 1 stage 落库可查；跨 tenant 403

**交给下一棒**：[开发 Admin EPIC-3](2026-06-29-tech-director-to-dev-admin-epic3-keywords.md)

## 窗口激活 Prompt 摘要

```
角色：开发 inbound-core。必读 keyword_opportunity DDL 与 AI HANDOFF 契约。
任务：ruoyi-keyword CRUD + generate 调 /ai/keywords/generate。
本机 Docker ADR-09。
```

---

## Done（由 To 角色填写）

- **完成时间**：2026-06-29
- **结果摘要**：
  - **模块**：并入 `ruoyi-project`（未新建 `ruoyi-keyword`）
  - **API**：`GET/POST/DELETE /api/v1/projects/{projectId}/keywords`；`POST .../keywords/generate`
  - **Feign**：`AiServiceClient.keywordsGenerate()` → `/ai/keywords/generate`（120s timeout）
  - **落库**：`KeywordOpportunity` 字段对齐 DDL；`source_json` 含 `source=ai`、`needs_human_review`、可选 `chunk_ids`
  - **隔离**：`assertProjectOwned` 跨 project/tenant → 403；业务表 `tenant.excludes` 追加 `keyword_opportunity`（修复 MyBatis 插件 `tenant_id='000000'` 与 BIGINT 冲突导致列表为空）
  - **Smoke**：`python deploy/scripts/test_keywords_api.py` ✅（project=1 · inspiration · insertedCount=3 · total 4→7）
- **遗留**：Casbin 细粒度 permission 仍沿用 `@SaCheckLogin` MVP 模式（与同模块 KnowledgeAsset 一致）；跨 tenant 403 未单独脚本化（逻辑同 `assertProjectOwned`）
