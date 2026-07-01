# HANDOFF | 技术总监 → UI 设计

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | UI 设计 |
| **日期** | 2026-07-01 |
| **优先级** | High |
| **关联** | EPIC-6 M1 · **FR-501~505** · [Sprint 总览](2026-07-01-tech-director-epic6-m1-landing-sprint.md) |

## 上下文

**当前状态**：关键词 + 内容任务已闭环；落地页为获客转化下一环（公开发布 → EPIC-7）。

**相关文件**：
- `PRD_商业化版_V2.0.md` §11 FR-501~505 · §20.3 `content_json` 模块
- `docs/design/wireframes/content-task-list.md` — 列表模式参考
- `database/ddl/001_schema.sql` — `landing_page`
- `docs/design/tokens.md`

## 交付请求

**需要什么**：落地页 **列表 + 创建 + 预览** 线框。

**验收标准**：
- [ ] `docs/design/wireframes/landing-page-list.md`
- [ ] 表格：title、slug、template_type、keyword、status、updated_at
- [ ] 创建弹窗：keyword 选择、template_type、slug 校验、market
- [ ] 「AI 生成页面」+ loading；预览 drawer（模块折叠 JSON / 可读摘要 + SEO meta）
- [ ] 路由 `/projects/:projectId/landing-pages`；关键词/内容页入口链
- [ ] 更新 `docs/design/README.md`
- [ ] HANDOFF → `docs/agent-team/HANDOFFS/2026-07-01-ui-to-developer-landing-page-list.md`

## 窗口激活 Prompt 摘要

```
角色：UI 设计。必读 PRD FR-501~505 与 landing_page DDL、PRD §20.3 模块结构。
任务：输出 landing-page-list.md 线框并 HANDOFF 开发。
```

---

## Done（由 To 角色填写）

- **完成时间**：2026-07-01
- **结果摘要**：
  - `docs/design/wireframes/landing-page-list.md` — 列表 + 创建 dialog + AI 生成 + 预览 drawer（§20.3 模块）
  - HANDOFF → 开发：[2026-07-01-ui-to-developer-landing-page-list.md](2026-07-01-ui-to-developer-landing-page-list.md)
  - 更新 `docs/design/README.md`
- **遗留**：M2 可视化编辑线框；FR-507 发布态 UI
