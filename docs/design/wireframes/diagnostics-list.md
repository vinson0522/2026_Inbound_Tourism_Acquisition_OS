# 线框：GEO 诊断 · 诊断任务列表（M1 + M3）

> **PRD**：§6.1 GEO 诊断 → 诊断任务 · **FR-103** 批量诊断任务 · **FR-109** 定时诊断（M3）  
> **EPIC**：EPIC-2 M1 · **M3** · ADR-20260710-26  
> **路由**：`/diagnostics`（项目上下文下：`/projects/:projectId/diagnostics` 二选一，开发优先后者 REST 风格）  
> **数据表**：`diagnostic_run` · **`diagnostic_schedule`**（M3 · `001_schema.sql` 追加）

---

## 页面目标

列出当前项目下所有 GEO 诊断任务，支持筛选、新建、查看进度与跳转报告；状态与探针模式清晰可读，符合 grounded-api 合规提示。

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 面包屑：GEO 诊断 / 诊断任务          当前项目 ▼ | [新建诊断任务]        │
├─────────────────────────────────────────────────────────────────────────┤
│ ┌─ 搜索区 (el-card, 可折叠) ─────────────────────────────────────────┐ │
│ │ 任务名称 [____]  状态 [全部▼]  市场 [US▼]  探针模式 [全部▼]         │ │
│ │ 创建时间 [日期范围]                    [搜索] [重置]                  │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 工具栏 ────────────────────────────────────────────────────────────┐ │
│ │ [新建] [导出]                                    [显示搜索] [刷新]  │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│ ┌─ el-table border ──────────────────────────────────────────────────┐ │
│ │ □ | 任务名称 | 市场/语言 | 探针模式 | 平台 | 采样 | 状态 | GEO分 | … │ │
│ │   | Q2 US…  | US/en   | grounded | Perp…| 3   | 执行中| —    | … │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                              [分页 10/20/50]                            │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 搜索表单字段

| 字段 | 组件 | 绑定 | 说明 |
|------|------|------|------|
| 任务名称 | `el-input` | `name` | 模糊搜索 |
| 状态 | `el-select` | `status` | 枚举见 tokens.md |
| 市场 | `el-select` | `market` | 来自项目 `target_markets_json` |
| 探针模式 | `el-select` | `probeMode` | grounded-api / browser-extension / headless |
| 创建时间 | `el-date-picker` daterange | `createdAt` | 与 user 列表页一致 |

---

## 表格列

| 列 | 宽度 | 来源字段 | 展示 |
|----|------|----------|------|
| 选择 | 50 | — | 批量导出预留 |
| 任务名称 | min 180 | `name` | `show-overflow-tooltip`；可点击进入详情 |
| 市场/语言 | 120 | `market`, `locale` | `US` + `el-tag size="small"` en-US |
| 探针模式 | 140 | `probe_modes_json` | 多值 `el-tag`；默认 grounded-api 加 `type="success"` |
| AI 平台 | 140 | `models_json` | 逗号分隔或 Tag 组（Perplexity/Gemini/OpenAI） |
| 采样次数 | 80 | `sample_count` | 居中数字 |
| 状态 | 100 | `status` | Tag 映射（见 tokens.md） |
| 进度 | 120 | 计算 | RUNNING 时 `el-progress`；否则 `—` |
| GEO 分数 | 90 | `geo_score` | 完成态显示，色阶 `--tg-score-*`；未完成 `—` |
| 创建时间 | 160 | `created_at` | `YYYY-MM-DD HH:mm` |
| 操作 | fixed right 200 | — | 见下表 |

### 行操作

| 状态 | 操作 |
|------|------|
| PENDING / RUNNING | 查看进度、取消（`el-popconfirm`） |
| SUCCESS / PARTIAL_FAILED | 查看结果、导出报告 |
| FAILED | 查看日志、重试 |
| 全部 | 复制 runId（mono 字体 tooltip） |

组件：`el-button link type="primary"` + `el-tooltip`。

---

## 新建诊断任务（抽屉 / 对话框）

**触发**：工具栏「新建诊断任务」`el-button type="primary" icon="Plus"`

**表单**（`el-drawer` size="480px" 或 `el-dialog`）：

| 字段 | 组件 | 必填 | 说明 |
|------|------|:----:|------|
| 任务名称 | `el-input` | ✅ | 默认「{项目名}-{日期}诊断」 |
| 目标市场 | `el-select` | ✅ | `market` |
| 语言/地区 | `el-select` | ✅ | `locale`, `region` |
| 问题范围 | `el-radio-group` | ✅ | 全部 / 按阶段 / 自定义数量（`question_scope_json`） |
| 探针模式 | `el-checkbox-group` | ✅ | 默认仅 **grounded-api** 勾选且置顶 |
| AI 平台 | `el-checkbox-group` | ✅ | Perplexity、Gemini、OpenAI（来自租户配置） |
| 采样次数 | `el-input-number` | ✅ | min 1 max 10，默认 3 |
| 校准比例 | `el-slider` | — | `calibration_ratio` 0–30%，附说明「扩展探针校准」 |

**合规提示**（抽屉底部 `el-alert type="info" show-icon`）：

> GEO 诊断使用联网检索采样结果，不承诺排名保证。报告将标注 probe_mode、sampled_at、参与平台。

**提交**：POST `/api/v1/projects/{id}/diagnostics` → 202 → 列表刷新 + toast「任务已创建」

---

## 空 / 错 / 加载

| 状态 | UI |
|------|-----|
| 加载 | `el-table v-loading` |
| 无数据 | `el-empty`：「暂无诊断任务」+ 主按钮「创建首次 GEO 诊断」 |
| 无项目 | 与 dashboard 一致，引导选项目或创建 |
| 部分失败行 | 状态 Tag `warning`；tooltip 展示失败子任务数（API 字段预留） |

---

## 合规与文案（固定）

- 页脚或列表上方小字：`诊断结果基于采样时刻的 AI 回答，仅供参考。`
- grounded-api 任务不得展示「缓存结果」类文案（PRD：GEO 不缓存）

---

## 响应式

- &lt;768px：隐藏「AI 平台」「采样次数」列；操作收起到 `el-dropdown`「更多」
- 搜索区默认折叠（`right-toolbar` 与 user 列表一致）

---

## 菜单配置（若依）

```text
父菜单：GEO 诊断 (path: /diagnostics, icon: chart)
  └─ 诊断任务 (path: /diagnostics/runs, component: tourgeo/diagnostics/index)
  └─ 问题库 (path: /diagnostics/questions, 占位 disabled)
  └─ 探针节点 (path: /diagnostics/probe-nodes, 占位 EPIC-11)
```

---

## 关联 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/projects/{projectId}/diagnostics` | 分页列表 |
| POST | `/api/v1/projects/{projectId}/diagnostics` | 创建任务 202 |
| GET | `/api/v1/diagnostics/{runId}` | 详情/进度 |
| POST | `/api/v1/diagnostics/{runId}/cancel` | 取消（若后端提供） |
| GET | `/api/v1/diagnostics/{runId}/report` | 导出 |

---

## 实现参考

复制 plus-ui 列表模式：`inbound-admin/src/views/system/user/index.vue`

- 搜索卡片 + `transition`
- `el-card` header 工具栏 + `right-toolbar`
- `pagination` 组件（项目内已有）

---

## M3 增量 · 定时计划 Tab（FR-109）

> **PRD**：FR-109 按周/月自动发起诊断 · **ADR-20260710-26**  
> **前置**：M1 诊断列表 + 新建 drawer ✅ · `DiagnosticRunServiceImpl.createRun` ✅  
> **关联 Java**：`GET/PUT .../diagnostics/schedule` · `DiagnosticScheduleJob` 每小时 due 检查

**M3 范围（本页）**：
- ✅ 页级 **`el-tabs`** — **「诊断任务」**（默认 · M1 列表不变）| **「定时计划」**
- ✅ 定时计划表单 — `enabled` · 频率 `WEEKLY`/`MONTHLY` · 复用新建 drawer 诊断参数字段
- ✅ 只读区 — 下次执行 · 最近触发 · 最近 run 链详情
- ✅ footnote — 超额跳过 · 不承诺排名 · 无通知
- ❌ 邮件/企微通知 · XXL-Job · 自由 cron · 每项目多计划 · 诊断结果缓存

**约束**（ADR-26）：
- 每项目 **仅 1 条** `diagnostic_schedule`（upsert）
- 触发时复用 `createRun` · 任务名默认 `{项目名}-定时-{YYYY-MM-DD}`
- 额度不足：**skip** + warn 日志 · **不创建 run** · UI 不弹错（footnote 说明）

**权限**：
- 查看计划：`tourgeo:diagnostic:view`（与列表一致）
- 保存计划：`tourgeo:diagnostic:edit`（与「新建诊断任务」一致）
- 只读角色：表单 disabled · 隐藏「保存计划」

---

### M3 页级 Tab 结构

**位置**：`tg-page-header` 与合规 hint **下方** · 列表/搜索 **上方**。

```
┌─ el-tabs v-model="activePageTab" ─────────────────────────────────────┐
│ [诊断任务]  [定时计划]                                                  │
└─────────────────────────────────────────────────────────────────────────┘

activePageTab === 'runs'     → 现有 M1 搜索区 + 表格 + 新建 drawer（不变）
activePageTab === 'schedule' → 下方「定时计划」单卡表单（本节）
```

**路由**（P2 可选）：`?tab=schedule` 深链定时 Tab；M3 可仅内存切换。

**副标题**（定时 Tab 内 `el-page-header` 或 card 标题下）：
> 按周或按月自动创建 GEO 诊断任务；到点由系统触发，无需手工新建。

---

### M3 布局增量（ASCII）

```
定时计划 Tab (el-card shadow="hover"):
┌─ 定时 GEO 诊断 ────────────────────────────────────────────────────────┐
│ 启用定时计划    [el-switch v-model="enabled"]  关闭后 Job 不触发          │
│ ── 执行频率 (FR-109) ──                                                 │
│ ( ) 每周 WEEKLY    ( ) 每月 MONTHLY   el-radio-group                    │
│   周：自上次成功触发起每 7 日 · 月：每自然月同日（无 31 日则月末）         │
│ ── 诊断参数（与「新建诊断任务」一致，不含任务名称） ──                    │
│ 目标市场*       [US ▼]     来自 project target_markets                 │
│ 语言/地区*      [en-US ▼]                                               │
│ 问题范围*       ( )全部  ( )按阶段  ( )自定义数量                         │
│ 探针模式*       [✓] grounded-api  [ ] browser-extension  [ ] headless   │
│ AI 平台*        [✓] Perplexity  [✓] Gemini  [ ] OpenAI                  │
│ 采样次数*       [ 3 ]  el-input-number min=1 max=10                     │
│ 校准比例        [====●====] 0–30%  （须勾选 browser-extension）          │
│ ── 执行状态（只读 · GET schedule 响应） ──                               │
│ 下次执行        2026-07-17 02:00   或 enabled=false 时「—」              │
│ 最近触发        2026-07-10 02:00   从未触发「—」                         │
│ 最近任务        [#42 Q2-US-定时-2026-07-10]  link → /diagnostics/42       │
│                 无 lastRunId 时「暂无自动任务」                           │
│ ℹ GEO 诊断使用联网检索采样，不承诺排名保证（同新建 drawer alert）          │
│ footnote 卡（见下）                                                      │
│                              [保存计划] primary  （diagnostic:edit）     │
└──────────────────────────────────────────────────────────────────────────┘

未配置过 schedule（GET 404 或空默认）:
  enabled=false · frequency=WEEKLY · 字段套用与新建 drawer 相同默认值
  提示 el-alert info「尚未保存定时计划，配置后点击保存。」

保存成功:
  ElMessage.success「定时计划已保存」
  刷新 nextRunAt / lastTriggeredAt / lastRunId
```

---

### 定时计划表单字段

| 字段 | 组件 | 必填 | API 字段 | 说明 |
|------|------|:----:|----------|------|
| 启用 | `el-switch` | — | `enabled` | 默认 `false`；关闭仍允许保存参数 |
| 执行频率 | `el-radio-group` | ✅ | `frequency` | `WEEKLY` \| `MONTHLY` |
| 目标市场 | `el-select` | ✅ | `market` | 与新建 drawer 同源 `marketOptions` |
| 语言/地区 | `el-select` | ✅ | `locale` | en-US / en-GB / en-AU |
| 问题范围 | `el-radio-group` | ✅ | `questionScope` → `question_scope_json` | all / stage / custom |
| 探针模式 | `el-checkbox-group` | ✅ | `probeModes` → `probe_modes_json` | grounded-api 默认勾选 |
| AI 平台 | `el-checkbox-group` | ✅ | `models` → `models_json` | 至少 1 项 |
| 采样次数 | `el-input-number` | ✅ | `sampleCount` | 1–10 · 默认 3 |
| 校准比例 | `el-slider` | — | `calibrationRatio` | 0–30% · 同 drawer 联动 browser-extension |

**只读展示**（`el-descriptions` :column="1" border）：

| UI 标签 | API 字段 | 展示 |
|---------|----------|------|
| 下次执行 | `nextRunAt` | `YYYY-MM-DD HH:mm` · `enabled=false` 显示「已暂停」 |
| 最近触发 | `lastTriggeredAt` | 时间或 `—` |
| 最近任务 | `lastRunId` + `lastRunName`（可选） | `el-link` → `/diagnostics/runs/:runId` 或项目内详情路由 · 无 run「暂无自动任务」 |

---

### 交互与校验

| 动作 | 行为 |
|------|------|
| 切换 Tab | 进入「定时计划」时 `GET .../diagnostics/schedule` · loading skeleton |
| 保存 | `PUT .../diagnostics/schedule` 全量 upsert · `:loading` on 按钮 |
| 校验失败 | 与新建 drawer 相同（市场/平台/采样必填） |
| `enabled=false` | 表单可编辑 · 只读区「下次执行」显示「已暂停」 |
| 最近任务链接 | 新 Tab 或 `router.push` 到已有 `detail.vue` |
| 项目切换 | 重置 Tab 到「诊断任务」或保持 Tab 但 reload schedule |

**与新建 drawer 差异**：

| 项 | 新建 drawer | 定时计划 |
|----|-------------|----------|
| 任务名称 | 用户输入 | **无** · Job 自动生成 |
| 提交 API | `POST .../diagnostics` 202 | `PUT .../schedule` 200 |
| 立即执行 | 是 | 否 · 等 `nextRunAt` |

---

### M3 合规 footnote（`el-card shadow="never"` · 表单下方）

```
┌─ 定时执行说明 ─────────────────────────────────────────────────────────┐
│ · 系统 **每小时** 检查到期计划（Spring `@Scheduled`）；到点自动创建诊断任务。 │
│ · **每周**：自上次成功触发起间隔 7 日；**每月**：每自然月相同日期（无该日则月末）。 │
│ · 若当月 **GEO 诊断额度不足**，本次触发 **跳过** 并记录日志，不创建任务。     │
│ · 诊断结果为采样时刻 AI 回答，**不承诺排名保证**；M3 **不发送** 邮件/企微通知。 │
│ · 每项目仅支持 **一条** 定时计划；如需变更参数请编辑后保存。                  │
└────────────────────────────────────────────────────────────────────────┘
```

页顶既有合规 hint（M1）在 Tab 外 **保留**；定时 Tab 内重复 info alert（与新建 drawer 同文案）即可。

---

### M3 关联 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/projects/{projectId}/diagnostics/schedule` | 单条计划 · 无记录可 404 + 前端默认空表单 |
| PUT | `/api/v1/projects/{projectId}/diagnostics/schedule` | upsert · 返回完整 Vo 含 `nextRunAt` |

**响应 Vo 建议**（`DiagnosticScheduleVo`）：

```typescript
{
  enabled: boolean
  frequency: 'WEEKLY' | 'MONTHLY'
  market: string
  locale: string
  questionScope: string
  probeModes: string[]
  models: string[]
  sampleCount: number
  calibrationRatio: number
  nextRunAt: string | null
  lastTriggeredAt: string | null
  lastRunId: number | null
  lastRunName?: string
}
```

---

### M3 实现参考

- Tab 容器：参考 [keywords-list.md](./keywords-list.md) 页内 `el-tabs` · 或 [project-detail.md](./project-detail.md) border-card
- 表单字段：**直接复用** `diagnostics/index.vue` 新建 drawer 表单项（抽 `DiagnosticRunParamsForm` 组件 P2）
- 只读区：`el-descriptions` · 最近任务 `el-link type="primary"`
- 文件：`inbound-admin/src/views/tourgeo/diagnostics/index.vue` · `api/tourgeo/diagnostic.ts`

---

### M3 范围边界

| 包含 | 不包含 |
|------|--------|
| 页级「定时计划」Tab + 单条 upsert 表单 | FR-109「到点通知」邮件/企微 |
| enabled + WEEKLY/MONTHLY | 自定义 cron / XXL-Job UI |
| 下次执行 / 最近 run 链接 | 多计划列表 / 计划历史 |
| 超额 skip footnote | 超额 Admin 内告警条（仅日志） |
| 复用探针/平台/采样参数 | 定时任务单独缓存 GEO 结果 |

---

## 版本

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-10 | UI 设计 | **§M3** 定时计划 Tab FR-109 · enabled/频率/参数复用 · nextRun/lastRun · ADR-26 footnote |
| 2026-06-26 | UI 设计 | M1 初版 · FR-103 列表 + 新建 drawer |
