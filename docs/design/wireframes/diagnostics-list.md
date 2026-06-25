# 线框：GEO 诊断 · 诊断任务列表

> **PRD**：§6.1 GEO 诊断 → 诊断任务 · FR-103 批量诊断任务  
> **路由**：`/diagnostics`（项目上下文下：`/projects/:projectId/diagnostics` 二选一，开发优先后者 REST 风格）  
> **数据表**：`diagnostic_run`（`001_schema.sql`）

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
