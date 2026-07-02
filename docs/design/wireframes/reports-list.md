# 线框：报告中心 · 报告列表（EPIC-8 M1）

> **PRD**：§8.9 报告中心 · **FR-701** 诊断报告 · **FR-702** 周报生成  
> **EPIC**：EPIC-8 M1 · ADR-20260702-15  
> **路由**：`/projects/:projectId/reports`（侧栏 `/reports` 重定向当前项目）  
> **数据表**：`report`（`report_type`：`DIAGNOSTIC` | `WEEKLY` | …）

---

## 页面目标

在项目上下文中 **统一管理已生成的报告**：展示 FR-106 写入的诊断报告（FR-701）、**手动生成本周增长周报**（FR-702），并支持 **下载 DOCX / PDF**。

**M1 范围**：
- ✅ 列表 + 类型筛选 + 生成本周报告 dialog + 下载 DOCX/PDF
- ✅ DIAGNOSTIC 行跳转诊断详情（`summary.runId`）
- ✅ 详情 drawer 只读预览 summary KPI
- ❌ 白标模板配置（FR-704）
- ❌ 月报（FR-703）、自动推送（FR-705）、自定义报告（FR-706）
- ❌ XXL-Job 定时周报、MinIO `file_url` 持久化预览

**入口**：
- 侧栏「报告中心」→ 报告列表
- [diagnostic-detail.md](./diagnostic-detail.md) 导出 DOCX/PDF 后 → 本页可见新 DIAGNOSTIC 行（P2 toast 链入）
- 工作台 KPI「本周报告」→ 本页 + 筛选 `type=WEEKLY`（P2）

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 面包屑：报告中心 / 报告列表          当前项目 ▼ Dragon Journey Travel       │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─ 项目选择器 (同 leads-list) ─────────────────────────────────────────────┐ │
│ │ 客户项目 [Dragon Journey ▼]   目标市场 US UK AU                          │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 搜索区 (el-card, 可折叠) ───────────────────────────────────────────────┐ │
│ │ 报告类型 [全部▼]  周期/关联 [____]  创建时间 [日期范围]  [搜索] [重置]    │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 工具栏 ─────────────────────────────────────────────────────────────────┐ │
│ │ [生成本周报告] primary   [月报] disabled  [模板配置] disabled             │ │
│ │                              [显示搜索] [刷新]                            │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ el-table border v-loading ──────────────────────────────────────────────┐ │
│ │ 类型 | 周期/关联 | 摘要 | 创建时间 | 操作                                 │ │
│ │ 诊断报告 | run #42 | GEO Score 85 · 竞品 3 家 | 2026-06-29 10:20 | ↓↓ 预览│ │
│ │ 增长周报 | 2026-W26 | 询盘 4 · 新词 12 · GEO +5 | 2026-07-02 09:15 | ↓↓ 预览│ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                              [分页 10/20/50]                                │
│ ℹ 诊断报告由 GEO 详情页导出写入；周报由本页手动生成。                         │
└─────────────────────────────────────────────────────────────────────────────┘

生成本周报告 (el-dialog 480px):
┌─ 生成本周增长报告 (FR-702) ─────────────────────────── [×] │
│ 统计区间*   [2026-06-25] 至 [2026-07-01]  (默认近 7 日)     │
│ 报告周期    2026-W26  (ISO week，只读，由结束日计算)         │
│ ── 将聚合 ──                                                │
│ · GEO 诊断次数与可见率变化                                   │
│ · 关键词新增与八阶段分布                                     │
│ · 内容任务 / 落地页 / 询盘数量                               │
│ · 3 条静态优化建议（无 AI 摘要）                             │
│ ⚠ 同周期重复生成将创建新记录（不覆盖）。                      │
│                              [取消]  [生成报告]              │
└─────────────────────────────────────────────────────────────┘

生成中:
  dialog 按钮 loading「正在聚合数据…」
  成功 toast「周报已生成」→ 关闭 dialog → 刷新列表 → 可选打开预览 drawer

预览 (el-drawer 600px):
┌─ 报告预览 · 增长周报 2026-W26 ──────────────────────── [×] │
│ [增长周报 Tag]  2026-06-25 ~ 2026-07-01                    │
│ ── KPI 卡片 (el-row :gutter="12") ──                       │
│ [GEO 85 ↑5] [询盘 4] [新词 12] [内容 1] [落地页 3]          │
│ ── 章节摘要 (el-descriptions / el-collapse) ──             │
│ ▼ GEO 可见率 · 关键词 · 内容 · 落地页 · 询盘 · 建议        │
│ ── 合规 ──                                                  │
│ 免责声明：数据来自系统统计区间，非排名承诺。                  │
│ [关闭]  [下载 DOCX]  [下载 PDF]                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 项目选择器

与 [leads-list.md](./leads-list.md) **一致**：

| 项 | 规范 |
|----|------|
| 路由 | `/projects/:projectId/reports` |
| 切换 | `router.replace({ name: 'ReportsList', params: { projectId } })` + 重置筛选 |
| 无项目 | `el-empty`「请先创建客户项目」→ `/projects` |

---

## 筛选栏

| 字段 | 组件 | 查询参数 | 说明 |
|------|------|----------|------|
| 报告类型 | `el-select` | `type` | `report_type`；M1 常用 `DIAGNOSTIC` / `WEEKLY` |
| 周期/关联 | `el-input` | `period` | 模糊：`2026-W26` 或 `run` 号 |
| 创建时间 | `el-date-picker` daterange | `createdAt` | 映射 `created_at` |

**Query 深链**（P2）：
- `?type=WEEKLY` — 工作台链入
- `?type=DIAGNOSTIC&runId=42` — 诊断导出后高亮（可选）

---

## 表格列 ↔ DDL / API（HANDOFF 必含列）

| 列 | 宽度 | DDL 列 | API 字段 | 展示 |
|----|------|--------|----------|------|
| 类型 | 110 | `type` | `type` | Tag 中文 |
| 周期/关联 | 140 | `period` | `period` + `summary` | 见下 |
| 摘要 | min 240 | `summary` | `summaryPreview` 或前端解析 | 单行 KPI 文案 |
| 创建时间 | 160 | `created_at` | `createdAt` | `YYYY-MM-DD HH:mm` |
| 操作 | fixed 200 | — | — | 见行操作 |

### `report_type` Tag

| DB 值 | Tag type | 中文 | M1 |
|-------|----------|------|-----|
| `DIAGNOSTIC` | `primary` | 诊断报告 | ✅ FR-701 / FR-106 |
| `WEEKLY` | `success` | 增长周报 | ✅ FR-702 |
| `MONTHLY` | `warning` | 增长月报 | ✅ FR-703（§M2） |
| `CUSTOM` | `info` | 自定义 | FR-706 disabled |

### 周期/关联列

| type | `period` 示例 | 展示 |
|------|---------------|------|
| `DIAGNOSTIC` | `42` 或 `run:42` | `诊断 #42`；可点击 → `/diagnostics/runs/42` |
| `WEEKLY` | `2026-W26` | ISO 周标签 + 副行日期区间（来自 summary） |

### 摘要列（一行）

**DIAGNOSTIC**（`summary` JSON 含 `runId`, `geoScore`, …）：

```
GEO Score 85 · 品牌出现率 72% · 竞品 3 家
```

**WEEKLY**（聚合 summary）：

```
询盘 4 · 新词 12 · 内容生成 1 · GEO +5
```

解析失败时 fallback：`summary` 前 80 字 + ellipsis。

---

## 行操作（M1）

| 操作 | 条件 | 行为 |
|------|------|------|
| 预览 | 全部 | 打开预览 drawer；`GET .../reports/{reportId}` |
| 下载 DOCX | 全部 | `GET .../export?format=docx` → blob 下载 |
| 下载 PDF | 全部 | `GET .../export?format=pdf` → blob 下载 |
| 查看诊断 | `DIAGNOSTIC` | 跳转 `/diagnostics/runs/{runId}` |
| 删除 | — | M1 **不提供**（审计保留） |

下载按钮组：`el-button-group` 或 link 按钮 + `el-tooltip`。

**导出 loading**（同 [diagnostic-detail.md](./diagnostic-detail.md)）：
- 单按钮 loading；文案「报告生成中，约 10–30 秒…」
- PDF 依赖 Gotenberg；失败 `ElMessage.error`「PDF 服务不可用，请下载 DOCX 或联系管理员」

---

## 生成本周报告 Dialog（FR-702）

| UI 标签 | API | 必填 | 说明 |
|---------|-----|:----:|------|
| 统计区间 | `periodStart`, `periodEnd` | ✅ | `el-date-picker` daterange；默认 **今天往前 7 日** |
| 报告周期 | `period` | — | 只读；由 `periodEnd` 算 ISO week `YYYY-Www` |
| 生成 | — | — | `POST /api/v1/projects/{projectId}/reports/weekly` |

**请求体示例**：

```json
{
  "periodStart": "2026-06-25",
  "periodEnd": "2026-07-01"
}
```

**成功**：201/200 + 新 `reportId` → 刷新列表 → toast「周报已生成」→ 可选自动打开预览 drawer。

**校验**：
- `periodEnd >= periodStart`
- 区间 ≤ 31 天（M1 限制，防滥用）
- 空数据仍生成（各 KPI 为 0 + 建议模板）

**重复生成**：同 `period` 可有多条记录（ADR：不覆盖）；列表按 `created_at` 降序。

---

## 预览 Drawer（FR-701 / FR-702）

**触发**：行「预览」或摘要列点击；宽度 `600px`；`destroy-on-close`。

### 诊断报告（`type=DIAGNOSTIC`）

| 区块 | 内容 |
|------|------|
| 头 | Tag + `诊断 #runId` + 创建时间 |
| KPI | `geoScore`、Top3 率、竞品数（来自 `summary`） |
| 链接 | `[查看完整诊断结果]` → diagnostic-detail |
| 合规 | probe_mode / sampled_at / region（若 summary 含） |
| 底栏 | 下载 DOCX / PDF |

### 增长周报（`type=WEEKLY`）

| 区块 | summary 路径 | 展示 |
|------|--------------|------|
| 头 | `period`, `periodStart`~`periodEnd` | Tag + 日期 |
| KPI 卡片 | `geo.latestScore`, `leads.newCount`, `keywords.newCount`, … | 5 格 mini stat |
| GEO | `geo.runs`, `geo.latestScore`, `geo.delta` | descriptions |
| 关键词 | `keywords.newCount`, `keywords.byStage` | 八阶段 mini bar 或 Tag 列表 |
| 内容 | `content.tasksCreated`, `content.generated` | |
| 落地页 | `landing.draftCount` | |
| 询盘 | `leads.newCount` | |
| 建议 | `recommendations[]` | `el-timeline` 3 条 |
| 合规 | 固定 disclaimer | |
| 底栏 | 下载 DOCX / PDF | |

**summary JSON 结构**（与 Java HANDOFF 对齐）：

```json
{
  "periodStart": "2026-06-25",
  "periodEnd": "2026-07-01",
  "runId": 42,
  "geoScore": 85,
  "geo": { "runs": 2, "latestScore": 85, "delta": 5 },
  "keywords": { "newCount": 12, "byStage": { "inspiration": 3 } },
  "content": { "tasksCreated": 2, "generated": 1 },
  "landing": { "draftCount": 3 },
  "leads": { "newCount": 4 },
  "recommendations": ["Increase GEO sampling frequency.", "…", "…"]
}
```

DIAGNOSTIC 行 `summary` 至少含 `runId`、`geoScore`；其余字段可选。

---

## 字段 ↔ DDL 完整对照

| UI | DDL 列 | API camelCase | 列表 | 预览 |
|----|--------|---------------|:----:|:----:|
| ID | `id` | `id` | — | ✅ |
| 类型 | `type` | `type` | ✅ | ✅ |
| 周期 | `period` | `period` | ✅ | ✅ |
| 摘要 | `summary` | `summary` | ✅ 一行 | ✅ 结构化 |
| 文件 URL | `file_url` | `fileUrl` | — | M2 MinIO |
| 模板 | `template_id` | `templateId` | — | ✅ FR-704（§M2 导出套白标） |
| 创建时间 | `created_at` | `createdAt` | ✅ | ✅ |
| 创建人 | `created_by` | `createdBy` | P2 | P2 |

---

## 工具栏 disabled 项（M1 占位）

| 按钮 | tooltip |
|------|---------|
| 月报 | 「月度增长报告 FR-703 · M2」 |
| 模板配置 | 「白标模板 FR-704 · M2」 |
| 自动推送 | 「定时推送 FR-705 · M2」（可不渲染，仅文档注明） |

---

## 空 / 加载 / 错误

| 状态 | UI |
|------|-----|
| 加载 | `v-loading` |
| 无报告 | `el-empty` description「暂无报告」；sub「完成 GEO 诊断并导出，或生成本周增长报告」；按钮「生成本周报告」「前往诊断」→ `/diagnostics` |
| 无项目 | → `/projects` |
| 周报生成失败 | dialog 内 error；保留表单 |
| 导出失败 | `ElMessage.error` + 保留行 |
| Gotenberg 不可用 | PDF 按钮仍可点但 error 提示（同 diagnostic-detail） |

---

## 响应式

| 断点 | 行为 |
|------|------|
| ≥1200px | 全列 |
| 768–1199px | 摘要列 `show-overflow-tooltip` |
| &lt;768px | 类型、摘要、创建时间、操作（DOCX/PDF 收进 dropdown）；drawer 100% 宽 |

---

## 菜单与路由

```text
父菜单：报告中心 (path: /reports, icon: document 或 data-analysis)
  └─ 报告列表 (path: /projects/:projectId/reports, component: tourgeo/reports/index)

侧栏快捷 /reports → redirect 到 projectStore.currentProjectId
```

**权限标识建议**：`tourgeo:report:list`、`tourgeo:report:export`、`tourgeo:report:weekly`

---

## API 依赖（开发对齐）

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/api/v1/projects/{projectId}/reports` | 分页列表 + `type` / `period` / 时间筛选 |
| GET | `/api/v1/projects/{projectId}/reports/{reportId}` | 预览 drawer |
| POST | `/api/v1/projects/{projectId}/reports/weekly` | 生成本周报告 |
| GET | `/api/v1/projects/{projectId}/reports/{reportId}/export?format=docx\|pdf` | 下载 |

List 响应建议额外字段：`summaryPreview`（后端拼好一行，减轻前端解析）。

分页：`page`, `size`；响应 `{ total, list }`。

**与诊断详情关系**：
- FR-106 仍可用 `GET /api/v1/diagnostics/{runId}/report?format=` 直接导出
- 本页 `export` 为统一入口；DIAGNOSTIC 行复用 `runId` 渲染

---

## 组件与实现提示

| 项 | 建议 |
|----|------|
| 视图 | `inbound-admin/src/views/tourgeo/reports/index.vue` |
| API | `src/api/tourgeo/report.ts` |
| 下载 | 复用 `diagnostic.ts` 的 `downloadDiagnosticReport` 模式（`responseType: 'blob'` + `FileSaver`）；新建 `downloadReport(projectId, reportId, format)` |
| 常量 | `src/constants/report.ts` — `REPORT_TYPE_LABELS` |
| 参考 | `leads/index.vue` 列表模式；`diagnostics/detail` 导出按钮 |

---

## 版本

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-02 | UI 设计 | EPIC-8 M1 初版 · FR-701/702 · ADR-20260702-15 |
| 2026-07-08 | UI 设计 | **§M2** 月报 dialog FR-703 · 预览 MoM/CRM · 启用模板配置入口 · ADR-20260708-21 |

---

## M2 增量 · 月报生成（FR-703）

> **PRD**：FR-703 月度增长报告 · **ADR-20260708-21**  
> **前置**：M1 周报 + 列表 ✅ · EPIC-7 M2 CRM 线索 status 统计 ✅  
> **白标**：导出套模板见 [report-template-settings.md](./report-template-settings.md)（FR-704）

**M2 范围（本页）**：
- ✅ 工具栏启用「生成月报」dialog + 「模板配置」链 `/settings/report-template`
- ✅ 列表 `type=MONTHLY` · Tag「增长月报」· 筛选联动
- ✅ 预览 drawer 扩展：MoM KPI · 线索按 status · WON 数
- ❌ FR-705 自动推送 · FR-706 自定义报告 · XXL-Job 定时 · LLM 摘要

---

### M2 工具栏变更

| 按钮 | M1 | M2 | 行为 |
|------|----|----|------|
| 生成本周报告 | ✅ primary | ✅ | 不变 |
| **生成月报** | disabled | ✅ `type="warning" plain` | 打开月报 dialog |
| **模板配置** | disabled | ✅ link / default | `router.push('/settings/report-template')` |
| 显示搜索 / 刷新 | ✅ | ✅ | 不变 |

**仍 disabled（仅文档）**：自动推送 FR-705 · 自定义报告 FR-706

**权限建议**：`tourgeo:report:monthly` · `tourgeo:report:template`（查看/编辑模板）

**未配置模板时**：月报 dialog 顶栏 `el-alert type="info"`「尚未配置报告白标，导出将使用系统默认样式」+ link「去配置模板」。

---

### M2 布局增量（ASCII）

```
工具栏 (M2):
│ [生成本周报告] primary   [生成月报] warning plain   [模板配置] link → /settings/report-template │
│                              [显示搜索] [刷新]                                                    │

表格行示例:
│ 增长月报 | 2026-06 | GEO 88 MoM +3 · 询盘 18 · 成交 3 | 2026-07-08 11:00 | ↓↓ 预览 │

生成月报 (el-dialog 520px · FR-703):
┌─ 生成月度增长报告 ──────────────────────────────────────── [×] │
│ ℹ 未配置白标时导出使用默认样式。[去配置模板 →]  (可选 alert)   │
│ 统计月份*   [2026-06 ▼]  el-date-picker type="month"          │
│             默认：上一完整自然月（今天 2026-07-08 → 默认 2026-06）│
│ 自然月区间   2026-06-01 至 2026-06-30  (只读，由月份计算)        │
│ ── 环比说明 (MoM) ──                                           │
│ · 对比上一自然月 2026-05 的 GEO 首尾分、询盘总量、关键词新增    │
│ · 月内 CRM：按线索状态统计 + 已成交 (WON) 数                    │
│ ── 将聚合章节 ──                                               │
│ ☑ GEO 可见率与 MoM Δ                                          │
│ ☑ 关键词新增 · 八阶段 · 机会均分                                │
│ ☑ 内容任务 / 落地页                                           │
│ ☑ 询盘与 CRM 漏斗                                             │
│ ☑ 5 条静态优化建议（无 AI 摘要）                               │
│ ⚠ 同月份重复生成将创建新记录；消耗「报告生成」月度额度。         │
│                              [取消]  [生成月报]                │
└────────────────────────────────────────────────────────────────┘

高级选项 (el-collapse · P2 可选，M2 后端已支持):
  ▼ 自定义统计区间
  [2026-06-01] 至 [2026-06-30]  daterange · 跨度 ≤ 62 天
  period 仍写 YYYY-MM（取结束日所在月）
```

**生成中**：按钮 loading「正在聚合月度数据…」· 成功 toast「月报已生成」→ 关闭 dialog → 刷新列表 → 可选打开预览 drawer。

**402 额度不足**：`ElMessage.error`「本月报告生成额度已用尽」+ link「查看套餐」→ `/settings/billing`。

---

### 生成月报 Dialog 字段 ↔ API

| UI 标签 | 组件 | API 字段 | 必填 | 说明 |
|---------|------|----------|:----:|------|
| 统计月份 | `el-date-picker` `type="month"` `value-format="YYYY-MM"` | `year`, `month` 或 `period` | ✅ | 默认 **上一完整自然月** |
| 自然月区间 | 只读文本 | — | — | 由所选月算 `periodStart`/`periodEnd` |
| 高级区间 | daterange（P2 UI） | `periodStart`, `periodEnd` | — | 跨度 ≤ 62 天；与 `year/month` 二选一 |
| 生成 | primary 按钮 | — | — | `POST .../reports/monthly` |

**请求体示例（推荐）**：

```json
{
  "year": 2026,
  "month": 6
}
```

**成功**：201/200 + `reportId` · `type=MONTHLY` · `period=2026-06` · `template_id` 写入当前租户 REPORT 模板。

**校验**：
- 不可选未来月（`selectedMonth > 当前月` disabled）
- 同 `period` 可有多条记录（不覆盖）
- 空数据仍生成（KPI 为 0 + 静态建议）

**扣额**：`reports_per_month`（与周报共用，见 [billing-settings.md](./billing-settings.md)）

---

### M2 列表增量

| 变更 | 说明 |
|------|------|
| 类型筛选 | `MONTHLY` 可选 · Tag「增长月报」`warning` |
| 周期/关联 | `period=2026-06` → 展示 `2026年6月` + 副行 `06-01 ~ 06-30`（summary） |
| 摘要列 | `GEO 88 MoM +3 · 询盘 18 · 成交 3`（`summaryPreview` 或前端拼） |
| 行操作 | 同 M1：预览 / DOCX / PDF |
| 删除 | 仍不提供 |

#### `MONTHLY` 摘要一行示例

```
GEO 88 MoM +3 · 询盘 18 · 新词 45 · 成交 3
```

解析失败 fallback 同 M1。

---

### M2 预览 Drawer · 增长月报（`type=MONTHLY`）

宽度仍 `600px`；头区增加 **白标快照**（若 summary 含 `templateSnapshot`）：

| 区块 | summary 路径 | 展示 |
|------|--------------|------|
| 头 | `period`, `periodStart`~`periodEnd` | Tag「增长月报」+ `2026年6月` |
| 环比条 | `geo.momDelta`, `geo.prevScore` | `el-statistic` 或 inline：「较上月 GEO +3（85→88）」 |
| KPI 卡片 | `geo.latestScore`, `leads.newCount`, `leads.wonCount`, `keywords.newCount`, `keywords.avgScore` | 5 格 mini stat · MoM 箭头色 |
| GEO | `geo.runs`, `geo.delta`, `geo.momDelta` | descriptions + MoM 小字 |
| 关键词 | `keywords.newCount`, `keywords.avgScore`, `keywords.byStage` | 八阶段 Tag · 均分 tooltip |
| 内容 | `content.tasksCreated`, `content.generated` | |
| 落地页 | `landing.draftCount`, `landing.publishedCount` | |
| 询盘 CRM | `leads.newCount`, `leads.byStatus`, `leads.wonCount` | **横向 bar 或 el-descriptions** 五态计数 |
| 建议 | `recommendations[]` | `el-timeline` **5 条** |
| 白标 | `templateSnapshot.companyName`, `primaryColor` | 小字「导出将使用 {公司名} 模板」 |
| 合规 | 固定 disclaimer + 「数据来自系统统计，非排名承诺」 | |
| 底栏 | 下载 DOCX / PDF（封面含 Logo · 见模板页） | |

**summary JSON 结构**（与 Java M2 HANDOFF 对齐）：

```json
{
  "periodStart": "2026-06-01",
  "periodEnd": "2026-06-30",
  "periodPrevStart": "2026-05-01",
  "periodPrevEnd": "2026-05-31",
  "geo": {
    "runs": 6,
    "latestScore": 88,
    "delta": 8,
    "momDelta": 3,
    "prevScore": 85
  },
  "keywords": {
    "newCount": 45,
    "avgScore": 72.5,
    "byStage": { "inspiration": 8, "planning": 12 }
  },
  "content": { "tasksCreated": 8, "generated": 5 },
  "landing": { "draftCount": 4, "publishedCount": 2 },
  "leads": {
    "newCount": 18,
    "wonCount": 3,
    "byStatus": {
      "NEW": 5,
      "FOLLOWING": 8,
      "QUOTED": 2,
      "WON": 3,
      "LOST": 0
    }
  },
  "recommendations": [
    "Increase GEO sampling in high-intent markets.",
    "Follow up QUOTED leads within 48h.",
    "…",
    "…",
    "…"
  ],
  "templateSnapshot": {
    "companyName": "Dragon Journey Travel",
    "primaryColor": "#059669"
  }
}
```

**线索 status 中文**（与 [leads-list.md](./leads-list.md) M2 一致）：NEW 新线索 · FOLLOWING 跟进中 · QUOTED 已报价 · WON 已成交 · LOST 已流失。

---

### M2 API 依赖（追加）

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/api/v1/projects/{projectId}/reports/monthly` | 生成月报 |
| GET | `/api/v1/settings/report-template` | 读取租户模板（dialog alert / 预览快照） |
| PUT | `/api/v1/settings/report-template` | 模板配置页保存 |

M1 列表/预览/导出 API 不变；`GET .../reports?type=MONTHLY` 筛选月报行。

**导出白标**：`GET .../export?format=docx|pdf` 服务端读 `template.config_json` 渲染封面 Logo 区 + 页脚；周报/诊断导出 M2 同步套用（Admin 无额外 UI）。

---

### M2 空态 / 错误增量

| 场景 | UI |
|------|-----|
| 首条月报 | 空态 sub 文案追加「或生成月度增长报告」 |
| 月报生成失败 | dialog 内 error · 保留所选月份 |
| 无 CRM 数据 | 预览 drawer 询盘区显示 0 · 五态均为 0 |
| 模板 Logo URL 无效 | 配置页校验 + 导出 fallback 文字 Logo（Java） |

---

### M2 组件提示

| 项 | 建议 |
|----|------|
| 月报 dialog | 同 weekly dialog 组件模式 · `createMonthlyReport` in `report.ts` |
| 月份选择 | `el-date-picker` `type="month"` · `:disabled-date` 禁未来月 |
| 模板入口 | 工具栏 `@click` → `/settings/report-template` · 侧栏见模板页线框 |
| 常量 | `REPORT_TYPE_LABELS.MONTHLY` =「增长月报」 |

**交叉引用**：[report-template-settings.md](./report-template-settings.md) · Java HANDOFF `2026-07-08-tech-director-to-dev-java-epic8-monthly-whitelabel.md`
