# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-07-01 |
| **优先级** | High |
| **关联** | EPIC-6 M1 · FR-501~505 · [技术总监 → UI](2026-07-01-tech-director-to-ui-epic6-landing-list.md) |

## 上下文

**当前状态**：EPIC-3 关键词 + EPIC-4 内容任务已闭环。落地页 M1 线框已交付，为转化承接下一环。

**相关文件**：
- `docs/design/wireframes/landing-page-list.md` — 线框 + DDL/API + §20.3 预览结构
- `docs/design/wireframes/keywords-list.md` — 项目选择器、FR-205 query
- `docs/design/wireframes/content-task-list.md` — 列表模式参考
- `docs/design/tokens.md`
- `database/ddl/001_schema.sql` — `landing_page`

**约束**：
- Admin 不直连 LLM；生成走 Java → `/ai/landing`
- M1：列表 + 创建 dialog + 预览 drawer；无可视化编辑器
- Slug 项目内唯一；实时校验 API
- 预览优先可读摘要，JSON 折叠为次级

## 交付请求

**需要什么**：Admin 落地页列表 + 创建弹窗 + AI 生成 loading + 预览 drawer；关键词页「转落地页」入口（query 约定）。

**验收标准**：
- [ ] 路由 `/projects/:projectId/landing-pages`；菜单「落地页 Agent → 页面草稿」
- [ ] 表格：title、slug、templateType、keyword、market、status、updatedAt
- [ ] 创建 dialog：keyword、templateType、slug 校验、market
- [ ] 「创建并 AI 生成」+ 行内 AI 生成 → EDITING loading → READY
- [ ] 预览 drawer：摘要 + SEO descriptions + content_json collapse + form/WhatsApp
- [ ] `?action=create&keywordId=` 自动打开 dialog
- [ ] FR-506 导出 / M2 编辑 disabled + tooltip
- [ ] 空态 / loading / slug 冲突

## 后端依赖（与 Java 开发对齐）

- [ ] CRUD + check-slug + generate 端点
- [ ] List join keyword；generate 写入 `content_json` / `seo_meta_json` / `form_config_json`

## 质量 / 证据

**必须提供**：列表 + 创建 + 生成成功预览截图；slug 冲突校验截图

---

## Done（由 To 角色填写）

- **完成时间**：2026-07-01
- **结果摘要**：Admin `tourgeo/landing/index.vue` 按线框实现；API `landing.ts`；关键词「转落地页」
- **遗留**：check-slug 服务端 API；截图验收待补
