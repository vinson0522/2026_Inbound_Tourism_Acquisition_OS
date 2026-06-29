# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-06-25 |
| **优先级** | High |
| **关联** | EPIC-1 Story 2 · FR-001 · FR-003 · [技术总监 Story 2](2026-06-25-tech-director-to-dev-story2-fr001.md) |

## 上下文

**当前状态**：项目列表 + FR-001 创建线框已输出，字段对齐 `customer_project` DDL。与 [技术总监 Story 2 HANDOFF](2026-06-25-tech-director-to-dev-story2-fr001.md) 配套实施。

**相关文件**：
- `docs/design/wireframes/projects-list.md` — 线框 + 字段/API 对照表
- `database/ddl/001_schema.sql` — `customer_project`、`travel_product`
- `docs/design/tokens.md` — Tag 状态色
- `inbound-admin/src/views/system/user/index.vue` — 列表模板
- `inbound-admin/src/store/modules/project`（或新建 `useProjectStore`）— 当前项目上下文

## 交付请求

**需要什么**：实现 `/projects` 列表 + 新建/编辑抽屉；对接 Story 2 项目 API；创建后可设当前项目并进工作台。

**验收标准**：
- [ ] 路由 `/projects`；菜单「客户项目」
- [ ] 表格列：name、brandName、targetMarkets Tag、languages Tag、status Tag、createdAt、操作（进入/编辑）
- [ ] 搜索：name、brandName、status、market、创建时间
- [ ] 抽屉创建：name、brandName、website、industry、targetMarkets、languages；可选折叠 travel_product 简版字段
- [ ] 「保存并进入工作台」更新 projectStore → `/dashboard`
- [ ] 空态 / loading / 校验错误态（见线框）
- [ ] 字段名与 API DTO 对齐线框对照表（`brandName` camelCase JSON）

## 质量 / 证据

**必须提供**：列表 + 创建成功截图；curl 或 Network 创建请求样例

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
