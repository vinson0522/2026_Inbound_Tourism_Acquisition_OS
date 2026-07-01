# HANDOFF | 技术总监 → UI 设计

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | UI 设计 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-4 M1 · **FR-301/302** · [Sprint 总览](2026-06-29-tech-director-epic4-m1-content-sprint.md) |

## 上下文

**当前状态**：EPIC-3 关键词列表已上线（`/keywords`）；用户可从机会词进入内容创作闭环。

**相关文件**：
- `PRD_商业化版_V2.0.md` §10 FR-301、FR-302
- `docs/design/wireframes/keywords-list.md` — 列表页模式参考
- `database/ddl/001_schema.sql` — `content_task`, `generated_content`
- `docs/design/tokens.md`

## 交付请求

**需要什么**：内容 Agent **任务列表 + 脚本预览** 线框。

**验收标准**：
- [ ] `docs/design/wireframes/content-task-list.md` 含：项目选择器、表格（keyword、platform、duration、status、needs_human_review、created_at）
- [ ] 「从关键词创建」入口（弹窗：选 keyword + platform + duration 15/30/60 + tone 可选）
- [ ] 「生成脚本」主按钮 + loading；详情/预览 drawer（hook、script、分镜列表、CTA）
- [ ] 路由建议 `/projects/:projectId/content` 或 `/content-tasks`
- [ ] 关键词页「创建内容」链到本页并带 `keywordId` query
- [ ] 更新 `docs/design/README.md`
- [ ] HANDOFF → `docs/agent-team/HANDOFFS/2026-06-29-ui-to-developer-content-task-list.md`

## 窗口激活 Prompt 摘要

```
角色：UI 设计。必读 PRD FR-301/302 与 content_task DDL。
任务：输出 content-task-list.md 线框并 HANDOFF 开发。
```

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
