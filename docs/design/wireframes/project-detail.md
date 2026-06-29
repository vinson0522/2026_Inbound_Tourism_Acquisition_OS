# 线框：客户项目详情（Story 3）

> **PRD**：§6.1 客户项目 · FR-001 基础字段 · FR-002 竞品 · FR-003 产品路线 · FR-004 知识上传 · FR-005 向量化（状态展示）  
> **路由**：`/projects/:projectId`（隐藏路由，`activeMenu: '/projects/index'`）  
> **入口**： [projects-list.md](./projects-list.md) 行操作「进入 / 编辑」；顶栏当前项目切换后「项目设置」  
> **数据表**：`customer_project`、`travel_product`、`competitor`、`knowledge_asset`

---

## 页面目标

在单页 Tab 内维护项目的**品牌基础信息、主推路线、竞品名单、知识库资产**；与列表页创建流互补（列表=快速新建，详情=深度维护）。不阻塞 EPIC-10 embed——知识库 Tab 先完成上传与 `vector_status` 展示，语义检索预览占位 FR-005。

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ ← 返回列表   面包屑：客户项目 / First-Time China Private Tour               │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─ 项目头 (el-card) ─────────────────────────────────────────────────────┐ │
│ │ Dragon Journey Travel          [正常 Tag]    [设为当前项目] [进入工作台]  │ │
│ │ 项目：First-Time China Private Tour  |  US·UK·AU  |  en  |  更新于 …    │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ el-tabs (border-card 或默认) ───────────────────────────────────────────┐ │
│ │ [品牌信息] [竞品] [知识库]                                                │ │
│ │ ┌─ Tab: 品牌信息 ──────────────────────────────────────────────────────┐ │ │
│ │ │ el-form 两列：项目名称/品牌/官网/行业/市场/语言/状态  [保存]          │ │ │
│ │ │ ── 产品路线 (FR-003) ───────────────────────────────────────────────  │ │ │
│ │ │ [添加路线]  el-table: 路线名|目的地|天数|价格|操作                    │ │ │
│ │ └──────────────────────────────────────────────────────────────────────┘ │ │
│ │ ┌─ Tab: 竞品 ──────────────────────────────────────────────────────────┐ │ │
│ │ │ 已添加 3/5 提示  [添加竞品]  el-table + 抽屉表单                     │ │ │
│ │ └──────────────────────────────────────────────────────────────────────┘ │ │
│ │ ┌─ Tab: 知识库 ────────────────────────────────────────────────────────┐ │ │
│ │ │ [上传资料]  筛选：类型/向量化状态  el-table + 上传 dialog            │ │ │
│ │ └──────────────────────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 项目头：`customer_project` 摘要

| UI 元素 | 字段 | 组件 |
|---------|------|------|
| 主标题 | `brand_name` | `h1` |
| 副标题 | `name` | 灰色 secondary |
| 状态 | `status` | `el-tag`（同 [projects-list.md](./projects-list.md)） |
| 市场/语言 | `target_markets_json`, `languages_json` | Tag 组 |
| 更新时间 | `updated_at` | 小字 |
| 设为当前项目 | — | `el-button` plain → `projectStore.setCurrent(id)` |
| 进入工作台 | — | `el-button type="primary"` → `/dashboard` |

**返回**：`el-page-header @back` → `/projects/index`

---

## Tab 1：品牌信息

### 1.1 基础资料（`customer_project`）

`el-form` `label-width="110px"`，`el-row :gutter="20"` 两列布局（`:lg="12"`）。

| UI 标签 | DDL 列 | API | 组件 | 校验 |
|---------|--------|-----|------|------|
| 项目名称 | `name` | `name` | `el-input` | required, max 200 |
| 品牌名 | `brand_name` | `brandName` | `el-input` | required, max 200 |
| 官网 | `website` | `website` | `el-input` | URL 可选 |
| 行业 | `industry` | `industry` | `el-select` | 默认 `inbound_tourism` |
| 目标市场 | `target_markets_json` | `targetMarkets` | `el-select multiple` | min 1 |
| 服务语言 | `languages_json` | `languages` | `el-select multiple` | min 1 |
| 状态 | `status` | `status` | `el-select` | ACTIVE/INACTIVE/… |

底部：`el-button type="primary"`「保存品牌信息」→ `PUT /api/v1/projects/{id}`

只读展示（折叠 `el-descriptions`）：`id`、`created_at`、`created_by`（可选）

### 1.2 产品路线（FR-003 · `travel_product`）

区块标题 + 说明：「路线供内容 Agent / 落地页 Agent 引用」

**工具栏**：`[添加路线]` → 抽屉 480px

**表格列**：

| 列 | 字段 | 宽度 |
|----|------|------|
| 路线名称 | `name` | min 200 |
| 目的地 | `destinations_json` | Tag 组 |
| 天数 | `days` | 80 |
| 价格区间 | `price_range` | 120 |
| 适合人群 | `suitable_for` | 140 truncate |
| 操作 | — | 编辑 / 删除 |

**路线抽屉字段**：

| UI 标签 | DDL 列 | API | 必填 |
|---------|--------|-----|:----:|
| 路线名称 | `name` | `name` | ✅ |
| 目的地 | `destinations_json` | `destinations` | ✅ Tag 多选 |
| 行程天数 | `days` | `days` | — |
| 价格区间 | `price_range` | `priceRange` | — |
| 适合人群 | `suitable_for` | `suitableFor` | — |
| 亮点 | `highlights` | `highlights` | — `el-input textarea` |
| 服务包含 | `inclusions` | `inclusions` | — textarea |

API：
- `GET /api/v1/projects/{id}/products`
- `POST /api/v1/projects/{id}/products`
- `PUT /api/v1/projects/{id}/products/{productId}`
- `DELETE /api/v1/projects/{id}/products/{productId}`

空态：`el-empty`「暂无路线」+「添加第一条路线」

---

## Tab 2：竞品（FR-002 · `competitor`）

### 顶部提示

`el-alert type="info" show-icon`：「建议至少维护 **5** 个竞品，用于 GEO 诊断对比（FR-002）」  
计数：`已添加 {n} 个竞品`；n &lt; 5 时 `el-tag type="warning"`

### 工具栏

`[添加竞品]` primary plain → 抽屉 480px

### 表格列

| 列 | 字段 | 展示 |
|----|------|------|
| 竞品名称 | `name` | 主文案 |
| 官网 | `website` | 外链 `el-link` |
| 社媒 | `social_links_json` | 图标按钮组（见下） |
| 主推产品 | `main_products` | 2 行 truncate |
| 备注 | `notes` | tooltip |
| 操作 | — | 编辑 / 删除 |

**`social_links_json` 展示**（对象键示例）：

| 键 | 图标 |
|----|------|
| `instagram` | Instagram |
| `facebook` | Facebook |
| `youtube` | YouTube |
| `tiktok` | TikTok |
| 其他 | 通用 link |

### 竞品抽屉表单

| UI 标签 | DDL 列 | API | 组件 |
|---------|--------|-----|------|
| 竞品名称 | `name` | `name` | `el-input` required |
| 官网 | `website` | `website` | `el-input` URL |
| Instagram | `social_links_json.instagram` | `socialLinks` 子字段 | `el-input` |
| Facebook | `social_links_json.facebook` | 同上 | `el-input` |
| YouTube / TikTok | 同上 | 同上 | 可选行 |
| 主推产品 | `main_products` | `mainProducts` | textarea |
| 备注 | `notes` | `notes` | textarea |

API：
- `GET /api/v1/projects/{id}/competitors`
- `POST /api/v1/projects/{id}/competitors`
- `PUT /api/v1/projects/{id}/competitors/{competitorId}`
- `DELETE /api/v1/projects/{id}/competitors/{competitorId}`

空态：`el-empty` +「添加第一个竞品」

---

## Tab 3：知识库（FR-004 · `knowledge_asset`）

### 工具栏

| 操作 | 组件 | 说明 |
|------|------|------|
| 上传资料 | `el-button type="primary"` | 打开上传 dialog |
| 刷新 | icon button | 轮询 INDEXING 状态（30s，可选） |

### 筛选（inline）

| 字段 | 组件 | 参数 |
|------|------|------|
| 标题 | `el-input` | `title` 模糊 |
| 类型 | `el-select` | `type` enum |
| 向量化状态 | `el-select` | `vectorStatus` |

### 表格列

| 列 | 字段 | 展示 |
|----|------|------|
| 标题 | `title` | 可点击预览（有 `file_url` 则新窗） |
| 类型 | `type` | Tag 映射见下 |
| 标签 | `tags_json` | Tag 组 |
| 向量化 | `vector_status` | Tag + 进度（INDEXING 时 `el-progress` indeterminate） |
| 上传时间 | `created_at` | datetime |
| 操作 | — | 预览 / 编辑元数据 / 删除 / 重新索引（P2） |

### `knowledge_asset_type` Tag

| 值 | 中文 |
|----|------|
| `DOCUMENT` | 文档 |
| `FAQ` | FAQ |
| `ROUTE` | 路线说明 |
| `POLICY` | 政策/签证 |
| `WEB_PAGE` | 网页 |
| `OTHER` | 其他 |

### `vector_index_status` Tag（FR-005 状态）

| 值 | Tag type | 中文 |
|----|----------|------|
| `PENDING` | `info` | 待处理 |
| `INDEXING` | `primary` | 向量化中 |
| `READY` | `success` | 可检索 |
| `FAILED` | `danger` | 失败 |

失败行 tooltip 展示 `error`（API 扩展字段或日志链接）。

### 上传 Dialog

| 字段 | DDL | 组件 | 说明 |
|------|-----|------|------|
| 文件 | → MinIO → `file_url` | `el-upload` drag | DOCX/PDF/TXT/图片；单文件 ≤20MB（与后端一致） |
| 标题 | `title` | `el-input` | 默认文件名 |
| 类型 | `type` | `el-select` | 默认 DOCUMENT |
| 标签 | `tags_json` | `el-select` allow-create multiple | |
| 文本内容 | `content` | `el-input textarea` | TXT 直传或粘贴；文件型可空 |

提交：`POST /api/v1/projects/{id}/knowledge` → 202 + toast「已提交，向量化处理中」

**FR-005 占位**（不阻塞 EPIC-10）：
- 行操作「检索预览」：`disabled` + tooltip「向量化完成后可用（EPIC-10）」
- Tab 底 `el-alert`：「资料将向量化供 RAG 引用；生成内容将标注 chunk 来源。」

### 编辑元数据 Drawer

可改：`title`、`type`、`tags_json`；不可直接改编译后 `content`（需重新上传）。

---

## 字段 ↔ DDL 总表

### `customer_project`（Tab 品牌信息）

见 [projects-list.md §字段对照](./projects-list.md#字段--ddl--api-对照表)。

### `travel_product`

| UI | DDL | API camelCase |
|----|-----|---------------|
| ID | `id` | `id` |
| 项目 | `project_id` | `projectId` |
| 路线名 | `name` | `name` |
| 目的地 | `destinations_json` | `destinations` |
| 天数 | `days` | `days` |
| 价格 | `price_range` | `priceRange` |
| 适合人群 | `suitable_for` | `suitableFor` |
| 亮点 | `highlights` | `highlights` |
| 包含 | `inclusions` | `inclusions` |

### `competitor`

| UI | DDL | API camelCase |
|----|-----|---------------|
| ID | `id` | `id` |
| 名称 | `name` | `name` |
| 官网 | `website` | `website` |
| 社媒 | `social_links_json` | `socialLinks` |
| 主推产品 | `main_products` | `mainProducts` |
| 备注 | `notes` | `notes` |

### `knowledge_asset`

| UI | DDL | API camelCase |
|----|-----|---------------|
| ID | `id` | `id` |
| 类型 | `type` | `type` |
| 标题 | `title` | `title` |
| 内容 | `content` | `content` |
| 文件 URL | `file_url` | `fileUrl` |
| 标签 | `tags_json` | `tags` |
| 向量状态 | `vector_status` | `vectorStatus` |

`knowledge_chunk` 不在本页 CRUD；详情「切片数」只读 P2。

---

## 空 / 加载 / 错误

| 状态 | UI |
|------|-----|
| 加载 | 页级 `v-loading` |
| projectId 无效 | `el-result 404` |
| 跨 tenant | `el-result 403` |
| Tab 内空 | 各 Tab `el-empty` + 主 CTA |
| 保存失败 | 字段 error + `ElMessage.error` |
| 上传失败 | dialog 内 error + 保留表单 |

---

## URL 与 Tab 同步

- 默认：`/projects/:projectId?tab=brand`
- 竞品：`?tab=competitors`
- 知识库：`?tab=knowledge`
- 从列表「编辑竞品」深链：`/projects/1?tab=competitors`（P2）

---

## 响应式

| 断点 | 行为 |
|------|------|
| ≥1200px | 品牌表单两列；表格全列 |
| 768–1199px | 表单单列；隐藏「适合人群」「备注」 |
| &lt;768px | Tab 保持；抽屉 `size="100%"`；社媒列收起到展开行 |

---

## 路由配置

```text
path: /projects/:projectId
name: ProjectDetail
component: tourgeo/projects/detail
hidden: true
meta: { activeMenu: '/projects/index', title: '项目详情' }
```

列表跳转：

```typescript
router.push({ name: 'ProjectDetail', params: { projectId: row.id }, query: { tab: 'brand' } })
```

---

## 关联 API 汇总

| 资源 | 方法 | 路径 |
|------|------|------|
| 项目 | GET/PUT | `/api/v1/projects/{id}` |
| 路线 | CRUD | `/api/v1/projects/{id}/products` |
| 竞品 | CRUD | `/api/v1/projects/{id}/competitors` |
| 知识 | GET/POST/DELETE | `/api/v1/projects/{id}/knowledge` |
| 知识元数据 | PUT | `/api/v1/projects/{id}/knowledge/{assetId}` |

---

## 实现参考

- 列表/抽屉模式：[projects-list.md](./projects-list.md)
- Tab 详情：[diagnostic-detail.md](./diagnostic-detail.md)
- 上传：plus-ui `system/oss/index.vue` 或 `el-upload` + 业务 API
- Token：[tokens.md](../tokens.md)

---

## 范围外（Story 3+ / 不阻塞）

| 项 | 说明 |
|----|------|
| FR-005 语义检索 UI | EPIC-10 embed 完成后单独 Story |
| 问题库 / GEO | EPIC-2 独立菜单 |
| 报告导出 | EPIC-8 |
