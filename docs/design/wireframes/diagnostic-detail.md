# 线框：GEO 诊断 · 诊断详情（诊断结果）

> **PRD**：§6.1 GEO 诊断 → 诊断结果 · FR-104 回答解析 · FR-105 可见率 · FR-106 报告导出 · FR-701 诊断报告  
> **EPIC**：EPIC-2  
> **路由**：`/diagnostics/runs/:runId`（推荐）；或 `/projects/:projectId/diagnostics/:runId`  
> **数据表**：`diagnostic_run`、`diagnostic_result`、`probe_task`、`question_bank`

---

## 页面目标

展示单次 GEO 诊断任务的**执行状态、综合评分、分项指标、问题级明细与竞品对比**；支持人工修正（FR-104）、导出报告（FR-106），并强制展示合规元数据（probe_mode、sampled_at、region）。

从 [diagnostics-list.md](./diagnostics-list.md) 行操作「查看结果/查看进度」进入。

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ ← 返回列表   面包屑：GEO 诊断 / 诊断任务 / Q2 US 首次诊断                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─ 任务头 (el-card) ─────────────────────────────────────────────────────┐ │
│ │ Q2 US 首次诊断          [执行中 Tag]     GEO 62.4  (--tg-score-mid)    │ │
│ │ 市场 US · en-US · region: us-east  |  探针: grounded-api              │ │
│ │ 平台 Perplexity Gemini  |  采样×3  |  2026-06-20 14:32 – 15:08       │ │
│ │ ⚠ 采样结果基于联网检索，不承诺 AI 推荐排名。probe_mode=grounded-api     │ │
│ │                              [导出 DOCX] [导出 PDF] [取消任务]          │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ RUNNING 态：进度条 (替代下方评分区) ──────────────────────────────────┐ │
│ │ el-progress 68%  |  已完成 136/200 探针子任务  |  预计剩余 12 min      │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ SUCCESS 态：KPI 六宫格 (el-row :gutter="16") ─────────────────────────┐ │
│ │ 品牌出现率 18% | Top3 6% | 竞品压制 0.62 | 引用覆盖 10% | 长尾 22% | … │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ el-tabs ──────────────────────────────────────────────────────────────┐ │
│ │ [概览] [问题明细] [竞品对比] [探针进度]                                  │ │
│ │ ┌─ Tab: 概览 ─────────────────────────────────────────────────────────┐ │ │
│ │ │ 左：六维指标 el-progress 条形  |  右：生命周期 by_stage 柱状图       │ │ │
│ │ │ 底部：优化建议 el-alert 列表（来自 score_json.recommendations）    │ │ │
│ │ └────────────────────────────────────────────────────────────────────┘ │ │
│ │ ┌─ Tab: 问题明细 (默认高流量 Tab 可切换) ─────────────────────────────┐ │ │
│ │ │ 筛选：平台/阶段/是否提及品牌/是否Top3  |  el-table + 行展开         │ │ │
│ │ └────────────────────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 任务头：`diagnostic_run`

| UI 元素 | DDL / 字段 | 组件 | 说明 |
|---------|------------|------|------|
| 任务名称 | `name` | `h1` `--tg-font-size-lg` | 主标题 |
| 状态 | `status` | `el-tag` | 映射见 [tokens.md](../tokens.md) |
| GEO 综合分 | `geo_score` | 大号数字 `--tg-font-size-display` + `--tg-score-*` | RUNNING/PENDING 显示 `—` |
| 市场/语言/地区 | `market`, `locale`, `region` | `el-descriptions` inline | |
| 探针模式 | `probe_modes_json` | `el-tag` 组 | 强调 grounded-api |
| AI 平台 | `models_json` | Tag | |
| 采样次数 | `sample_count` | 文本 | |
| 时间范围 | `started_at`, `finished_at` | 文本 | 未完成仅 started_at |
| 合规条 | — | `el-alert type="info"` | 固定文案 + 动态 probe_mode |
| 导出 | — | `el-button` plain | FR-106；RUNNING 时 disabled |
| 取消 | — | `el-button` danger plain | 仅 PENDING/RUNNING + popconfirm |

**返回**：`el-page-header @back` → `/diagnostics/runs`

---

## 状态分态

| `status` | 页面主体 |
|----------|----------|
| `PENDING` | 头信息 + `el-empty`「等待调度」+ 取消按钮 |
| `RUNNING` | 头信息 + **探针进度 Tab 默认选中** + 子任务进度条（见下） |
| `SUCCESS` | 完整 KPI + 默认 Tab「概览」 |
| `PARTIAL_FAILED` | 同 SUCCESS + 顶栏 `el-alert type="warning"`「部分子任务失败，评分基于成功样本」 |
| `FAILED` | `el-result icon="error"` + 失败日志摘要 +「重试」 |
| `CANCELLED` | `el-result` + 只读头信息 |

### RUNNING：探针进度（`probe_task`）

| 列 | 字段 | 展示 |
|----|------|------|
| 问题摘要 | `question_id` → join `question_bank.question` | 截断 + tooltip |
| 平台 | `platform` | Tag |
| 探针模式 | `probe_mode` | Tag |
| 状态 | `status` | PENDING/DISPATCHED/RUNNING/SUCCESS/FAILED/RETRY |
| 重试 | `retry_count` | 数字 |
| 耗时 | `dispatched_at`, `finished_at` | 计算 |

汇总：`successCount / totalCount` → 页头 `el-progress`。

---

## Tab 1：概览（FR-105 / PRD §10）

数据来源：run 级聚合 `score_json`（API 从 `diagnostic_result` 聚合后返回，结构见 PRD §10.3）。

### KPI 六宫格

| 指标 | `metrics` 键 | 展示 |
|------|--------------|------|
| 品牌出现率 | `brand_mention_rate` | `el-statistic` + `%`；权重 25% 小字 |
| Top3 推荐率 | `top3_rate` | 同上；20% |
| 竞品压制指数 | `competitor_suppression` | 反向色（越高越危险）；15% |
| 引用链接覆盖 | `citation_coverage` | 15% |
| 长尾覆盖率 | `longtail_coverage` | 15% |
| 内容资产完整度 | `asset_completeness` | 10% |

组件：`el-row` 6×`el-col :lg="4" :md="8" :xs="12"`，`el-card shadow="never"` 浅底。

### 图表（MVP）

| 图表 | 数据 | 组件建议 |
|------|------|----------|
| 六维指标 | `metrics` | 6 条 `el-progress`（`:stroke-width="12"`） |
| 生命周期 | `by_stage` | `el-table` 或 ECharts 柱状（plus-ui 已有 ECharts 可复用） |
| 竞品 Top5 | `competitors[]` | 横向条形对比客户 vs 竞品 mention_rate |

### 优化建议

- 数据源：`score_json.recommendations[]` 或后端生成文本数组
- UI：`el-timeline` 或带 `el-tag`「建议类型」的列表
- CTA（P2）：「转为内容任务」「转为落地页任务」→ disabled + tooltip 直至 EPIC-3/6

---

## Tab 2：问题明细（`diagnostic_result` + `question_bank`）

### 筛选栏

| 字段 | 组件 | 参数 |
|------|------|------|
| 平台 | `el-select` | `platform` |
| 生命周期阶段 | `el-select` | join `question_bank.stage` |
| 品牌是否提及 | `el-select` | 是/否（解析 `mentioned_brands_json`） |
| Top3 | `el-select` | `rank` ≤ 3 |
| 人工修正 | `el-checkbox` | `human_corrected=true` |
| 关键词 | `el-input` | 问题文本模糊 |

### 表格列

| 列 | 宽度 | 字段 | 展示 |
|----|------|------|------|
| 问题 | min 240 | `question_bank.question` | 2 行 clamp + expand |
| 阶段 | 90 | `question_bank.stage` | Tag（八阶段中文映射） |
| 平台 | 90 | `platform` | Tag |
| 探针 | 100 | `probe_mode` | 小 Tag |
| 排名 | 70 | `rank` | 数字；无则 `—` |
| 品牌 | 80 | `mentioned_brands_json` | 客户品牌高亮 `el-tag success` |
| 竞品 | 120 | `competitors_json` | Tag 组，最多 2 +「+N」 |
| 引用数 | 70 | `citations_json` length | 数字 |
| 采样时间 | 140 | `sampled_at` | datetime |
| 操作 | 100 | — | 「详情」「修正」 |

### 行展开（`el-table type="expand"`）

| 区块 | 字段 | 组件 |
|------|------|------|
| AI 回答摘要 | `answer_text` | `el-text` 最多 6 行 +「展开全文」 |
| 引用列表 | `citations_json` | `el-table` 子表：url、title、domain、is_customer |
| 链接 | `links_json` | 外链列表 |
| 截图 | `screenshot_url` | `el-image` preview（FR-117，可选） |
| 原始元数据 | `model`, `capture_method`, `sampled_at` | `el-descriptions size="small"` |

### 人工修正抽屉（FR-104）

**触发**：行操作「修正」→ `el-drawer` 480px

| 字段 | 可编辑 | 说明 |
|------|:------:|------|
| `rank` | ✅ | `el-input-number` 1–10 |
| `mentioned_brands_json` | ✅ | Tag 输入 |
| `competitors_json` | ✅ | Tag 输入 |
| `answer_text` | 只读 | 参考 |
| `human_corrected` | 自动 true | 保存时置位 |

提交：`PUT /api/v1/diagnostics/results/{resultId}` → 刷新行 + 提示「将触发重新计分」（异步）。

---

## Tab 3：竞品对比（FR-105 / FR-107 占位）

| 区块 | 内容 |
|------|------|
| 对比表 | 客户品牌 vs `competitors[]`：mention_rate、top3_rate、压制差值 |
| 高亮 | 压制差值 &gt; 0.3 行背景 `--tg-color-accent-light` |
| 趋势 | MVP：`el-empty`「完成第二次诊断后可对比趋势（FR-108）」 |

---

## Tab 4：探针进度

- SUCCESS 态：只读 `probe_task` 完成列表（同 RUNNING 表，默认折叠）
- RUNNING 态：同前，自动刷新（轮询 5s 或 WebSocket P2）

---

## 字段 ↔ DDL 对照（明细 Tab）

| UI | `diagnostic_result` 列 | API camelCase |
|----|------------------------|---------------|
| 结果 ID | `id` | `id` |
| 任务 ID | `run_id` | `runId` |
| 问题 ID | `question_id` | `questionId` |
| 平台 | `platform` | `platform` |
| 探针模式 | `probe_mode` | `probeMode` |
| 模型 | `model` | `model` |
| 回答 | `answer_text` | `answerText` |
| 提及品牌 | `mentioned_brands_json` | `mentionedBrands` |
| 竞品 | `competitors_json` | `competitors` |
| 链接 | `links_json` | `links` |
| 引用 | `citations_json` | `citations` |
| 排名 | `rank` | `rank` |
| 分项得分 | `score_json` | `score` |
| 人工修正 | `human_corrected` | `humanCorrected` |
| 采样时间 | `sampled_at` | `sampledAt` |

`question_bank` join：`question`, `stage`, `is_longtail`, `market`.

---

## 空 / 加载 / 错误

| 状态 | UI |
|------|-----|
| 加载 | 页级 `v-loading` |
| 无权限/跨 tenant | `el-result 403` |
| runId 不存在 | `el-result 404` + 返回列表 |
| 无结果行 | Tab 问题明细 `el-empty`「探针尚未产生结果」 |
| 导出中 | 按钮 loading +「报告生成中，约 1–3 分钟」 |

---

## 合规与固定文案（必须展示）

页头 `el-alert` 或 descriptions 脚注，**不可隐藏**：

- `probe_mode`：如 `grounded-api`
- `sampled_at` 范围：最早–最晚采样时间
- `region` / `locale`
- 参与 `platform` 列表
- 免责声明：「本报告为指定时刻、指定平台的采样结果，不代表持续排名承诺。」

---

## 响应式

| 断点 | 行为 |
|------|------|
| ≥1200px | KPI 6 列；概览左右双栏 |
| 768–1199px | KPI 3×2；图表上下堆叠 |
| &lt;768px | 隐藏竞品/引用数列；Tab 改 `el-dropdown` 切换；导出收进「更多」 |

---

## 关联 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/diagnostics/{runId}` | run 头 + 聚合 `scoreJson` + 进度 |
| GET | `/api/v1/diagnostics/{runId}/results` | 问题明细分页 |
| GET | `/api/v1/diagnostics/{runId}/probe-tasks` | 探针子任务 |
| PUT | `/api/v1/diagnostics/results/{resultId}` | FR-104 人工修正 |
| GET | `/api/v1/diagnostics/{runId}/report?format=docx\|pdf` | FR-106 导出 |
| POST | `/api/v1/diagnostics/{runId}/cancel` | 取消 |
| POST | `/api/v1/diagnostics/{runId}/retry` | 失败重试 |

---

## 菜单 / 路由

```text
隐藏路由（TagsView 仍显示）：
  path: /diagnostics/runs/:runId
  component: tourgeo/diagnostics/detail
  meta: { activeMenu: '/diagnostics/runs', title: '诊断详情' }
```

列表页 `name` 列 /「查看」→ `router.push({ name: 'DiagnosticDetail', params: { runId } })`

---

## 实现参考

- 列表页：`inbound-admin/src/views/tourgeo/diagnostics/index.vue`
- Tab 详情模式：plus-ui `monitor/cache` 或 `el-descriptions` + `el-tabs`
- 分数色阶：`docs/design/tokens.md` §1.4
- 八阶段中文：`灵感/种草/比较/签证/规划/信任/决策/复购`（PRD 用户生命周期）

---

## 与项目详情子页的关系

| 页面 | 优先级 | 说明 |
|------|:------:|------|
| **本页（诊断详情）** | P0 EPIC-2 | 承接列表「查看结果」 |
| 项目详情 Tab（品牌/竞品/知识库） | P1 Story 3 | FR-002/003/004；独立线框 `project-detail.md` 待补 |

---

## M2 增量 · 校准对比 Tab（FR-115）

> **PRD**：FR-115 API 与网页版校准 · **ADR-20260709-22**  
> **前置**：创建诊断时 `calibration_ratio` > 0（见 [diagnostics-list.md](./diagnostics-list.md) 滑块 0–30%）且探针模式含 **grounded-api** + **browser-extension**  
> **关联**：[probe-adapters.md](./probe-adapters.md) FR-116

**M2 范围（本页）**：
- ✅ 新增 Tab **「校准对比」**：偏差率 KPI · 重叠问题对比表 · 双模式答案摘要
- ✅ 任务头展示 `calibration_ratio`（若有）
- ❌ FR-117 截图样例 · 报告导出嵌入校准章节（M2 只读 Tab）

---

### Tab 可见性

| 条件 | UI |
|------|-----|
| `calibrationRatio === 0` 或未勾选 `browser-extension` | **不渲染** Tab（减少噪音） |
| `calibrationRatio > 0` 且双模式已选 · run 未完成 | Tab 可见 · `el-empty`「任务完成后展示校准对比」 |
| SUCCESS / PARTIAL_FAILED · 有 pairs | 完整 KPI + 对比表 |
| 已完成 · `pairedCount === 0` | `el-empty`「无重叠抽样问题，请提高校准比例或检查探针节点」+ link 探针节点 |

**Tab 顺序**（SUCCESS 态）：

```text
[概览] [问题明细] [竞品对比] [探针进度] [校准对比]
```

RUNNING 且含校准：可与「探针进度」并列；默认仍「探针进度」。

---

### M2 任务头增量

| UI 元素 | DDL | 展示 |
|---------|-----|------|
| 校准比例 | `calibration_ratio` | 当 `> 0` 时 descriptions 一行：「校准抽样 10% · API vs 扩展重叠对比」 |
| 探针模式 | `probe_modes_json` | 已有 Tag 组；强调同时含 grounded-api + browser-extension |

---

### M2 布局增量（ASCII）

```
Tab: 校准对比 (FR-115)
┌─ 说明 el-alert type="info" ────────────────────────────────────────────────┐
│ 对同一问题同时采样 grounded-api 与 browser-extension，对比品牌提及与回答差异。│
│ 偏差率越高表示 API 与网页版结果越不一致，需关注 adapter 或平台变更。          │
└────────────────────────────────────────────────────────────────────────────┘
┌─ KPI el-row :gutter="16" ──────────────────────────────────────────────────┐
│ [综合偏差率 18%]  [品牌一致率 82%]  [重叠样本 12/15]  [平台 Perplexity]     │
└────────────────────────────────────────────────────────────────────────────┘
┌─ 重叠问题对比 el-table border ─────────────────────────────────────────────┐
│ 问题 | 阶段 | 平台 | 品牌一致 | 相似度 | 偏差 | 操作                          │
│ Best private China tour… | 规划 | Perp | ✗ | 62% | 高 warning | [展开对比] │
└────────────────────────────────────────────────────────────────────────────┘
  行展开 (type="expand"):
┌─ 双栏对比 el-row ────────────────────────────────────────────────────────────┐
│ ┌─ grounded-api ──────────────┐  ┌─ browser-extension ───────────────────┐ │
│ │ Tag primary · Perplexity    │  │ Tag warning · 扩展节点 dev-probe-1     │ │
│ │ 品牌提及 [是 Tag success]   │  │ 品牌提及 [否 Tag info]                 │ │
│ │ 排名 2  |  引用 5           │  │ 排名 —  |  引用 3                      │ │
│ │ ── 回答摘要 ──              │  │ ── 回答摘要 ──                         │ │
│ │ China Highlights offers…    │  │ Several tour operators provide…        │ │
│ │ (最多 8 行 + 展开全文 P2)   │  │                                        │ │
│ │ [查看完整结果 → 问题明细]   │  │ [查看完整结果 → 问题明细]              │ │
│ └─────────────────────────────┘  └────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────┘
┌─ footnote ─────────────────────────────────────────────────────────────────┐
│ M2 指标：品牌 mention 一致率 + 回答文本相似度（Jaccard/contains 简化）。      │
│ 无截图存证（FR-117 M3+）。 [检查平台 Adapter → /settings/probe-adapters]    │
└────────────────────────────────────────────────────────────────────────────┘
```

---

### KPI 卡片

| KPI | API 字段 | 展示 | 色阶 |
|-----|----------|------|------|
| 综合偏差率 | `deviationRate` | `el-statistic` + `%` · tooltip「1 − 平均一致度」 | ≤15% success · 15–30% warning · &gt;30% danger |
| 品牌一致率 | `brandMentionAgreementRate` | `%` | 反向：越高越好 |
| 重叠样本 | `pairedCount` / `sampleCount` | `12 / 15` | plain |
| 涉及平台 | `pairs[].platform` 去重 | Tag 组 | — |

**无数据**：KPI 显示 `—`。

---

### 对比表列

| 列 | 宽度 | 来源 | 展示 |
|----|------|------|------|
| 问题 | min 220 | `question` join | 2 行 clamp + tooltip |
| 阶段 | 90 | `stage` | 八阶段 Tag |
| 平台 | 100 | `platform` | Tag |
| 品牌一致 | 90 | `brandMatch` | ✓ success / ✗ danger |
| 相似度 | 90 | `similarityScore` | `%` · &lt;0.5 warning |
| 偏差 | 80 | `deviationScore` | Tag：低/中/高 |
| 操作 | 100 | — | 「展开对比」或依赖 table expand |

**排序**：默认 `deviationScore` **降序**（偏差大的在上）。

**筛选（P2）**：平台 · 仅品牌不一致 · 仅高偏差。

---

### 行展开 · 双模式答案摘要

| 侧 | 数据路径 | 区块 |
|----|----------|------|
| **grounded-api** | `pair.groundedApi` | Tag + `brandMentioned` · `rank` · `citationCount` · `answerPreview` |
| **browser-extension** | `pair.browserExtension` | Tag + 可选 `probeNodeKey` · 同上 |

| 字段 | API camelCase | 展示 |
|------|---------------|------|
| 结果 ID | `resultId` | 链接跳转问题明细 Tab 并 scroll 到行（P2） |
| 回答摘要 | `answerPreview` | 最多 **8 行** · `line-clamp` |
| 品牌提及 | `brandMentioned` | 是/否 Tag |
| 排名 | `rank` | 数字或 `—` |
| 引用数 | `citationCount` | 数字 |

**「查看完整结果」**：切换 Tab「问题明细」并 filter `questionId`（P2）；M2 可仅 tooltip resultId。

---

### API 依赖（M2 追加）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/projects/{projectId}/diagnostics/{runId}/calibration` | 校准对比数据 |

**响应结构**（与 Java M2 HANDOFF 对齐）：

```json
{
  "deviationRate": 0.18,
  "brandMentionAgreementRate": 0.82,
  "sampleCount": 15,
  "pairedCount": 12,
  "pairs": [
    {
      "questionId": 42,
      "question": "Best private China tour for first-time visitors?",
      "stage": "planning",
      "platform": "perplexity",
      "brandMatch": false,
      "similarityScore": 0.62,
      "deviationScore": 0.38,
      "groundedApi": {
        "resultId": 1001,
        "answerPreview": "China Highlights offers curated 10-day itineraries…",
        "brandMentioned": true,
        "rank": 2,
        "citationCount": 5
      },
      "browserExtension": {
        "resultId": 1002,
        "probeNodeKey": "dev-probe-1",
        "answerPreview": "Several tour operators provide private China tours…",
        "brandMentioned": false,
        "rank": null,
        "citationCount": 3
      }
    }
  ]
}
```

**加载**：Tab 首次激活时请求 · `v-loading` · 失败 `ElMessage.error`。

---

### 与创建诊断表单关系

[diagnostics-list.md](./diagnostics-list.md) 已有 **校准比例** 滑块 0–30%：

| `calibration_ratio` | 行为 |
|---------------------|------|
| `0` | 不生成重叠 browser-extension 任务 · 本 Tab 隐藏 |
| `0.1`（10%） | M2 简化：同一批 question 各 1 样本双模式（Java 实现） |
| 需同时勾选 | `grounded-api` + `browser-extension` · 否则表单项 warning |

**合规 footnote**（创建抽屉已有）：报告将标注 probe_mode；校准 Tab 只读展示，不承诺排名。

---

### M2 空 / 错误

| 场景 | UI |
|------|-----|
| Tab 隐藏 | 不满足双模式+ratio 条件 |
| run 进行中 | empty + 「探针执行中，请稍后刷新」 |
| 扩展全失败 | alert warning「browser-extension 子任务无成功样本，无法校准」 |
| adapter 停用 | footnote 链 probe-adapters |

---

### M2 组件提示

| 项 | 建议 |
|----|------|
| Tab | `diagnostics/detail.vue` 新增 `calibration` pane |
| API | `src/api/tourgeo/diagnostic.ts` — `getDiagnosticCalibration(projectId, runId)` |
| 双栏 | `el-col :span="12"` · 小屏 stack |
| 常量 | `CALIBRATION_DEVIATION_META` — 低/中/高阈值 |

**交叉引用**：[probe-adapters.md](./probe-adapters.md) · Java HANDOFF `2026-07-09-tech-director-to-dev-java-epic11-probe-m2.md`

---

## 版本

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-06-26 | UI 设计 | EPIC-2 初版 · FR-104~106 |
| 2026-07-09 | UI 设计 | **§M2** 校准对比 Tab FR-115 · ADR-20260709-22 |
