# EPIC-8 M1 报告中心 Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-02 |
| **优先级** | High |
| **关联** | EPIC-8 · **FR-701/702** · ADR-20260702-15 |
| **前置** | EPIC-2 FR-106 诊断导出 ✅ · EPIC-7 M1 线索 ✅ · C11 `76da501` |

## 目标（M1 MVP）

**报告中心列表 + 周报手动生成 + DOCX/PDF 导出**

| 范围 | M1 做 | M1 不做 |
|------|-------|---------|
| FR-701 | Admin 报告列表；展示 `report` 表（含 FR-106 已写入的 DIAGNOSTIC） | 白标封面 FR-704 |
| FR-702 | `POST .../reports/weekly` 聚合近 7 日数据；DOCX/PDF 导出 | XXL-Job 定时、月报 FR-703 |
| 诊断报告 | 列表中 DIAGNOSTIC 行可「再次导出」；summary 含 runId | MinIO `file_url` 持久化 |
| Admin | `/reports` 列表 + 类型筛选 + 生成周报 + 下载 | 模板配置后台 |

## 任务拆分

| # | 角色 | HANDOFF | 依赖 | 验收 |
|---|------|---------|------|------|
| **1** | **UI 设计** | [→ 报告中心线框](2026-07-02-tech-director-to-ui-epic8-reports-list.md) | — | ✅ |
| **2** | **开发 Java** | [→ report API](2026-07-02-tech-director-to-dev-java-epic8-reports.md) | — | ⏳ **P0** |
| **3** | **开发 Admin** | [→ 报告中心页](2026-07-02-tech-director-to-dev-admin-epic8-reports.md) | #2 联调 | ✅ build · 待 API |

**无 Python M1**（周报为 Java 聚合现有表；AI 摘要 → M2）。

## DDL / API

- 表：`report`（`type`: DIAGNOSTIC | WEEKLY | …；`period` 诊断存 runId，周报存 `2026-W26`）
- `GET /api/v1/projects/{projectId}/reports` — 分页；`type` 筛选
- `GET /api/v1/projects/{projectId}/reports/{reportId}` — 详情（summary JSON）
- `POST /api/v1/projects/{projectId}/reports/weekly` — body 可选 `{ periodStart, periodEnd }`；默认近 7 日
- `GET /api/v1/projects/{projectId}/reports/{reportId}/export?format=docx|pdf` — 下载

## 周报聚合字段（M1 最小集）

| 章节 | 数据源 |
|------|--------|
| GEO 可见率 | 近 7 日 `diagnostic_run` SUCCESS 次数 + 最新/上期 `geo_score` |
| 关键词 | `keyword_opportunity` 近 7 日新增 count + 按 stage 分布 |
| 内容 | `content_task` 近 7 日创建/生成 count |
| 落地页 | `landing_page` DRAFT/近 7 日 count |
| 询盘 | `lead` 近 7 日 NEW count |
| 建议 | 静态模板 3 条（无 LLM） |

## 窗口 Prompt 摘要

| 角色 | Prompt |
|------|--------|
| UI | `必读 FR-701/702 与 report DDL → reports-list.md` |
| Java | `report 列表 + weekly 聚合 + export；复用 DiagnosticReportExport/Gotenberg` |
| Admin | `报告中心列表 + 生成周报 + 下载` |

## 完成后

- 各 HANDOFF Done + `MEMORY.md` EPIC-8 M1 ✅
- **技术总监签核（2026-07-02）**：功能 ✅ · smoke docx 2885B · **C12 commit ⏳**
- **下一 Sprint**：EPIC-6 M2 Astro 预览 或 EPIC-9 计费 MVP（C12 后定案）
