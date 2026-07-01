# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-07-01 |
| **优先级** | High |
| **关联** | EPIC-7 M1 · FR-601 · [技术总监 → UI](2026-07-01-tech-director-to-ui-epic7-leads-list.md) · ADR-20260701-14 |

## 上下文

**当前状态**：EPIC-6 落地页 M1 已交付；公开表单提交需 Admin 线索列表承接转化闭环。

**相关文件**：
- `docs/design/wireframes/leads-list.md` — 线框 + DDL/API + 详情 drawer 结构
- `docs/design/wireframes/landing-page-list.md` — 列表模式、项目选择器、FR-505 表单字段对照
- `docs/design/tokens.md`
- `database/ddl/001_schema.sql` — `lead`、`lead_status`
- Java HANDOFF：`2026-07-01-tech-director-to-dev-java-epic7-leads.md`

**约束**：
- Admin **只读**；不可手工创建线索
- M1 无状态编辑、无跟进、无导出、无 AI 话术
- 列表邮箱/电话脱敏；详情全量（项目权限内）
- 不调 Python

## 交付请求

**需要什么**：Admin 询盘线索列表 + 详情 drawer；侧栏「线索与转化」入口。

**验收标准**：
- [ ] 路由 `/projects/:projectId/leads`；菜单「线索与转化 → 询盘线索」
- [ ] 表格：name、email（脱敏）、phone（脱敏）、source、landingPage、keyword、status、createdAt
- [ ] 筛选：name、email、phone、source、status、landingPage、keyword、createdAt 范围
- [ ] 详情 drawer：message、utm（5 字段）、device、travelDate、partySize、budget + 联系人 + 归因
- [ ] 空态「暂无询盘，发布落地页后可见」+ 链到落地页列表
- [ ] FR-605 变更状态 / FR-603 AI 跟进 / 导出 — disabled + tooltip
- [ ] 合规 alert：个人信息提示
- [ ] `?landingPageId=` 预置筛选（P2 可选）

## 后端依赖（与 Java 开发对齐）

- [ ] `GET /api/v1/projects/{projectId}/leads` — 分页 + join landing/keyword
- [ ] `GET /api/v1/projects/{projectId}/leads/{leadId}` — 详情含 `utm` object
- [ ] Public `POST /api/v1/public/leads` 写入（Admin 不调用）

## 质量 / 证据

**必须提供**：列表截图（含空态）；详情 drawer 截图（含 UTM + 行程字段）

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
