# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-06-29 |
| **优先级** | Medium（**预研** · 待技术总监 EPIC-4 M1 Sprint HANDOFF 定稿） |
| **关联** | EPIC-4 M1 · FR-205 · FR-301~303（列表/创建） |

## 上下文

**当前状态**：EPIC-3 关键词页已上线；FR-205「转内容任务」在关键词列表为 disabled，待本页落地后启用。线框为 **M1 预研**，技术总监定稿 Sprint 后可能微调字段/路由。

**相关文件**：
- `docs/design/wireframes/content-task-list.md` — 线框 + DDL/API
- `docs/design/wireframes/keywords-list.md` — 项目选择器、跳转 query 约定
- `docs/design/tokens.md` — Tag、审核 warning 色
- `database/ddl/001_schema.sql` — `content_task`、`generated_content`
- `inbound-admin/src/views/tourgeo/keywords/index.vue` — 列表参考实现

**约束**：
- `needs_human_review` 来自 **最新** `generated_content`，非 `content_task` 列
- M1：列表 + 创建抽屉；脚本详情/编辑器 M2
- 路由含 `projectId`；与关键词页 FR-205 query 对齐
- AI 生成走 Java → Python，Admin 不直连 LLM

## 交付请求

**需要什么**：Admin 内容任务列表页 + 创建抽屉 + 关键词页「转内容任务」跳转打通。

**验收标准**：
- [ ] 路由 `/projects/:projectId/content/tasks`；侧栏「内容 Agent → 内容任务」
- [ ] 项目选择器（同 keywords 页）
- [ ] 表格列：关键词、platform、format、duration、tone、targetMarket、status、**needsHumanReview** Tag
- [ ] 筛选含 `needsHumanReview`
- [ ] 创建抽屉 POST task；`?action=create&keywordId=` 自动打开并预填
- [ ] keywords 列表「转内容任务」跳转本页（启用 FR-205）
- [ ] `content_task_status` Tag 映射完整
- [ ] 空态 / loading / 无效 keywordId

## 后端依赖（开发+技术总监协调）

- [ ] `GET/POST .../content/tasks` Java API
- [ ] List join `keyword_opportunity` + latest `generated_content`
- [ ] M2：`POST .../generate`、详情页

## 质量 / 证据

**必须提供**：列表截图 + 从关键词页跳入创建抽屉截图

---

## Done（由 To 角色填写）

- **完成时间**：2026-07-01
- **结果摘要**：Admin 内容任务列表 + 创建抽屉 + 脚本预览 drawer；FR-205 关键词跳转已启用
- **遗留**：M2 详情路由 `/content/tasks/:id`、TipTap 编辑器
