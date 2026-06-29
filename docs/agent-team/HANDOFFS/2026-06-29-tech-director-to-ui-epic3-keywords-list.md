# HANDOFF | 技术总监 → UI 设计

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | UI 设计 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-3 M1 · **FR-201/202** · [Sprint 总览](2026-06-29-tech-director-epic3-m1-keywords-sprint.md) |

## 上下文

**当前状态**：项目详情 Tab 已上线；关键词模块 Admin 路由占位或未实现。

**相关文件**：
- `PRD_商业化版_V2.0.md` §9 FR-201、FR-202
- `docs/design/wireframes/project-detail.md` — 项目上下文
- `database/ddl/001_schema.sql` — `keyword_opportunity`
- `docs/design/tokens.md`

**约束**：八阶段中文标签与 PRD 一致（灵感→复购）；M1 无详情抽屉也可，列表为主。

## 交付请求

**需要什么**：关键词机会词 **列表页** 线框 + HANDOFF 开发。

**验收标准**：
- [ ] `docs/design/wireframes/keywords-list.md` 含：项目选择器、八阶段 Tab/筛选、表格列（keyword, keyword_en/cn, stage, market, score 占位, status, created_at）
- [ ] 「AI 生成机会词」主按钮 + 生成中 loading + 空态
- [ ] 路由建议 `/projects/:projectId/keywords` 或 `/keywords?projectId=`
- [ ] 更新 `docs/design/README.md`
- [ ] HANDOFF → `docs/agent-team/HANDOFFS/2026-06-29-ui-to-developer-keywords-list.md`

## 质量 / 证据

**必须提供**：线框 + 八阶段枚举对照表

**交给下一棒**：[开发 Admin EPIC-3](2026-06-29-tech-director-to-dev-admin-epic3-keywords.md)

## 窗口激活 Prompt 摘要

```
角色：UI 设计。必读 PRD FR-201/202 与 keyword_opportunity DDL。
任务：输出 keywords-list.md 线框并 HANDOFF 开发。
```

---

## Done（由 To 角色填写）

- **完成时间**：2026-06-29
- **结果摘要**：
  - `docs/design/wireframes/keywords-list.md` — 项目选择器、八阶段 Tab、表格列、AI 生成 + loading + 空态
  - 路由 `/projects/:projectId/keywords`；八阶段枚举对照表（与 `LIFECYCLE_STAGE_LABELS` 一致）
  - `docs/design/README.md` §6.1 已更新
  - HANDOFF → [ui-to-developer-keywords-list.md](2026-06-29-ui-to-developer-keywords-list.md)
- **遗留**：FR-203 机会分列占位；FR-205 转任务 disabled；项目详情快捷入口 P2
