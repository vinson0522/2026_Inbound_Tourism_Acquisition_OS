# 线框：落地页 Agent · 页面列表（EPIC-6 M1）

> **PRD**：§6.1 落地页 Agent · **FR-501~505** · §20.3 模块结构 · FR-205 关键词转化  
> **EPIC**：EPIC-6 M1  
> **路由**：`/projects/:projectId/landing-pages`（侧栏 `/landing-pages` 重定向当前项目）  
> **数据表**：`landing_page`、`keyword_opportunity`（join）

---

## 页面目标

管理项目下 **英文落地页草稿**：列表浏览、按关键词/模板创建、**AI 生成**页面结构与文案（FR-502~504）、**预览**模块与 SEO 元数据；M1 不含在线发布（FR-507 P3）与可视化编辑器（M2）。

**入口**：
- 侧栏「落地页 Agent」→ 页面草稿
- [keywords-list.md](./keywords-list.md) 行操作「转落地页」（FR-205 P1）→ `?action=create&keywordId=`
- [content-task-list.md](./content-task-list.md) 行操作「生成落地页」/`landing_page_suggestion`（P1）→ 带 `keywordId` + 可选 `contentTaskId`

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 面包屑：落地页 Agent / 页面草稿      当前项目 ▼ Dragon Journey Travel       │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─ 项目选择器 (同 keywords-list) ──────────────────────────────────────────┐ │
│ │ 客户项目 [Dragon Journey ▼]   目标市场 US UK AU                          │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 搜索区 (el-card, 可折叠) ───────────────────────────────────────────────┐ │
│ │ 标题 [____]  slug [____]  模板 [全部▼]  状态 [全部▼]  关键词 [____]      │ │
│ │ 市场 [全部▼]  更新时间 [日期范围]                  [搜索] [重置]          │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 工具栏 ─────────────────────────────────────────────────────────────────┐ │
│ │ [新建页面] [从关键词创建 ▼]                    [显示搜索] [刷新]            │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ el-table border ────────────────────────────────────────────────────────┐ │
│ │ 页面标题 | slug | 模板 | 关联关键词 | 市场 | 状态 | 更新时间 | 操作      │ │
│ │ Chongqing Cyberpunk Tour | chongqing-cyber… | 主题游 | Chongqing… | US | 草稿│ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                              [分页 10/20/50]                                │
└─────────────────────────────────────────────────────────────────────────────┘

创建 (el-dialog 560px):
┌─ 新建落地页 ───────────────────────────────────────── [×] │
│ 页面标题*     [Chongqing Cyberpunk City Tour]              │
│ URL Slug*     [/ chongqing-cyberpunk-tour    ]  ✓ 可用     │
│ 页面模板*     [主题游 themed_tour ▼]  (FR-501)             │
│ 关联关键词*   [Chongqing cyberpunk city tour ▼]            │
│ 目标市场      [US ▼]  (默认 keyword.market)                │
│ ☐ 创建后立即 AI 生成页面 (FR-502~504)                      │
│ [取消]  [仅创建]  [创建并 AI 生成]                         │
└─────────────────────────────────────────────────────────────┘

AI 生成中 (行内 + dialog 关闭后):
  行 status → EDITING Tag + el-progress indeterminate
  toast「AI 正在生成落地页，约 1–3 分钟…」

预览 (el-drawer 640px):
┌─ 预览 · Chongqing Cyberpunk Tour ─────────────────── [×] │
│ [草稿 Tag]  slug: /chongqing-cyberpunk-tour               │
│ ── 可读摘要 ──                                            │
│ H1: Discover Chongqing's Futuristic Skyline               │
│ Hero 副标题: Private tours for first-time visitors…       │
│ ── SEO / GEO (FR-504) el-descriptions ──                  │
│ Title | Meta Description | H1 | FAQ Schema 有/无          │
│ ── 页面模块 (FR-503 · content_json) el-collapse ──       │
│ ▼ Hero · Why This Trip · Itinerary · FAQ · Lead Form …   │
│ ── 表单 / WhatsApp (FR-505) ──                            │
│ 字段: 姓名/邮箱/电话/日期/人数/预算 · WhatsApp 链接       │
│ [关闭]  [编辑页面] M2  [导出 HTML] FR-506 disabled       │
└───────────────────────────────────────────────────────────┘
```

---

## 项目选择器

与 [keywords-list.md](./keywords-list.md) 一致；路由 `/projects/:projectId/landing-pages`。

---

## 筛选栏

| 字段 | 组件 | 参数 | 说明 |
|------|------|------|------|
| 标题 | `el-input` | `title` | 模糊 |
| Slug | `el-input` | `slug` | 模糊 |
| 模板 | `el-select` | `templateType` | FR-501 枚举 |
| 状态 | `el-select` | `status` | `landing_page_status` |
| 关键词 | `el-input` | `keyword` | join 模糊 |
| 市场 | `el-select` | `market` | join `keyword_opportunity.market` |
| 更新时间 | `el-date-picker` daterange | `updatedAt` | |

---

## 表格列 ↔ DDL / API（HANDOFF 必含列）

| 列 | 宽度 | DDL 列 | API 字段 | 展示 |
|----|------|--------|----------|------|
| 页面标题 | min 200 | `title` | `title` | 主文案；点击进入预览 drawer |
| Slug | 160 | `slug` | `slug` | mono `/slug`；`show-overflow-tooltip` |
| 模板 | 110 | `template_type` | `templateType` | Tag 中文 |
| 关联关键词 | min 180 | `keyword_id` → join | `keywordText` | 无则 `—` |
| 市场 | 70 | join `keyword.market` | `market` | Tag（无 keyword 时用创建时 market 扩展字段 P2） |
| 状态 | 100 | `status` | `status` | Tag 见下 |
| 更新时间 | 160 | `updated_at` | `updatedAt` | `YYYY-MM-DD HH:mm` |
| 操作 | fixed 200 | — | — | 见行操作 |

可选隐藏列：`published_url`（PUBLISHED 时）、`published_at`（M2）。

### `landing_page_status` Tag

| DB 值 | Tag type | 中文 |
|-------|----------|------|
| `DRAFT` | `info` | 草稿 |
| `EDITING` | `primary` | 生成/编辑中 |
| `READY` | `success` | 待发布 |
| `PUBLISHED` | `success` | 已发布 |
| `ARCHIVED` | `info` | 已归档 |

`EDITING` 行显示 loading 动画（AI 生成中）。

### `template_type`（FR-501）

| 值 | 中文 |
|----|------|
| `destination` | 目的地页 |
| `route` | 路线页 |
| `themed_tour` | 主题游 |
| `visa_policy` | 签证政策 |
| `event` | 活动页 |
| `default` | 默认 |

---

## 行操作（M1）

| 操作 | 条件 | 行为 |
|------|------|------|
| 预览 | 有 `content_json` | 打开预览 drawer |
| AI 生成 | DRAFT / FAILED | `POST .../generate` + 行 loading |
| 编辑 | READY+ | M2 → `/landing-pages/:id/edit` disabled + tooltip |
| 复制 slug | 全部 | clipboard |
| 删除 | 非 PUBLISHED | soft delete confirm |

---

## 创建弹窗（el-dialog）

| UI 标签 | DDL 列 | API | 必填 | 说明 |
|---------|--------|-----|:----:|------|
| 页面标题 | `title` | `title` | ✅ | max 500；影响默认 slug 建议 |
| URL Slug | `slug` | `slug` | ✅ | 见 slug 校验 |
| 页面模板 | `template_type` | `templateType` | ✅ | FR-501 |
| 关联关键词 | `keyword_id` | `keywordId` | ✅ M1 | 下拉 ACTIVE 关键词 |
| 目标市场 | —（UI） | `market` | — | 写入请求体；默认 `keyword.market`；列表展示用 |

**按钮**：
- **仅创建** → `POST` → `status=DRAFT` → 关闭 dialog → 刷新列表
- **创建并 AI 生成** → `POST` 后链式 `POST .../generate` → 行 `EDITING`

### Slug 校验（实时）

| 规则 | UI |
|------|-----|
| 格式 | 小写 a-z、0-9、连字符；2–200 字 |
| 唯一 | 项目内 `uq_landing_page_project_slug` |
| 组件 | `el-input` + 失焦/防抖 `GET .../landing-pages/check-slug?slug=` |
| 可用 | 绿色 `✓ 可用` |
| 冲突 | 红色「Slug 已存在」 |
| 自动建议 | 标题 blur → kebab-case 预填 |

示例：`Chongqing Cyberpunk Tour` → `chongqing-cyberpunk-tour`

---

## 「AI 生成页面」（FR-502~504）

| 步骤 | UI | API |
|------|-----|-----|
| 触发 | 创建并生成 / 行操作「AI 生成」 | `POST /api/v1/projects/{id}/landing-pages/{pageId}/generate` |
| 进行中 | 行 `EDITING` + progress；禁止重复点击 | 202 异步 |
| 成功 | `READY`；toast「页面已生成，请预览并人工确认」 | 刷新行 |
| 失败 | `DRAFT` 或保持 FAILED Tag；展示 error | |

**Loading 文案**：`AI 正在生成英文文案与页面结构，约 1–3 分钟…`（`--tg-color-running`）

**合规**（dialog / 预览底）：生成内容默认 `needs_human_review` 等价提示 — 价格/签证/政策需人工确认（PRD 合规）。

---

## 预览 Drawer（FR-503 / §20.3）

**触发**：行点击标题或「预览」；宽度 `640px` `destroy-on-close`。

### 区块 1：可读摘要

从 `content_json` + `seo_meta_json` 提取（非 JSON 裸展示优先）：

| 项 | 来源 |
|----|------|
| 页面 H1 | `seo_meta_json.h1` 或 `content_json.modules[hero].heading` |
| Hero 副标题 | `content_json` hero subtitle |
| 模块数量 | `content_json.modules.length` |

### 区块 2：SEO / GEO 元数据（FR-504）

`el-descriptions` `:column="1"` border size="small"

| 标签 | `seo_meta_json` 键 |
|------|-------------------|
| Title | `title` |
| Meta Description | `description` |
| H1 | `h1` |
| FAQ Schema | `faq_schema` 有/无 + 展开 JSON（developer mode `el-collapse`） |
| 结构化建议 | `structured_data_hints` 文本列表 |

### 区块 3：页面模块 `el-collapse`（§20.3）

`content_json` 建议结构（API 契约，开发对齐 Python Agent）：

```json
{
  "modules": [
    { "type": "hero", "heading": "...", "subheading": "...", "cta": "..." },
    { "type": "why_this_trip", "body": "..." },
    { "type": "itinerary", "days": [...] },
    { "type": "what_we_provide", "items": [...] },
    { "type": "reviews", "items": [...] },
    { "type": "faq", "items": [{ "q": "", "a": "" }] },
    { "type": "lead_form", "ref": "form_config" },
    { "type": "whatsapp_cta", "label": "..." }
  ]
}
```

| `type` | §20.3 中文 | Collapse 标题展示 |
|--------|-----------|-------------------|
| `hero` | Hero | 标题 + 副标题前 80 字 |
| `why_this_trip` | Why This Trip | 正文 truncate |
| `itinerary` | Itinerary | 天数 + 城市列表 |
| `what_we_provide` | What We Provide | 条目数 |
| `reviews` | Traveler Reviews | 评价条数 |
| `faq` | FAQ | 问题数 |
| `lead_form` | Lead Form | 「见表单配置」 |
| `whatsapp_cta` | WhatsApp CTA | 按钮文案 |

每项展开：关键字段只读 `el-text`；底部可选「查看 JSON」`el-collapse-item`（调试，小字 mono）。

空 `content_json`：`el-empty`「尚未生成内容，请先 AI 生成」+ 按钮触发 generate。

### 区块 4：表单与 WhatsApp（FR-505）

| 字段 | `form_config_json` |
|------|---------------------|
| 启用字段 | `fields[]`: `name`,`email`,`phone`,`travel_date`,`pax`,`budget`,`notes` |
| WhatsApp | `landing_page.whatsapp_link` 或 `form_config_json.whatsapp_url` |

展示为 Tag 列表 + WhatsApp `el-link`。

---

## 字段 ↔ DDL 完整对照

| UI | DDL 列 | API camelCase | 列表 | 创建 |
|----|--------|---------------|:----:|:----:|
| ID | `id` | `id` | — | — |
| 标题 | `title` | `title` | ✅ | ✅ |
| Slug | `slug` | `slug` | ✅ | ✅ |
| 模板 | `template_type` | `templateType` | ✅ | ✅ |
| 关键词 | `keyword_id` | `keywordId` | join | ✅ |
| 内容 | `content_json` | `contentJson` | 预览 | AI |
| SEO | `seo_meta_json` | `seoMeta` | 预览 | AI |
| 表单 | `form_config_json` | `formConfig` | 预览 | AI/默认 |
| WhatsApp | `whatsapp_link` | `whatsappLink` | 预览 | M2 编辑 |
| 状态 | `status` | `status` | ✅ | auto |
| 发布 URL | `published_url` | `publishedUrl` | P2 | — |
| 更新时间 | `updated_at` | `updatedAt` | ✅ | — |

---

## 从关键词 / 内容页入口（FR-205）

### 关键词列表

```
/projects/{projectId}/landing-pages?action=create&keywordId={id}
```

`onMounted` 打开创建 dialog 并预填 keyword + title/slug 建议 + market。

### 内容任务（P1）

```
?action=create&keywordId={id}&contentTaskId={id}
```

标题可预填 `generated_content.title` 或 `landing_page_suggestion` 摘要。

---

## 空 / 加载 / 错误

| 状态 | UI |
|------|-----|
| 加载 | `v-loading` |
| 无页面 | `el-empty` +「新建页面」「从关键词创建」 |
| slug 冲突 | 创建按钮 disabled |
| 生成失败 | 行 FAILED 态 + `ElMessage.error` |
| 无项目 | → `/projects` |

---

## 响应式

| 断点 | 行为 |
|------|------|
| ≥1200px | 全列 |
| 768–1199px | 隐藏 market |
| &lt;768px | 标题、slug、状态、操作；preview drawer 100% 宽 |

---

## 菜单与路由

```text
父菜单：落地页 Agent (path: /landing, icon: document 或 link)
  └─ 页面草稿 (path: /projects/:projectId/landing-pages, component: tourgeo/landing/index)
  └─ FAQ / 表单配置 / 发布记录 — M2+ placeholder disabled
```

---

## 关联 API（EPIC-6 M1）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/projects/{projectId}/landing-pages` | 分页列表 |
| POST | `/api/v1/projects/{projectId}/landing-pages` | 创建 |
| GET | `/api/v1/projects/{projectId}/landing-pages/check-slug` | slug 唯一性 |
| GET | `/api/v1/projects/{projectId}/landing-pages/{id}` | 预览详情 |
| POST | `/api/v1/projects/{projectId}/landing-pages/{id}/generate` | AI 生成 FR-502~505 |
| DELETE | `/api/v1/projects/{projectId}/landing-pages/{id}` | 软删 |

Python：`POST /ai/landing`（Java Feign，LangGraph + RAG）。

---

## M1 范围

| 包含 | 不包含 |
|------|--------|
| 列表 + 创建 dialog + slug 校验 | GrapesJS/TipTap 可视化编辑（M2） |
| AI 生成 + loading | FR-507 在线发布 |
| 预览 drawer（摘要 + collapse + SEO） | FR-508 A/B |
| FR-205 关键词入口 | FR-506 导出 HTML（按钮 disabled） |

---

## 实现参考

- 列表：[content-task-list.md](./content-task-list.md)、`tourgeo/keywords/index.vue`
- Dialog 创建：[projects-list.md](./projects-list.md) 抽屉字段密度
- Preview collapse：[diagnostic-detail.md](./diagnostic-detail.md) Tab 展开
- Token：[tokens.md](../tokens.md)

---

## 版本

| 日期 | 说明 |
|------|------|
| 2026-07-01 | EPIC-6 M1 列表 + 创建 + AI 生成 + 预览线框 |
