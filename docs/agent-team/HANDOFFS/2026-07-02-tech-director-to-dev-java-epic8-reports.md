# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-02 | EPIC-8 M1 · FR-701/702 · ADR-20260702-15 |

## 上下文

**当前状态**：
- `Report` 实体 + `ReportMapper` 已在 `ruoyi-diagnostic`
- `DiagnosticReportExportServiceImpl` 导出时 `insertReportRecord`（type=DIAGNOSTIC）
- 无 Report 列表/周报/统一下载 API

**相关文件**：
- `ruoyi-diagnostic/.../DiagnosticReportExportServiceImpl.java` — 复用 DOCX/PDF 渲染与 Gotenberg
- `ruoyi-diagnostic/.../DiagnosticMetricsAggregator` — FR-108 趋势聚合可参考
- `ruoyi-project` — keyword / content_task / landing_page / lead Mapper

## 交付请求

**需要什么**：报告中心 CRUD + 周报生成 + 导出

**验收标准**：
- [x] `ReportController` — `GET/POST .../projects/{projectId}/reports` 及子资源
- [x] `GET .../reports/{reportId}` — 详情 Vo（summary 解析为对象）
- [x] `POST .../reports/weekly` — 聚合近 7 日（或 body 指定区间）；写入 `report` type=WEEKLY；`period` 如 `2026-W26`
- [x] `GET .../reports/{reportId}/export?format=docx|pdf` — WEEKLY 新渲染器（HTML→Gotenberg PDF / 简单 DOCX）；DIAGNOSTIC 复用 runId 导出
- [x] 租户隔离：`BusinessTenantHelper` + project 归属校验
- [x] `tenant.excludes` += `report`（与其它业务表一致）
- [x] smoke：`deploy/scripts/test_reports_api.py` — list → weekly create → export docx（≥1KB）

## 周报 summary_json 建议结构

```json
{
  "periodStart": "2026-06-25",
  "periodEnd": "2026-07-01",
  "geo": { "runs": 2, "latestScore": 85, "delta": 5 },
  "keywords": { "newCount": 12, "byStage": { "inspiration": 3 } },
  "content": { "tasksCreated": 2, "generated": 1 },
  "landing": { "draftCount": 3 },
  "leads": { "newCount": 4 },
  "recommendations": ["...", "...", "..."]
}
```

## Prompt

```
角色：开发 Java。必读 EPIC-8 HANDOFF、report DDL、DiagnosticReportExportServiceImpl、inbound-admin/src/api/tourgeo/types.ts（ReportVo 字段对齐）。
任务：ReportController + IReportService — list/detail/weekly/export。模块 ruoyi-diagnostic（已依赖 ruoyi-project，可注入 Keyword/Content/Landing/Lead Mapper）。
DIAGNOSTIC export：summary.runId → 复用 diagnosticReportExportService.exportReport。
WEEKLY export：新建 WeeklyHtmlReportRenderer + DOCX（或 HTML→Gotenberg PDF）。
验收：deploy/scripts/test_reports_api.py（list → weekly → export docx ≥1KB）。
```

## Admin 已对齐 API 契约（勿改字段名）

- `GET .../reports` → `TableDataInfo<ReportVo>`；Vo 含 `id,type,period,summaryPreview,createdAt`；DIAGNOSTIC 的 summaryPreview 含 geoScore
- `GET .../reports/{id}` → `R<ReportDetailVo>`；`summary` 为 JSON 对象
- `POST .../reports/weekly` body `{ periodStart?, periodEnd? }` → `R<{ reportId: number }>` 或 `R<Long>`
- `GET .../reports/{id}/export?format=docx|pdf` → blob 流（同 DiagnosticController exportReport 响应头）

## Done（由 To 角色填写）

- **完成时间**：2026-07-02
- **结果摘要**：`ReportController` + `ReportServiceImpl` — list/detail/weekly/export；周报聚合 diagnostic/keyword/content/landing/lead + 3 条静态 recommendations；`WeeklyHtmlReportRenderer` + `WeeklyDocxReportRenderer`；DIAGNOSTIC export 复用 `DiagnosticReportExportServiceImpl`；smoke `test_reports_api.py` E2E passed（docx 2884B）
- **遗留**：PDF 需 Gotenberg；C12 commit 待用户指令
