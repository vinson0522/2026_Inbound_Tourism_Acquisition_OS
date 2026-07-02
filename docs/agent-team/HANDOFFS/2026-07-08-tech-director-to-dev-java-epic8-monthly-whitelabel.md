# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-08 | EPIC-8 M2 · FR-703/704 · ADR-20260708-21 |

## 上下文

M1 已有 `ReportServiceImpl.createWeeklyReport` + `WeeklyHtmlReportRenderer`/`WeeklyDocxReportRenderer`。M2 扩展月报与白标模板。DDL `template.type=REPORT` · `report.template_id` 已存在。

**相关文件**：
- `ReportServiceImpl.java` · `ReportController.java`
- `WeeklyHtmlReportRenderer.java` · `WeeklyDocxReportRenderer.java`
- `002_seed_demo.sql` — 可 seed 默认 REPORT template
- EPIC-7 M2 `lead` status 统计

## 交付请求

**验收标准**：
- [x] `POST /api/v1/projects/{projectId}/reports/monthly` — body 可选 `{ year, month }` 或 `{ periodStart, periodEnd }` · 写 `type=MONTHLY` · `period=YYYY-MM`
- [x] 聚合：GEO MoM · 关键词（含 score 均值）· 内容/落地页 · **lead 按 status + WON**
- [x] 扣额：复用 `QuotaType.REPORTS_PER_MONTH`
- [x] `GET /api/v1/settings/report-template` · `PUT` upsert — 租户级 `template` REPORT · `config_json`：`logoUrl, coverTitle, companyName, primaryColor, footerText, sections[]`
- [x] 导出：周报/月报/DIAGNOSTIC 渲染时注入 template（封面 Logo 区 + footer）；`report.template_id` 写入
- [x] `MonthlyHtmlReportRenderer` + `MonthlyDocxReportRenderer`（或复用 Weekly 泛化）
- [x] smoke：`deploy/scripts/test_reports_monthly.py` — 设 template → POST monthly → GET detail → export docx ≥1KB

## config_json 示例

```json
{
  "logoUrl": "https://cdn.example.com/logo.png",
  "coverTitle": "Inbound Growth Report",
  "companyName": "Dragon Journey Travel",
  "primaryColor": "#059669",
  "footerText": "Confidential · TourGEO Agent",
  "sections": ["geo", "keywords", "content", "landing", "leads", "recommendations"]
}
```

## Prompt

```
角色：开发 Java。必读 ReportServiceImpl、ADR-21、template 表。
任务：POST monthly + report-template API + 导出白标 + test_reports_monthly.py。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-08
- **结果摘要**：
  - `POST /api/v1/projects/{projectId}/reports/monthly` · `{ year, month }` / `{ periodStart, periodEnd }` ≤62d · `type=MONTHLY` · `period=YYYY-MM`
  - 聚合：`geo.momDelta/prevScore` · `keywords.avgScore` · `leads.byStatus/wonCount` · `recommendations[5]` · `templateSnapshot`
  - `GET/PUT /api/v1/settings/report-template` · 租户 `template.type=REPORT` upsert
  - 导出白标：周报/月报/诊断 DOCX/PDF 套 cover+footer · `report.template_id` 写入
  - smoke：`deploy/scripts/test_reports_monthly.py` ✅（docx 3122B）
- **遗留**：Logo 仅 URL 文本注入（M2 无 MinIO 上传）；PDF 需 Gotenberg
