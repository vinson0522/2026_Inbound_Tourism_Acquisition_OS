# 线框：GEO 诊断 · 趋势对比（FR-108）

> **PRD**：§6.1 GEO 诊断 → 趋势监控 · FR-108 诊断趋势对比 · FR-105 可见率  
> **EPIC**：EPIC-2 M2  
> **路由**：`/diagnostics/trends`（项目上下文：`projectId` 来自顶栏当前项目或 query）  
> **数据表**：`diagnostic_run`（`geo_score`、`finished_at`）；分项指标来自 run 级聚合 `score_json`（PRD §10.3）

---

## 页面目标

在同一项目下**对比多次已完成诊断**的 GEO 综合分与六维分项指标，呈现可见率变化曲线与时间轴；支持选择 2–6 次 run 对比，强调**趋势与概率**而非固定排名承诺。

**入口**：
- 侧栏 GEO 诊断 → 趋势监控
- [diagnostic-detail.md](./diagnostic-detail.md) Tab「竞品对比」底部链接「查看历史趋势 →」
- [dashboard.md](./dashboard.md) KPI「GEO 可见率」旁「趋势」链接（P2）

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 面包屑：GEO 诊断 / 趋势对比          当前项目 ▼ Dragon Journey              │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─ 筛选栏 (el-card) ─────────────────────────────────────────────────────┐ │
│ │ 时间范围 [近3月▼]  市场 [全部▼]  状态 [已完成▼]     [应用] [重置]       │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 多次 run 选择器 (el-card) ────────────────────────────────────────────┐ │
│ │ 选择对比任务（2–6 次）：                                                │ │
│ │ [☑ Q2 US 首次  62.4  06-20] [☑ Q1 基准  58.1  03-15] [☐ …] [全选最近6]│ │
│ │ el-checkbox-group 卡片式；每条显示 name + geo_score Tag + finished_at  │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 主图区 (el-row :gutter="16") ─────────────────────────────────────────┐ │
│ │ ┌─ 左 16 col ─────────────────────┐ ┌─ 右 8 col ─────────────────────┐ │ │
│ │ │ GEO 综合分趋势（折线图）          │ │ 变化摘要 el-statistic 卡片      │ │ │
│ │ │ Y: 0–100  X: finished_at       │ │ 最新 vs 最早：+4.3 ↑            │ │ │
│ │ │ 线色 #1677A0 + 数据点 hover     │ │ 最高 / 最低 / 平均              │ │ │
│ │ └─────────────────────────────────┘ └─────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 时间轴 (el-timeline 横向或图表下 axis) ────────────────────────────────┐ │
│ │ ●──06-20 Q2 US 62.4──●──03-15 Q1 58.1──●── …  点击跳转诊断详情         │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 分项指标对比 (el-card) ────────────────────────────────────────────────┐ │
│ │ [分组柱状图 ECharts]  六指标 × N 次 run 并排                            │ │
│ │ 品牌出现率 | Top3 | 竞品压制 | 引用覆盖 | 长尾 | 资产完整度              │ │
│ │ 下方：el-table 数值表（便于 WCAG 读屏）                                  │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ⚠ 趋势基于历史采样结果，不同时间/模型/探针模式不可直接等同，仅供参考。      │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 筛选栏

| 字段 | 组件 | 参数 | 说明 |
|------|------|------|------|
| 时间范围 | `el-select` | `dateRange` | 近 1 月 / 3 月 / 6 月 / 1 年 / 自定义 daterange |
| 市场 | `el-select` | `market` | 来自项目 `target_markets_json`；默认全部 |
| 状态 | `el-select` | `status` | 默认 `SUCCESS,PARTIAL_FAILED`（有 `geo_score`） |
| 探针模式 | `el-select` | `probeMode` | 可选；对比时建议同模式 |

仅 **`geo_score IS NOT NULL`** 且 `finished_at` 非空的 run 进入候选池。

---

## 多次 run 选择器

**组件**：`el-checkbox-group` + 卡片列表（`el-card shadow="never"` 每项可点选）

| 展示项 | 字段 | 说明 |
|--------|------|------|
| 任务名 | `name` | 主文案，truncate |
| GEO 分 | `geo_score` | 色阶 `--tg-score-high/mid/low` |
| 完成时间 | `finished_at` | `MM-DD` + tooltip 完整 datetime |
| 市场 | `market` | 小 Tag |
| 状态 | `status` | 仅 PARTIAL_FAILED 时 warning Tag |

**规则**：
- 最少选 **2** 条才可渲染对比图；选 1 条时图表区 `el-empty`「请再选择至少 1 次诊断」
- 最多选 **6** 条（超出 toast 提示）
- 「全选最近 6 次」快捷按钮
- 默认：按 `finished_at` DESC 自动勾选最近 2 条成功 run

**线条配色**（ECharts series，WCAG 区分）：

| 序号 | 色 | Token |
|------|-----|-------|
| Run 1 | `#1677A0` | `--tg-color-primary` |
| Run 2 | `#059669` | `--tg-score-high` |
| Run 3 | `#D4920A` | `--tg-color-accent` |
| Run 4–6 | `#64748B` / `#7C3AED` / `#DC2626` | 固定 palette |

图例显示 `name` + `finished_at` 短格式。

---

## 图表 1：GEO 综合分折线

| 项 | 规范 |
|----|------|
| 库 | ECharts（plus-ui 已集成） |
| X 轴 | `finished_at` 时间升序 |
| Y 轴 | `geo_score` 0–100，间隔 20 |
| 系列 | 单系列多点（**同一项目多次 run 的单线趋势**）；每点 `runId` 存 meta |
| 数据点 | `symbolSize: 8`；hover 显示 tooltip：name、score、market、probe_mode |
| 参考线 | 可选 `--tg-score-mid` 40 虚线「及格参考」（非业务硬阈值，浅灰标注） |
| 点击点 | `router.push('/diagnostics/runs/' + runId)` |

**双模式说明**（实现二选一，推荐 A）：

| 模式 | 行为 |
|------|------|
| **A 趋势线（默认）** | 选中 run 按时间连成 **1 条折线**（纵轴 geo_score） |
| B 并列对比 | 选中 run 作为分类轴上的并列柱（P2 切换 `el-radio`） |

线框默认 **模式 A**；选择器变更时重绘。

---

## 时间轴

**组件**：图表下方 **横向时间轴**（ECharts `timeline` 或自定义 `flex` + 圆点）

| 节点 | 内容 | 交互 |
|------|------|------|
| 圆点 | `finished_at` 日期 | 选中高亮 `--tg-color-primary` |
| 标签 | `name` 截断 + `geo_score` | |
| 连线 | 按时间顺序 | |
| 点击 | — | 打开 [diagnostic-detail](./diagnostic-detail.md) |

不足 2 个节点：隐藏时间轴，仅显示 empty。

---

## 图表 2：分项指标对比

数据来源：每次 run 的聚合 `score_json.metrics`（与 [diagnostic-detail.md §概览](./diagnostic-detail.md) 一致）。

| 指标键 | 中文 | Y 轴 |
|--------|------|------|
| `brand_mention_rate` | 品牌出现率 | 0–100% |
| `top3_rate` | Top3 推荐率 | 0–100% |
| `competitor_suppression` | 竞品压制指数 | 0–100%（越高越差，柱色 `--tg-color-danger` 浅底） |
| `citation_coverage` | 引用链接覆盖 | 0–100% |
| `longtail_coverage` | 长尾覆盖率 | 0–100% |
| `asset_completeness` | 内容资产完整度 | 0–100% |

**图表**：分组柱状图 — X 轴 = 六指标，每组内 N 根柱（N = 选中 run 数），图例 = run 名称。

**无障碍表格**（图表下方，`el-table border size="small"`）：

| 诊断任务 | 完成时间 | GEO 分 | 品牌出现率 | Top3 | … |
|----------|----------|--------|------------|------|---|
| 行 = 每个选中 run | `finished_at` | `geo_score` | metrics… | | |

数值百分比保留 1 位小数；变化列（最早→最新 Δ）可选 P2。

---

## 变化摘要卡片（右侧）

基于选中 run 按 `finished_at` 排序：

| 统计 | 计算 |
|------|------|
| 最新 GEO 分 | 最后一条 `geo_score` |
| 较上次变化 | 最新 − 次新，带 ↑↓ 色（success/danger） |
| 期间最高/最低 | max/min |
| 平均值 | avg |

组件：`el-statistic` + `el-card` 堆叠。

---

## 字段 ↔ DDL / API 对照

### 请求

```
GET /api/v1/projects/{projectId}/diagnostics/trends
  ?market=US
  &finishedAfter=2026-01-01
  &finishedBefore=2026-06-30
  &status=SUCCESS,PARTIAL_FAILED
  &runIds=12,10,8   // 可选；不传则返回候选列表
```

### 响应 `data.runs[]`（每条 = `diagnostic_run` + 聚合 metrics）

| UI | DDL / 来源 | API 字段 | 说明 |
|----|------------|----------|------|
| Run ID | `id` | `runId` | |
| 任务名 | `name` | `name` | |
| GEO 综合分 | `geo_score` | `geoScore` | NUMERIC(6,2) |
| 完成时间 | `finished_at` | `finishedAt` | 时间轴 / X 轴 |
| 开始时间 | `started_at` | `startedAt` | tooltip |
| 市场 | `market` | `market` | |
| 语言 | `locale` | `locale` | |
| 地区 | `region` | `region` | tooltip 合规 |
| 状态 | `status` | `status` | |
| 探针模式 | `probe_modes_json` | `probeModes` | 对比提示 |
| 平台 | `models_json` | `models` | tooltip |
| 采样 | `sample_count` | `sampleCount` | |
| 分项指标 | 聚合 `score_json.metrics` | `metrics` | 见 §10.3 |
| 生命周期 | `score_json.by_stage` | `byStage` | P2 第三张图 |
| 项目 | `project_id` | `projectId` | 过滤用 |

**不入趋势图**：`PENDING` / `RUNNING` / `FAILED` / 无 `geo_score` 的 run（列表可见但 disabled checkbox + tooltip）。

---

## 空 / 加载 / 边界

| 状态 | UI |
|------|-----|
| 加载 | 图表区 `v-loading` |
| 项目无 run | `el-empty`「暂无已完成诊断」+ CTA → 诊断任务列表 |
| 仅 1 次成功 run | 选择器仅 1 条可选 + empty「完成第二次诊断后可查看趋势（FR-108）」 |
| 选中 &lt; 2 | 图表 placeholder + 说明 |
| 跨 probe_mode 混选 | `el-alert type="warning"`「所选任务探针模式不一致，对比仅供参考」 |
| 无项目 | 同 dashboard，引导选择项目 |

---

## 合规文案（页脚固定）

```
趋势图展示历史采样 GEO 分数变化，受模型版本、探针模式、采样时间影响，不表示持续排名承诺。
报告级明细请进入单次诊断详情页，标注 probe_mode 与 sampled_at。
```

---

## 响应式

| 断点 | 行为 |
|------|------|
| ≥1200px | 折线图 16 + 摘要 8；分项柱图全宽 |
| 768–1199px | 摘要卡片移到折线图下方 |
| &lt;768px | run 选择器改下拉 multiselect；图表高度 240px；表格横向 scroll |

---

## 菜单 / 路由

```text
父菜单：GEO 诊断
  └─ 趋势监控 (path: /diagnostics/trends, component: tourgeo/diagnostics/trends)
      meta: { title: '趋势对比', icon: 'trend-charts' }
```

隐藏 query：`?projectId=` 可选（默认 store 当前项目）。

---

## 与 diagnostic-detail 联动

| 位置 | 改动 |
|------|------|
| 详情 Tab「竞品对比」 | 将 FR-108 empty 替换为链接 `router-link` → `/diagnostics/trends?runIds={current},{prev}` |
| 详情页头 | 可选 badge「查看趋势」 |

---

## 实现参考

- ECharts：plus-ui 现有 dashboard 图表示例
- 色彩：[tokens.md](../tokens.md) §1.1、§1.4
- 列表候选数据：复用 `GET /api/v1/projects/{id}/diagnostics` 分页或专用 trends 端点
- 详情字段：[diagnostic-detail.md](./diagnostic-detail.md)

---

## 范围外（M2+）

| 项 | 说明 |
|----|------|
| `by_stage` 折线 | P2 第三 Tab |
| 竞品 mention 趋势 | FR-107 独立 Story |
| 导出趋势 PNG/PDF | 报告中心 EPIC-8 |
