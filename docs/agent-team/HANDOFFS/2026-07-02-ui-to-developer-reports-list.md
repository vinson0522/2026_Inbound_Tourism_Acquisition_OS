# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-07-02 |
| **优先级** | High |
| **关联** | EPIC-8 M1 · FR-701/702 · [技术总监 → UI](2026-07-02-tech-director-to-ui-epic8-reports-list.md) · ADR-20260702-15 |

## 上下文

**当前状态**：FR-106 诊断详情已可导出 DOCX/PDF 并写入 `report`（DIAGNOSTIC）；缺统一报告中心 UI 与 FR-702 周报入口。

**相关文件**：
- `docs/design/wireframes/reports-list.md` — 线框 + DDL/API + 预览 drawer + 周报 dialog
- `docs/design/wireframes/leads-list.md` — 项目选择器、列表模式
- `docs/design/wireframes/diagnostic-detail.md` — 导出 loading / Gotenberg 错误文案
- `database/ddl/001_schema.sql` — `report`、`report_type`
- Java HANDOFF：`2026-07-02-tech-director-to-dev-java-epic8-reports.md`

**约束**：
- M1 无白标（FR-704）、月报（FR-703）、推送（FR-705）
- 周报 Java 聚合，不调 Python
- 下载复用 `src/api/tourgeo/diagnostic.ts` → `downloadDiagnosticReport`（blob + `FileSaver`）；新建 `report.ts` 的 `downloadReport`
- DIAGNOSTIC 行可跳转 `/diagnostics/runs/:runId`

## 交付请求

**需要什么**：Admin 报告中心列表 + 生成本周报告 dialog + 预览 drawer + DOCX/PDF 下载。

**验收标准**：
- [ ] 路由 `/projects/:projectId/reports`；菜单「报告中心 → 报告列表」
- [ ] 表格：type、period/关联、summary 一行、createdAt、操作（预览/下载）
- [ ] 筛选：type、period、createdAt 范围
- [ ] 「生成本周报告」→ dialog（默认近 7 日）→ `POST .../reports/weekly` → 刷新
- [ ] 预览 drawer：WEEKLY KPI + 章节；DIAGNOSTIC 链诊断详情
- [ ] 下载 `GET .../reports/{id}/export?format=docx|pdf`
- [ ] 空态「暂无报告，完成诊断或生成本周报告」
- [ ] 月报 / 模板配置 disabled + tooltip
- [ ] `pnpm build:prod` ✅

## 后端依赖（与 Java 开发对齐）

- [ ] `GET .../reports` — 分页 + 筛选；可选 `summaryPreview`
- [ ] `GET .../reports/{reportId}` — summary JSON
- [ ] `POST .../reports/weekly` — body `periodStart`/`periodEnd`
- [ ] `GET .../reports/{reportId}/export?format=docx|pdf`

## 质量 / 证据

**必须提供**：列表截图（含 DIAGNOSTIC + WEEKLY 行）；生成周报成功；DOCX/PDF 下载成功截图

---

## Done（由 To 角色填写）

- **完成时间**：2026-07-02
- **结果摘要**：Admin 已实现 `report.ts` + `/reports/index.vue` · build ✅；**待 Java API 联调**
- **遗留**：创建时间筛选为客户端过滤；Java 完成后跑浏览器走查
