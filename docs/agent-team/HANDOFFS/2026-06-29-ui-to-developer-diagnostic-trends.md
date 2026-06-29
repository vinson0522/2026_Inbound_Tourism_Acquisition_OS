# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-06-29 |
| **优先级** | Medium |
| **关联** | EPIC-2 M2 · FR-108 · FR-105 |

## 上下文

**当前状态**：M1 已有诊断列表 + 详情页。FR-108 趋势对比线框已输出；详情页竞品 Tab 当前为 empty 占位，本页落地后改链。

**相关文件**：
- `docs/design/wireframes/diagnostic-trends.md` — 线框 + API 字段表
- `docs/design/wireframes/diagnostic-detail.md` — 分项 metrics 结构、详情跳转
- `docs/design/tokens.md` — GEO 分数色阶、折线 palette
- `inbound-admin/src/views/tourgeo/diagnostics/` — 同目录新增 `trends.vue`
- `database/ddl/001_schema.sql` — `diagnostic_run.geo_score`, `finished_at`

**约束**：
- 仅 `SUCCESS` / `PARTIAL_FAILED` 且 `geo_score` 非空参与对比
- 最少 2 条 run 才渲染图表；最多选 6 条
- 页脚合规 disclaimer 不可隐藏
- ECharts 需附表格数值（WCAG）；不只用颜色区分 series

## 交付请求

**需要什么**：实现 `/diagnostics/trends` 趋势对比页 + 菜单项 + 详情页 FR-108 链接。

**验收标准**：
- [ ] 路由 `/diagnostics/trends`；侧栏「趋势监控」
- [ ] 筛选：时间范围、市场、状态
- [ ] Run 选择器（checkbox 卡片，2–6 条）+ 默认最近 2 条
- [ ] GEO 分折线图（X=`finishedAt`, Y=`geoScore`）+ 点击跳转详情
- [ ] 横向时间轴节点可点进详情
- [ ] 六分项指标分组柱状图 + 下方数值 `el-table`
- [ ] 变化摘要卡片（最新/Δ/最高/最低/平均）
- [ ] API：`GET .../diagnostics/trends` 或等价聚合（含 `metrics`）
- [ ] `diagnostic-detail` 竞品 Tab 链到本页（带 runIds query）
- [ ] 空态：0 run / 1 run / 未选满 2 条

## 质量 / 证据

**必须提供**：≥2 次 mock run 折线 + 分项柱截图；从详情页链接跳转录屏或步骤

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
