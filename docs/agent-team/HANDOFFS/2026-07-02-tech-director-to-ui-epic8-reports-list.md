# HANDOFF | 技术总监 → UI 设计

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | UI 设计 | 2026-07-02 | EPIC-8 M1 · FR-701/702 · [Sprint](2026-07-02-tech-director-epic8-m1-reports-sprint.md) |

## 上下文

**当前状态**：FR-106 诊断详情页已可导出 DOCX/PDF，并写入 `report` 表（type=DIAGNOSTIC）。缺统一「报告中心」入口与周报 UI。

**相关文件**：
- `docs/design/wireframes/leads-list.md` — 列表 + 项目选择器模式
- `docs/design/wireframes/diagnostic-detail.md` — 诊断导出按钮参考
- `database/ddl/001_schema.sql` — `report` 表、`report_type` 枚举

## 交付请求

**需要什么**：`docs/design/wireframes/reports-list.md` — 报告中心列表 + 生成周报 + 下载/预览

**验收标准**：
- [x] 路由 `/projects/:projectId/reports`（侧栏「报告中心」）
- [x] 表格列：类型、周期/关联、摘要（geo_score 或周报 KPI 一行）、创建时间、操作（下载 DOCX/PDF）
- [x] 工具栏：「生成本周报告」primary；类型筛选 DIAGNOSTIC / WEEKLY
- [x] 空态：「暂无报告，完成诊断或生成本周报告」
- [x] DIAGNOSTIC 行操作链到诊断详情 runId（summary.runId）
- [x] M1 不做：白标配置、月报、推送、模板编辑（disabled 或省略）
- [x] HANDOFF → `2026-07-02-ui-to-developer-reports-list.md`

## Prompt

```
角色：UI 设计。必读 PRD §8.9 FR-701/702、report DDL、leads-list 列表模式。
任务：reports-list.md 线框；含生成周报 dialog（可选 period 日期范围，默认近 7 日）。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-02
- **结果摘要**：`reports-list.md` 列表 + 周报 dialog + 预览 drawer + DOCX/PDF 下载；M1 disabled 月报/白标
- **遗留**：MinIO file_url 预览 M2；诊断导出后 toast 链入 P2
