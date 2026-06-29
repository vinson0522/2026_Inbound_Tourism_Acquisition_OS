# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-06-26 |
| **优先级** | High |
| **关联** | EPIC-2 · FR-104/105/106/701 · [诊断列表线框](../design/wireframes/diagnostics-list.md) |

## 上下文

**当前状态**：EPIC-2 诊断详情线框已输出。列表页 `/diagnostics/runs` 已有 mock 实现；详情页为「查看结果/查看进度」的落地页。

**相关文件**：
- `docs/design/wireframes/diagnostic-detail.md` — 线框 + DDL/API 对照
- `docs/design/tokens.md` — 状态 Tag、GEO 分数色阶
- `inbound-admin/src/views/tourgeo/diagnostics/index.vue` — 列表入口
- `database/ddl/001_schema.sql` — `diagnostic_run`、`diagnostic_result`、`probe_task`

**约束**：
- RUNNING / SUCCESS 分态；合规元数据不可隐藏
- 人工修正仅改解析字段，不直连 LLM（Java 落库，Python 重算可选）
- 导出 FR-106 可先做按钮 disabled，后端就绪后接

## 交付请求

**需要什么**：实现 `/diagnostics/runs/:runId` 详情页（Tabs：概览/问题明细/竞品对比/探针进度），列表跳转打通。

**验收标准**：
- [ ] 路由 + 隐藏菜单项；列表「查看」可进入
- [ ] 任务头展示 `diagnostic_run` 字段 + 合规 alert
- [ ] RUNNING：默认探针进度 Tab + 轮询刷新
- [ ] SUCCESS：KPI 六宫格 + 概览 Tab（metrics / by_stage）
- [ ] 问题明细表 + 行展开 + 修正抽屉（mock 或 API）
- [ ] PARTIAL_FAILED / FAILED 分态
- [ ] 分数使用 `--tg-score-*` 色阶

## 质量 / 证据

**必须提供**：SUCCESS + RUNNING 两态截图；列表→详情导航录屏或步骤说明

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
