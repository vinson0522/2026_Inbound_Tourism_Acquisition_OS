# HANDOFF | 技术总监 → UI 设计

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | UI 设计 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-2 M2.2 · **FR-108** · [Sprint 总览](2026-06-29-tech-director-epic2-m22-fr108-sprint.md) |

## 上下文

**当前状态**：GEO 诊断列表/详情已上线；本地已有多次 `diagnostic_run`（含 `geo_score`）。M2 FR-106 DOCX 导出 ✅。

**相关文件**：
- `PRD_商业化版_V2.0.md` §8.3 FR-108
- `docs/design/wireframes/diagnostic-detail.md` — 详情页结构
- `docs/design/tokens.md` — GEO 分数色阶、`--tg-score-*`
- `database/ddl/001_schema.sql` — `diagnostic_run.geo_score`, `finished_at`

**约束**：不重造 Admin 壳；图表用 Element Plus + ECharts（与现有 Admin 一致）。

## 交付请求

**需要什么**：输出 **GEO 诊断趋势对比** 线框，并 HANDOFF → 开发 Admin。

**验收标准**：
- [ ] `docs/design/wireframes/diagnostic-trends.md` 含：run 多选/时间轴、geo_score 折线、空态（&lt;2 次有效 run）
- [ ] 可选：分项指标迷你图（brand/top3/citation）— 标注 M2.2 可选
- [ ] 入口：工作台跳转 / 诊断模块侧栏 / 项目上下文（三选一，线框标明）
- [ ] 更新 `docs/design/README.md` §6.1 路由映射
- [ ] 复制 HANDOFF → `docs/agent-team/HANDOFFS/2026-06-29-ui-to-developer-diagnostic-trends.md`

## 质量 / 证据

**必须提供**：线框 Markdown + 与 `diagnostic_run` 字段对照表

**交给下一棒**：[开发 Admin FR-108](2026-06-29-tech-director-to-dev-admin-fr108-trends.md)（依赖 Java API 可 mock）

## 窗口激活 Prompt 摘要

```
角色：UI 设计。必读 PRD FR-108、diagnostic-detail 线框、tokens.md。
任务：输出 docs/design/wireframes/diagnostic-trends.md + HANDOFF 开发。
```

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
