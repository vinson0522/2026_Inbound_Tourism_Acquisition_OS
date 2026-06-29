# HANDOFF | 技术总监 → 开发（Admin）

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-2 M2.2 · **FR-108** |

## 上下文

**当前状态**：`diagnostic.ts` 已有 list/get/export；Java trends API 由 [Java HANDOFF](2026-06-29-tech-director-to-dev-java-fr108-trends.md) 提供。

**相关文件**：
- `docs/design/wireframes/diagnostic-trends.md` — UI 线框 ✅
- `docs/agent-team/HANDOFFS/2026-06-29-ui-to-developer-diagnostic-trends.md` — UI→开发详细验收
- `inbound-admin/src/api/tourgeo/diagnostic.ts`
- `inbound-admin/src/router/index.ts`
- `inbound-admin/src/views/tourgeo/diagnostics/` — 列表/详情

**约束**：API 未就绪时可 mock 2 点数据开发图表，联调前切真实 API。

## 交付请求

**需要什么**：Admin 诊断趋势页（FR-108 可视化）。

**验收标准**：
- [ ] 路由 `/diagnostics/trends` 或 `/projects/:id/diagnostics/trends`（与线框一致）
- [ ] `getDiagnosticTrends(projectId)` 封装 trends API
- [ ] ECharts 折线图：X=`finishedAt`，Y=`geoScore`；Tooltip 含 run 名称
- [ ] 空态：&lt;2 个有效点时 `el-empty` + 跳转「发起诊断」
- [ ] 从诊断列表/工作台增加入口链接
- [ ] 分数色使用 `--tg-score-high/mid/low`

## 质量 / 证据

**必须提供**：≥2 次 SUCCESS run 截图；空态截图

**交给下一棒**：技术总监 M2.2 验收

## 窗口激活 Prompt 摘要

```
角色：开发 Admin。必读 diagnostic-trends 线框与 diagnostic.ts。
任务：FR-108 趋势页 + ECharts；接 Java trends API。
依赖 Java HANDOFF 完成或 mock 2 点联调。
```

---

## Done（由 To 角色填写）

- **完成时间**：2026-06-29
- **结果摘要**：`/diagnostics/trends` + 侧栏「趋势监控」；`getDiagnosticTrends()`；ECharts 折线（GEO 分）+ 分组柱图（六分项）+ 数值表；run 多选 2–6、变化摘要；空态 0/1 run；详情竞品 Tab「查看历史趋势 →」；`--tg-color-primary` / score tokens
- **遗留**：时间范围/探针模式筛选（P2）；dashboard KPI「趋势」链（P2）；横向时间轴组件（P2）
