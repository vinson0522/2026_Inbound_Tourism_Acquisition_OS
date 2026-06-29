# HANDOFF | 技术总监 → 开发

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-2 M2 / FR-106 / FR-701 |

## 上下文

**当前状态**：EPIC-2 M1 本地 E2E ✅ runId=2 SUCCESS geo_score=85。诊断详情页导出按钮此前 disabled。

**相关文件**：
- `ruoyi-diagnostic/.../DiagnosticReportExportServiceImpl.java`
- `inbound-admin/.../diagnostics/detail.vue`
- `deploy/LOCAL_DOCKER.md` §Gotenberg

## 交付请求

**需要什么**：FR-106 诊断报告导出最小闭环 — DOCX + 可选 PDF（Gotenberg）。

**验收标准**：
- [x] SUCCESS run → GET `/api/v1/diagnostics/{runId}/report?format=docx` 可下载
- [x] 报告含 geo_score、probe_mode、sampled_at、region、platforms
- [x] Admin 详情页导出按钮可用
- [x] PDF 路径文档化（Gotenberg profile full）

## Done（由 To 角色填写）

- **完成时间**：2026-06-29
- **结果摘要**：
  - Java：`DiagnosticReportExportService` + Apache POI DOCX + `GotenbergClient`（HTML→PDF）
  - 导出时写入 `report` 表（type=DIAGNOSTIC，summary 含合规元数据）
  - Admin：`downloadDiagnosticReport` + 详情页导出按钮（SUCCESS/PARTIAL_FAILED）
  - Smoke：`test_diagnostic_report_export.py` runId=2 → `geo-report-run-2.docx` 2842B
- **遗留**：
  - 本地默认不启 Gotenberg；PDF 需 `--profile full` + `GOTENBERG_BASE_URL`
  - 报告未上传 MinIO（M2.1）；无 XDocReport 模板表驱动
