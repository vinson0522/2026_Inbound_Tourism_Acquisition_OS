# EPIC-8 M2 月报 + 白标 Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-08 |
| **优先级** | High |
| **关联** | EPIC-8 · **FR-703/704** · ADR-20260708-21 |
| **前置** | EPIC-8 M1 ✅ · EPIC-7 M2 CRM ✅ · C17 `ecb0d46` |

## 商业决策（技术总监 · 完整版路线图 #2）

客户续费与服务商交付依赖 **可贴牌的月度复盘报告**。M1 仅有周报且无 Logo/封面；M2 补 **月报聚合 + 报告模板配置**，导出 DOCX/PDF 自动套用白标，形成可对外交付的「增长月报」。

## 目标（M2 MVP）

**手动生成月报 + 租户报告模板 + 导出带白标**

| 范围 | M2 做 | M2 不做 |
|------|-------|---------|
| FR-703 | `POST .../reports/monthly` · 自然月或近 30 日 · MoM 对比 · 含 CRM 线索统计 | XXL-Job 定时 · LLM 摘要 |
| FR-704 | `GET/PUT /api/v1/settings/report-template` · `template` 表 `type=REPORT` · logo/封面/公司名/页脚 | 自定义域名 FR-805 · GrapesJS |
| 导出 | 周报+月报+诊断导出渲染读 template · DOCX/PDF 封面区 | MinIO `file_url` 持久化 |
| Admin | 「生成月报」dialog · `/settings/report-template` 配置页 | FR-705 推送 · FR-706 自定义 |

## 任务拆分

| # | 角色 | HANDOFF | 依赖 | 验收 |
|---|------|---------|------|------|
| **1** | **UI 设计** | [→ 月报+白标线框](2026-07-08-tech-director-to-ui-epic8-monthly-whitelabel.md) | — | `reports-list.md` M2 + `report-template-settings.md` |
| **2** | **开发 Java** | [→ 月报+模板 API](2026-07-08-tech-director-to-dev-java-epic8-monthly-whitelabel.md) | — | smoke |
| **3** | **开发 Admin** | [→ 月报+模板 UI](2026-07-08-tech-director-to-dev-admin-epic8-monthly-whitelabel.md) | #1+#2 | build |

**无 Python / 无运维 M2**

## 月报 period 约定

- `period` = `YYYY-MM`（如 `2026-06`）
- 默认：上一完整自然月（`periodStart`/`periodEnd` 可选手动覆盖，跨度 ≤ 62 天）
- `report.type` = `MONTHLY`

## 月报聚合（M2 在周报基础上扩展）

| 章节 | 数据源 |
|------|--------|
| GEO | 月内 SUCCESS 诊断数 · 首尾 `geo_score` · **MoM Δ** |
| 关键词 | 月内新增 · Top stage · 已评分词均分（EPIC-3 M2） |
| 内容/落地页 | 月内创建/生成数 |
| 询盘 CRM | 月内 `lead` 总数 · 按 status · **WON 数** |
| 建议 | 静态模板 5 条（含 MoM 阈值） |

## 窗口派发（复制到各 Cursor 窗口）

| # | 窗口 | HANDOFF | 激活 Prompt（Custom Instructions 首行） |
|---|------|---------|----------------------------------------|
| **1** | **UI 设计** | [→ 月报+白标线框](2026-07-08-tech-director-to-ui-epic8-monthly-whitelabel.md) | `角色：UI 设计。必读 reports-list.md、FR-703/704、HANDOFF 2026-07-08-tech-director-to-ui-epic8-monthly-whitelabel.md。任务：reports-list.md M2 月报 dialog + report-template-settings.md 白标配置 + UI→开发 HANDOFF。` |
| **2** | **开发 Java** | [→ Java 月报+模板](2026-07-08-tech-director-to-dev-java-epic8-monthly-whitelabel.md) | `角色：开发 Java。必读 ReportServiceImpl、WeeklyHtmlReportRenderer、template 表、ADR-21、HANDOFF 2026-07-08-tech-director-to-dev-java-epic8-monthly-whitelabel.md。任务：POST monthly + report-template GET/PUT + 导出套白标 + test_reports_monthly.py。` |
| **3** | **开发 Admin** | [→ Admin 月报+模板](2026-07-08-tech-director-to-dev-admin-epic8-monthly-whitelabel.md) | `角色：开发 Admin。必读 reports/index.vue、billing 设置页结构、HANDOFF 2026-07-08-tech-director-to-dev-admin-epic8-monthly-whitelabel.md。任务：生成月报 dialog + /settings/report-template · build:prod。` |

**并行**：#1 UI 与 #2 Java 可并行；#3 等 Java smoke。

## 完成后

- smoke：`deploy/scripts/test_reports_monthly.py`
- commit **C18**：`feat(core,admin): EPIC-8 M2 monthly report and white-label template`
- 下一 Sprint：**EPIC-11 M2** 探针 adapter + 校准（路线图 #3）
