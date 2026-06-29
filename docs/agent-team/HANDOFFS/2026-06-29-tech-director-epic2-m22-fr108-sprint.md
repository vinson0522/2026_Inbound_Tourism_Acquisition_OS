# EPIC-2 M2.2 诊断趋势 Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-2 · **FR-108** · ADR-20260629-10 |
| **前置** | EPIC-2 M1/M2 ✅ · 本地 ≥2 次 SUCCESS `diagnostic_run` · Admin 详情/列表 ✅ |

## 目标（M2.2）

**同一项目多次 GEO 诊断 → 趋势 API → Admin 折线图对比 geo_score 变化**

| 范围 | M2.2 做 | M2.2 不做 |
|------|---------|-----------|
| 数据源 | `diagnostic_run.geo_score` + `finished_at`；可选聚合 `diagnostic_result.score_json` 分项 | 新建趋势表 |
| 可视化 | geo_score 折线 + 最近 N 次 run 选择 | 竞品趋势、多平台拆分 |
| 入口 | 项目上下文内「诊断趋势」页或详情 Tab | FR-109 定时任务 |
| 空态 | &lt;2 次 SUCCESS/PARTIAL_FAILED 有分数 → 引导再跑诊断 | — |

## 任务拆分（推荐顺序）

| # | 角色 | HANDOFF | 依赖 | 验收一句话 |
|---|------|---------|------|------------|
| **1** | **UI 设计** | [→ 趋势线框](2026-06-29-tech-director-to-ui-diagnostic-trends.md) | — | ✅ `diagnostic-trends.md` + [UI→开发](2026-06-29-ui-to-developer-diagnostic-trends.md) |
| **2** | **开发 Java** | [→ FR-108 API](2026-06-29-tech-director-to-dev-java-fr108-trends.md) | M1 diagnostic 模块 | GET trends 返回 ≥2 run 序列 |
| **3** | **开发 Admin** | [→ FR-108 图表](2026-06-29-tech-director-to-dev-admin-fr108-trends.md) | #1 线框 + #2 API | ECharts 折线 + 空态 |

**并行**：#2 与 #3 可并行启动（Admin 可 mock API；线框已就绪）。

## DDL / API 基线

- 表：`diagnostic_run`（`geo_score`, `finished_at`, `status`, `market`）
- 无新表；分项趋势 M2.2 可选读 `diagnostic_result.score_json` 聚合
- 路由建议：`GET /api/v1/projects/{projectId}/diagnostics/trends?limit=12&market=`

## 窗口激活 Prompt 摘要

| 角色 | 首行 Prompt |
|------|-------------|
| UI | `角色：UI 设计。必读 FR-108 与 diagnostic-detail 线框。任务：输出 diagnostic-trends.md` |
| Java | `角色：开发。必读 HANDOFF 2026-06-29-tech-director-to-dev-java-fr108-trends.md。任务：FR-108 trends API` |
| Admin | `角色：开发 Admin。必读 trends 线框 + Java API Done。任务：ECharts 趋势页` |

## 完成后

- To 角色填各 HANDOFF **Done** + 更新 `MEMORY.md`
- ~~技术总监验收~~ → **✅ 签核 2026-06-29**（M2.2 关闭；P2 遗留不阻塞）
- **可并行启动 EPIC-3 M1**（无硬依赖）
