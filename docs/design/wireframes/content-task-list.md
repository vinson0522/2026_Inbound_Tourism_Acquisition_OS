# 线框：内容 Agent · 内容任务列表（EPIC-4 M1）

> **PRD**：§6.1 内容 Agent · **FR-205** 内容任务转化 · **FR-301~303**（M1 列表 + 创建；生成详情 M2）  
> **EPIC**：EPIC-4 M1（预研线框，待技术总监 HANDOFF 定稿后可微调）  
> **路由**：`/projects/:projectId/content/tasks`（侧栏 `/content/tasks` 重定向到当前项目）  
> **数据表**：`content_task`、`generated_content`（join 最新版本）、`keyword_opportunity`

---

## 页面目标

在项目上下文中管理 **社媒内容生产任务**：展示关联关键词、目标平台、内容格式、任务状态与 **人工审核**（`needs_human_review`）；支持从 **关键词页一键转入**（FR-205）及本页创建空任务。

**入口**：
- 侧栏「内容 Agent」→ 内容任务
- [keywords-list.md](./keywords-list.md) 行操作「转内容任务」→ 本页 + 打开创建抽屉（`?keywordId=`）
- 工作台「今日任务 · 待审核脚本」→ 本页筛选 `needsHumanReview=true`（P2）

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 面包屑：内容 Agent / 内容任务        当前项目 ▼ Dragon Journey Travel       │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─ 项目选择器 (同 keywords-list) ──────────────────────────────────────────┐ │
│ │ 客户项目 [Dragon Journey ▼]   目标市场 US UK AU                          │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 搜索区 (el-card, 可折叠) ───────────────────────────────────────────────┐ │
│ │ 关键词 [____]  平台 [全部▼]  格式 [全部▼]  任务状态 [全部▼]              │ │
│ │ 人工审核 [全部▼]  创建时间 [日期范围]              [搜索] [重置]          │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 工具栏 ─────────────────────────────────────────────────────────────────┐ │
│ │ [新建任务] [从关键词创建 ▼]              [显示搜索] [刷新]                │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ el-table border ────────────────────────────────────────────────────────┐ │
│ │ 关联关键词 | 平台 | 格式 | 时长 | 语气 | 市场 | 任务状态 | 人工审核 | … │ │
│ │ Chongqing cyberpunk… | TikTok | 短视频 | 30s | 亲切 | US | 已生成 | 待审核│ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                              [分页 10/20/50]                                │
└─────────────────────────────────────────────────────────────────────────────┘

创建抽屉 (el-drawer 560px) — 从关键词预填或空白新建:
┌─ 创建内容任务 ─────────────────────────────────────── [×] │
│ 关联关键词*  [Chongqing cyberpunk city tour ▼]  [跳转关键词页]│
│ 目标平台*    [TikTok ▼]                                    │
│ 内容格式*    [短视频 short_video ▼]                          │
│ 视频时长     [30] 秒  (15/30/60 快捷)                        │
│ 品牌语气     [亲切 friendly ▼]  (FR-306 P1 可扩展)         │
│ 目标市场     [US ▼]  (默认 keyword.market)                 │
│ 语言         [en ▼]  (默认项目 languages)                    │
│ ℹ 创建后将进入「草稿」，可在详情页触发 AI 脚本生成 (FR-302)。 │
│                              [取消]  [创建任务]              │
└─────────────────────────────────────────────────────────────┘
```

---

## 项目选择器

与 [keywords-list.md](./keywords-list.md) **一致**：

| 项 | 规范 |
|----|------|
| 路由 | `/projects/:projectId/content/tasks` |
| 切换 | `router.replace({ name: 'ContentTaskList', params: { projectId } })` |
| 无项目 | `el-empty` → `/projects` |

---

## 筛选栏

| 字段 | 组件 | 查询参数 | 说明 |
|------|------|----------|------|
| 关键词 | `el-input` | `keyword` | 模糊匹配关联词 `keyword` / `keyword_en` |
| 平台 | `el-select` | `platform` | TikTok / Instagram / YouTube / … |
| 格式 | `el-select` | `format` | 见下表 |
| 任务状态 | `el-select` | `status` | `content_task_status` |
| 人工审核 | `el-select` | `needsHumanReview` | `true` / `false` / 全部（仅 GENERATED+ 有意义） |
| 创建时间 | `el-date-picker` daterange | `createdAt` | |

---

## 表格列 ↔ DDL / API

| 列 | 宽度 | 来源 | API 字段 | 展示 |
|----|------|------|----------|------|
| 任务 ID | 80 | `content_task.id` | `id` | mono 小字；默认隐藏列 optional |
| 关联关键词 | min 200 | `keyword_opportunity` join | `keywordText` / `keywordId` | 主链接；无 keyword 显示「—（手动任务）」 |
| 平台 | 100 | `platform` | `platform` | `el-tag` |
| 格式 | 100 | `format` | `format` | Tag 中文映射 |
| 时长 | 70 | `duration` | `duration` | `{n}s` 或 `—` |
| 语气 | 90 | `tone` | `tone` | 中文映射 FR-306 |
| 市场 | 70 | `target_market` | `targetMarket` | Tag |
| 语言 | 60 | `language` | `language` | |
| 任务状态 | 100 | `status` | `status` | Tag 见下 |
| **人工审核** | 100 | `generated_content.needs_human_review`（最新 `version`） | `needsHumanReview` | **见下** |
| 内容标题 | min 160 | `generated_content.title` | `contentTitle` | GENERATED+ 有值；否则 `—` |
| 创建时间 | 160 | `created_at` | `createdAt` | |
| 操作 | fixed 180 | — | — | 见行操作 |

### `content_task_status` Tag

| DB 值 | Tag type | 中文 |
|-------|----------|------|
| `DRAFT` | `info` | 草稿 |
| `GENERATING` | `primary` | 生成中 |
| `GENERATED` | `success` | 已生成 |
| `ADOPTED` | `success` | 已采纳 |
| `DISCARDED` | `info` | 已废弃 |
| `FAILED` | `danger` | 失败 |

GENERATING 行可选 `el-progress` indeterminate（与关键词 AI 生成 loading 一致）。

### `needs_human_review` 列（M1 重点）

数据来自 **最新一条** `generated_content`（`MAX(version)` per `task_id`）。

| 条件 | Tag | 文案 | 色 |
|------|-----|------|-----|
| 无 generated_content | — | `—` | 灰字 |
| `needs_human_review=true` | `warning` | **待审核** | `--tg-color-warning` |
| `needs_human_review=false` | `success` | 已通过 | `--tg-score-high` |
| 任务 `DISCARDED` | `info` | 不适用 | |

**WCAG**：不仅靠颜色，Tag 内必须带中文文案。

Tooltip（待审核）：「AI 生成内容默认需人工确认价格/签证/政策类信息（PRD 合规）」。

### 格式 `format` 中文

| 值 | 中文 |
|----|------|
| `short_video` | 短视频 |
| `carousel` | 图文轮播 |
| `long_video` | 长视频 |
| `story` | Stories |

### 平台 `platform` 常用值

`TikTok` · `Instagram` · `YouTube` · `Facebook` · `X` · `RedNote`（存储英文 key，UI 中文可选）

### 语气 `tone`（FR-306 占位）

| 值 | 中文 |
|----|------|
| `premium` | 高端 |
| `friendly` | 亲切 |
| `youthful` | 年轻化 |
| `official` | 官方 |
| `family` | 家庭友好 |

---

## 行操作（M1）

| 状态 | 操作 |
|------|------|
| DRAFT | **生成脚本**（FR-302，M1 可 disabled + tooltip）、编辑、删除 |
| GENERATING | 查看进度（disabled 刷新） |
| GENERATED / ADOPTED | **查看内容**（→ 详情页 M2 `/content/tasks/:id`）、标记已采纳 |
| FAILED | 重试生成、删除 |
| 全部 | 复制 taskId |

M1 最小：**查看内容** 可链到占位页或 drawer 只读预览 `title` + `script` 前 200 字。

---

## 「从关键词创建」入口（FR-205）

### 路径 A：关键词列表跳转（主路径）

[keywords-list.md](./keywords-list.md) 行操作启用：

```
router.push({
  name: 'ContentTaskList',
  params: { projectId },
  query: { action: 'create', keywordId: row.id }
})
```

本页 `onMounted`：若 `query.action=create` && `keywordId` → 打开创建抽屉并预填关键词。

### 路径 B：本页「从关键词创建」

1. 点击工具栏 `el-dropdown`「从关键词创建」
2. 弹出 `el-dialog`：表格/下拉选择当前项目 `keyword_opportunity`（仅 `ACTIVE`）
3. 选中 → 打开同一创建抽屉

### 路径 C：「新建任务」

空白创建抽屉；关键词可选（不选则 `keyword_id` null，列表显示手动任务）。

---

## 创建抽屉字段 ↔ `content_task`

| UI 标签 | DDL 列 | API | 必填 | 默认 |
|---------|--------|-----|:----:|------|
| 关联关键词 | `keyword_id` | `keywordId` | — | 来自 query / 选择器 |
| 目标平台 | `platform` | `platform` | ✅ | TikTok |
| 内容格式 | `format` | `format` | ✅ | `short_video` |
| 视频时长 | `duration` | `duration` | — | 30 |
| 品牌语气 | `tone` | `tone` | — | `friendly` |
| 目标市场 | `target_market` | `targetMarket` | — | keyword.market |
| 语言 | `language` | `language` | — | `en` |

提交：`POST /api/v1/projects/{projectId}/content/tasks` → 201 → 列表刷新 → toast「任务已创建」→ 可选跳转详情（M2）。

创建后 `status=DRAFT`；触发 AI 生成另接口 `POST .../content/tasks/{id}/generate`（M2，FR-302）。

---

## 字段 ↔ API 列表响应（join 摘要）

```json
{
  "id": 1,
  "projectId": 1,
  "keywordId": 12,
  "keywordText": "Chongqing cyberpunk city tour",
  "keywordStage": "inspiration",
  "platform": "TikTok",
  "format": "short_video",
  "duration": 30,
  "tone": "friendly",
  "targetMarket": "US",
  "language": "en",
  "status": "GENERATED",
  "needsHumanReview": true,
  "contentTitle": "3 Reasons Chongqing Feels Like Blade Runner",
  "contentVersion": 1,
  "createdAt": "2026-06-29T10:00:00Z"
}
```

| UI | 表 | 说明 |
|----|-----|------|
| `needsHumanReview` | `generated_content` | 最新 version |
| `contentTitle` | `generated_content.title` | 可选 join |
| `keywordText` | `keyword_opportunity.keyword` | LEFT JOIN |

---

## 空 / 加载 / 错误态

| 状态 | UI |
|------|-----|
| 加载 | `v-loading` |
| 无任务 | `el-empty`「暂无内容任务」+「从关键词创建」+「新建任务」 |
| 筛选无结果 | empty +「清除筛选」 |
| 关键词页跳入但 keywordId 无效 | toast 警告 + 打开空白抽屉 |
| 402 额度 | 「内容生成额度不足」（P2 计费） |

---

## 与其他页面联动

| 场景 | 行为 |
|------|------|
| keywords-list | FR-205 启用「转内容任务」 |
| 内容详情 M2 | `/content/tasks/:taskId` 脚本/分镜编辑（FR-302/303） |
| 落地页 Agent | `landing_page_suggestion` 字段链 EPIC-6（P2） |
| 发布计划 | FR-305 独立菜单（M2+） |

---

## 响应式

| 断点 | 行为 |
|------|------|
| ≥1200px | 全列 |
| 768–1199px | 隐藏时长、语气、语言 |
| &lt;768px | 关键词、平台、状态、人工审核、操作；工具栏 dropdown 合并 |

---

## 菜单与路由

```text
父菜单：内容 Agent (path: /content, icon: edit 或 video-camera)
  └─ 内容任务 (path: /projects/:projectId/content/tasks, component: tourgeo/content/tasks/index)
  └─ 脚本生成 / 分镜 / 发布计划 — M2+ 占位 disabled
  └─ 爆款拆解 (/projects/:projectId/materials) — ✅ 见 [viral-breakdown-list.md](./viral-breakdown-list.md)
  └─ 素材标签库 — FR-404 disabled M2+
```

**重定向**：`/content/tasks` → 当前 `projectId` 或提示选项目。

**Query 约定**：`?action=create&keywordId=` · `?needsHumanReview=true`（工作台深链 P2）

---

## 关联 API（EPIC-4 M1 建议）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/projects/{projectId}/content/tasks` | 分页列表 + join keyword + latest content |
| POST | `/api/v1/projects/{projectId}/content/tasks` | 创建 FR-205 |
| DELETE | `/api/v1/projects/{projectId}/content/tasks/{id}` | 软删 |
| POST | `/api/v1/projects/{projectId}/content/tasks/{id}/generate` | FR-302 M2 |

Python 内网（M2）：`POST /ai/content`（LangGraph Agent，Java 代理）。

---

## M1 范围（预研）

| 包含 | 不包含 |
|------|--------|
| 列表 + 筛选 + 创建抽屉 | 脚本/分镜完整编辑器（FR-302/303） |
| FR-205 从关键词创建 | FR-301 批量选题生成 |
| `needs_human_review` 列展示 | 审核工作流（采纳/驳回 API M2） |
| 占位「查看内容」 | TipTap 编辑器、RAG chunk 展示 |

---

## 实现参考

- 列表模式：[keywords-list.md](./keywords-list.md)、`tourgeo/keywords/index.vue`
- Tag / 空态：[tokens.md](../tokens.md)
- 抽屉创建：[projects-list.md](./projects-list.md)、[diagnostics-list.md](./diagnostics-list.md)

---

## 版本

| 日期 | 说明 |
|------|------|
| 2026-06-29 | EPIC-4 M1 预研线框 · 待技术总监 HANDOFF 定稿 |
