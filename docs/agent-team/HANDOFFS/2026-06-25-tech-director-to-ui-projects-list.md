# HANDOFF | 技术总监 → UI 设计

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | UI 设计 |
| **日期** | 2026-06-25 |
| **优先级** | High |
| **关联** | EPIC-1 Story 2 · FR-001 · FR-002 · PRD §6.1 客户项目 |

## 上下文

**当前状态**：Sprint-1 设计交付已完成（token、工作台、GEO 诊断列表）。开发 P0 启动验通与 Admin 页实现并行中。**Story 2 前置设计**不依赖后端，可立即开工。

**相关文件**：
- `docs/design/tokens.md` — 沿用 `--tg-*` token
- `docs/design/wireframes/dashboard.md` — 无项目时引导「创建客户项目」
- `PRD_商业化版_V2.0.md` §8.2 FR-001、FR-002
- `database/ddl/001_schema.sql` → `customer_project`、`competitor`

**约束**：
- 字段名与 DDL 一致，不发明新列
- 创建流用 `el-drawer` 或 `el-dialog`（与 GEO 新建诊断一致）
- 列表页模式对齐 `diagnostics-list.md`

## 交付请求

**需要什么**：输出「客户项目列表」+「创建客户项目（FR-001）」线框到 `docs/design/wireframes/projects-list.md`，并 HANDOFF 开发。

**验收标准**：
- [ ] 列表列：项目名、品牌名、目标市场、状态、创建时间、操作（进入/编辑）
- [ ] 创建表单字段对齐 FR-001：客户名、品牌名、官网、行业、目标国家、服务语言、主推路线（路线可 Step 2 或折叠高级项）
- [ ] 空态、加载、校验错误态说明
- [ ] 路由建议：`/projects`、`/projects/create` 或 drawer 内创建
- [ ] 更新 `MEMORY.md` UI 章节；新建或更新 UI→开发 HANDOFF

## 质量 / 证据

**必须提供**：Markdown 线框 + 字段/API 对照表（`customer_project` 列）

**交给下一棒**：开发 Story 2 实现（等技术总监发 [dev Story 2 HANDOFF](2026-06-25-tech-director-to-dev-story2-fr001.md)）

---

## Done（由 To 角色填写）

- **完成时间**：2026-06-25
- **结果摘要**：
  - `docs/design/wireframes/projects-list.md` — 列表 + 创建抽屉 + DDL/API 对照表
  - HANDOFF → 开发：[2026-06-25-ui-to-developer-projects-list.md](2026-06-25-ui-to-developer-projects-list.md)
  - 更新 `docs/design/README.md`、`MEMORY.md` UI 章节
- **遗留**：品牌资料/竞品/知识库二级页（PRD §6.1 客户项目子页）待后续 Sprint
