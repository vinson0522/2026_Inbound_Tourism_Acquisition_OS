# 线框：客户项目列表 + 创建项目（FR-001）

> **PRD**：§6.1 客户项目 · FR-001 创建客户项目 · FR-003 产品路线（创建流 Step 2）  
> **路由**：`/projects`（列表）；创建用 **抽屉**（推荐）或 `/projects/create`  
> **数据表**：`customer_project`；主推路线 → `travel_product`（创建成功后可选 Step 2）

---

## 页面目标

租户内维护客户项目：列表浏览、筛选、新建（FR-001）、进入项目工作台。创建成功后自动设为顶栏「当前项目」，并引导进入工作台或补充路线。

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 面包屑：客户项目                                    [新建客户项目]      │
├─────────────────────────────────────────────────────────────────────────┤
│ ┌─ 搜索区 (el-card, 可折叠) ─────────────────────────────────────────┐ │
│ │ 项目名称 [____]  品牌名 [____]  状态 [全部▼]  目标市场 [全部▼]     │ │
│ │ 创建时间 [日期范围]                    [搜索] [重置]                │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 工具栏 ────────────────────────────────────────────────────────────┐ │
│ │ [新建]                                              [显示搜索][刷新]│ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│ ┌─ el-table border ──────────────────────────────────────────────────┐ │
│ │ 项目名称 | 品牌名 | 目标市场 | 服务语言 | 状态 | 创建时间 | 操作    │ │
│ │ First-Time… | Dragon Journey | US UK AU | en | 正常 | 2026-06-20 … │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                              [分页 10/20/50]                            │
└─────────────────────────────────────────────────────────────────────────┘

创建抽屉 (el-drawer 560px, 自右侧):
┌─ 新建客户项目 ─────────────────────────────── [×] │
│ Step 1 基础信息 (el-steps :active="0")          │
│ ┌─────────────────────────────────────────────┐ │
│ │ 项目名称*     [________________________]    │ │
│ │ 品牌名*       [________________________]    │ │
│ │ 官网          [https://________________]    │ │
│ │ 行业          [入境游 ▼] (默认 inbound_tourism)│ │
│ │ 目标市场*     [多选 Tag: US UK AU …]        │ │
│ │ 服务语言*     [多选: en ▼]                  │ │
│ └─────────────────────────────────────────────┘ │
│ ▼ 展开：主推路线（可选，FR-003 简版）           │
│ ┌─────────────────────────────────────────────┐ │
│ │ 路线名称      [10-Day China Private Tour]   │ │
│ │ 目的地        [多选/标签 Beijing Xi'an…]    │ │
│ │ 行程天数      [10]  价格区间 [USD 2500-4500]│ │
│ └─────────────────────────────────────────────┘ │
│ [取消]              [保存并进入工作台] [仅保存] │
└─────────────────────────────────────────────────┘
```

---

## 字段 ↔ DDL / API 对照表

### `customer_project`（列表 + Step 1 创建）

| UI 标签 | DDL 列 | 类型 | API 字段 | 列表 | 创建 | 校验 |
|---------|--------|------|----------|:----:|:----:|------|
| 项目 ID | `id` | BIGSERIAL | `id` | — | — | 只读 |
| — | `tenant_id` | BIGINT | — | — | — | 后端 JWT 注入，前端不传 |
| 项目名称 | `name` | VARCHAR(200) | `name` | ✅ | ✅ | 必填，1–200 字 |
| 品牌名 | `brand_name` | VARCHAR(200) | `brandName` | ✅ | ✅ | 必填，1–200 字 |
| 官网 | `website` | VARCHAR(500) | `website` | — | ✅ | 可选，URL 格式 |
| 行业 | `industry` | VARCHAR(64) | `industry` | — | ✅ | 默认 `inbound_tourism` |
| 目标市场 | `target_markets_json` | JSONB | `targetMarkets` | ✅ Tag | ✅ | 必填，至少 1 项；值如 `US`/`UK`/`AU` |
| 服务语言 | `languages_json` | JSONB | `languages` | ✅ Tag | ✅ | 必填，默认 `["en"]` |
| 状态 | `status` | entity_status | `status` | ✅ Tag | 编辑 | 默认 `ACTIVE` |
| 创建时间 | `created_at` | TIMESTAMPTZ | `createdAt` | ✅ | — | — |
| 更新时间 | `updated_at` | TIMESTAMPTZ | `updatedAt` | — | — | — |
| — | `created_by` | BIGINT | — | — | — | 后端当前用户 |
| — | `deleted_at` | TIMESTAMPTZ | — | — | — | 软删，列表不展示 |

### `entity_status`（`status` 列）

| 值 | Tag type | 中文 |
|----|----------|------|
| `ACTIVE` | `success` | 正常 |
| `INACTIVE` | `info` | 停用 |
| `SUSPENDED` | `warning` | 暂停 |
| `ARCHIVED` | `info` | 已归档 |

### Step 2 可选：`travel_product`（FR-001「主推路线」→ FR-003 简版）

创建项目 API 成功后，若用户填写了路线区块，**追加** `POST /api/v1/projects/{id}/products`（Story 2 可与创建同事务或分步）。

| UI 标签 | DDL 列 | API 字段 | 校验 |
|---------|--------|----------|------|
| 路线名称 | `name` | `name` | 展开时必填 |
| 目的地 | `destinations_json` | `destinations` | JSON 数组 |
| 行程天数 | `days` | `days` | 正整数 |
| 价格区间 | `price_range` | `priceRange` | 可选，如 `USD 2500-4500` |
| 适合人群 | `suitable_for` | `suitableFor` | 高级项，可隐藏 |
| 亮点 | `highlights` | `highlights` | 高级项 |
| 服务包含 | `inclusions` | `inclusions` | 高级项 |

**UI 策略**：默认折叠「主推路线」；展开后仅展示 name + destinations + days + price_range（与 demo 种子一致）。未展开则只创建 `customer_project`。

---

## 搜索表单

| 字段 | 组件 | 查询参数 | 说明 |
|------|------|----------|------|
| 项目名称 | `el-input` | `name` | 模糊 |
| 品牌名 | `el-input` | `brandName` | 模糊 |
| 状态 | `el-select` | `status` | ACTIVE / INACTIVE / … |
| 目标市场 | `el-select` | `market` | 单选过滤 JSONB 含该市场 |
| 创建时间 | `el-date-picker` daterange | `createdAt` | 与 user 列表一致 |

---

## 表格列

| 列 | 宽度 | 字段 | 展示 |
|----|------|------|------|
| 项目名称 | min 200 | `name` | 主链接 → `/dashboard?projectId={id}` 或设当前项目后进工作台 |
| 品牌名 | 160 | `brand_name` | `show-overflow-tooltip` |
| 目标市场 | 140 | `target_markets_json` | 多 `el-tag size="small"`（US、UK…） |
| 服务语言 | 100 | `languages_json` | Tag（en、zh…） |
| 状态 | 90 | `status` | Tag 见上表 |
| 创建时间 | 160 | `created_at` | `YYYY-MM-DD HH:mm` |
| 操作 | fixed right 180 | — | 见下 |

### 行操作

| 操作 | 组件 | 行为 |
|------|------|------|
| 进入 | `el-button link` primary | 设为当前项目 → `/dashboard` |
| 编辑 | `el-button link` | 打开抽屉，同创建表单（PUT） |
| 更多 | `el-dropdown` | 停用/归档（改 `status`）、删除（软删，二次确认） |

---

## 创建 / 编辑抽屉

| 项 | 规范 |
|----|------|
| 组件 | `el-drawer` `size="560px"` `destroy-on-close` |
| 标题 | 新建：「新建客户项目」；编辑：「编辑项目 · {name}」 |
| 表单 | `el-form` `label-width="100px"` `label-position="right"` |
| 行业 | `el-select`：入境游(`inbound_tourism`)、文旅、酒店、其他（值存 DDL `industry` 字符串） |
| 目标市场 | `el-select multiple` + 常用：US、UK、AU、DE、FR、JP、KR、SEA |
| 服务语言 | `el-select multiple`：en（默认）、zh、de、fr、ja、ko |
| 官网 | `el-input` prefix `https://`；校验 URL |
| 主推路线 | `el-collapse-item`「主推路线（可选）」内嵌子表单 |

### 提交按钮

| 按钮 | 行为 |
|------|------|
| 取消 | 关闭抽屉，不保存 |
| 仅保存 | POST/PUT 项目；toast 成功；刷新列表 |
| 保存并进入工作台 | 保存 → `projectStore.setCurrent(id)` → `router.push('/dashboard')` |

### 表单校验（`el-form rules`）

| 字段 | 规则 |
|------|------|
| `name` | required；max 200 |
| `brand_name` | required；max 200 |
| `website` | type url（可选） |
| `target_markets` | required；min 1 |
| `languages` | required；min 1 |
| 路线 `name` | 仅 collapse 展开且任一路线字段有值时 required |

---

## 空 / 加载 / 错误态

| 状态 | UI |
|------|-----|
| 加载 | 表格 `v-loading` |
| 空列表 | `el-empty` description「暂无客户项目」+ 主按钮「新建客户项目」 |
| 搜索无结果 | `el-empty`「未找到匹配项目」+ 链接「清除筛选」 |
| 提交失败 | 字段级 `el-form-item` error + 顶部 `ElMessage.error`（展示 `message`） |
| 网络错误 | `ElMessage` + 抽屉内「重试」保留已填数据 |

---

## 与其他页面联动

| 场景 | 行为 |
|------|------|
| 工作台无项目 | [dashboard.md](./dashboard.md) 空态 CTA → 打开本页新建抽屉 |
| 顶栏「当前项目」 | 下拉数据源 = 本列表 API；切换后刷新 dashboard / diagnostics |
| 创建成功 | 自动加入 projectStore；可选 toast「已设为当前项目」 |

---

## 响应式

| 断点 | 行为 |
|------|------|
| ≥1200px | 全列展示 |
| 768–1199px | 隐藏「服务语言」列 |
| &lt;768px | 仅保留项目名称、状态、操作；搜索默认折叠；抽屉 `size="100%"` |

---

## 菜单配置（若依）

```text
父菜单：客户项目 (path: /projects, icon: briefcase 或 list)
  └─ 项目列表 (path: /projects/index, component: tourgeo/projects/index)
  └─ 品牌资料 / 竞品 / 知识库 … (二级占位，Story 3+)
```

---

## 关联 API（Story 2）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/projects` | 分页列表，`?name=&status=&market=&pageNum=&pageSize=` |
| POST | `/api/v1/projects` | 创建 FR-001 |
| GET | `/api/v1/projects/{id}` | 详情 / 编辑回填 |
| PUT | `/api/v1/projects/{id}` | 更新 |
| DELETE | `/api/v1/projects/{id}` | 软删（设 `deleted_at`） |
| POST | `/api/v1/projects/{id}/products` | 可选，创建主推路线 |

响应体遵循 `{ code, message, data, trace_id }`；列表 `data` 含 `rows` + `total`。

---

## 实现参考

- 列表模式：`inbound-admin/src/views/system/user/index.vue`
- 抽屉创建：对齐 [diagnostics-list.md](./diagnostics-list.md) 新建诊断抽屉
- Token：`docs/design/tokens.md`
