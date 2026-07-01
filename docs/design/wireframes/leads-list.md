# 线框：线索与转化 · 询盘线索列表（EPIC-7 M1）

> **PRD**：§8.8 线索与转化 · **FR-601** 表单线索记录  
> **EPIC**：EPIC-7 M1 · ADR-20260701-14  
> **路由**：`/projects/:projectId/leads`（侧栏 `/leads` 重定向当前项目）  
> **数据表**：`lead`（join `landing_page`、`keyword_opportunity`）

---

## 页面目标

在项目上下文中 **查看落地页表单提交的询盘线索**：列表浏览、筛选、**只读详情**；记录联系人、来源页、关键词、UTM、设备与行程需求（FR-601）。

**M1 范围**：
- ✅ 列表 + 详情 drawer；status 展示（默认 `NEW`）
- ❌ 状态流转 / 负责人 / 跟进记录（FR-605 P3）
- ❌ WhatsApp 点击追踪（FR-602）
- ❌ AI 跟进话术（FR-603）
- ❌ 归因报表 / 导出 CSV（FR-606；FR-601 导出 → M2 disabled）

**入口**：
- 侧栏「线索与转化」→ 询盘线索
- [landing-page-list.md](./landing-page-list.md) 预览 drawer 底部「查看该页线索」→ `?landingPageId=`（P2 预置筛选）
- 工作台 KPI「本周询盘」→ 本页 + 近 7 日时间筛选（P2）

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 面包屑：线索与转化 / 询盘线索        当前项目 ▼ Dragon Journey Travel       │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─ 项目选择器 (同 landing-page-list) ──────────────────────────────────────┐ │
│ │ 客户项目 [Dragon Journey ▼]   目标市场 US UK AU                          │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 搜索区 (el-card, 可折叠) ───────────────────────────────────────────────┐ │
│ │ 姓名 [____]  邮箱 [____]  电话 [____]  来源 [全部▼]  状态 [全部▼]        │ │
│ │ 落地页 [____]  关键词 [____]  提交时间 [日期范围]    [搜索] [重置]        │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 工具栏 ─────────────────────────────────────────────────────────────────┐ │
│ │                              [导出线索] disabled  [显示搜索] [刷新]       │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ el-table border v-loading ──────────────────────────────────────────────┐ │
│ │ 姓名 | 邮箱 | 电话 | 来源 | 落地页 | 关键词 | 状态 | 提交时间 | 操作      │ │
│ │ Sarah M. | s***@gmail.com | +1•••4521 | 表单 | chongqing-cyber… | Chong… │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                              [分页 10/20/50]                                │
│ ℹ 线索由落地页公开表单写入；Admin 不可手工创建。                              │
└─────────────────────────────────────────────────────────────────────────────┘

详情 (el-drawer 560px):
┌─ 线索详情 · #1042 ──────────────────────────────────── [×] │
│ [新线索 Tag]  提交于 2026-07-01 14:32 (UTC+8)              │
│ ── 联系人 (el-descriptions :column="1" border) ──          │
│ 姓名      Sarah Mitchell                                   │
│ 邮箱      sarah.mitchell@gmail.com    [复制] [发邮件] P2     │
│ 电话      +1 415 555 4521             [复制]               │
│ ── 行程需求 ──                                              │
│ 出行日期  2026-09-15                                       │
│ 人数      2                                                │
│ 预算      USD 3,000–5,000 per person                       │
│ 留言      We are first-time visitors interested in…        │
│            (el-input type=textarea readonly rows=4)        │
│ ── 来源归因 ──                                              │
│ 来源渠道  表单 form                                        │
│ 落地页    Chongqing Cyberpunk Tour  /chongqing-cyber…      │
│           [打开落地页预览] → landing-page-list drawer P2    │
│ 关键词    Chongqing cyberpunk city tour · US               │
│ ── UTM (el-descriptions) ──                                │
│ Source    google    Medium   cpc    Campaign  cn-tour-us   │
│ Content   hero-cta  Term       chongqing private tour      │
│ ── 设备 ──                                                  │
│ device    Mozilla/5.0 … Chrome/125 … Mobile Safari         │
│            (mono 小字；过长 show-overflow-tooltip)           │
│ ── 系统 ──                                                  │
│ 线索 ID   1042 · status NEW · assignee — (M2)            │
│ [关闭]  [变更状态] FR-605 disabled  [AI 跟进建议] FR-603 disabled │
└─────────────────────────────────────────────────────────────┘
```

---

## 项目选择器

与 [landing-page-list.md](./landing-page-list.md) / [keywords-list.md](./keywords-list.md) **一致**：

| 项 | 规范 |
|----|------|
| 路由 | `/projects/:projectId/leads` |
| 切换 | `router.replace({ name: 'LeadsList', params: { projectId } })` + 重置筛选 |
| 无项目 | `el-empty`「请先创建客户项目」→ `/projects` |

---

## 筛选栏

| 字段 | 组件 | 查询参数 | 说明 |
|------|------|----------|------|
| 姓名 | `el-input` | `name` | 模糊 |
| 邮箱 | `el-input` | `email` | 模糊 |
| 电话 | `el-input` | `phone` | 模糊 |
| 来源 | `el-select` | `source` | 见 `source` 枚举 |
| 状态 | `el-select` | `status` | `lead_status`；M1 多为 `NEW` |
| 落地页 | `el-input` | `landingPage` | join `landing_page.title` / `slug` 模糊 |
| 关键词 | `el-input` | `keyword` | join `keyword_opportunity.keyword` 模糊 |
| 提交时间 | `el-date-picker` daterange | `createdAt` | 映射 `created_at` |

**Query 深链**（P2）：
- `?landingPageId={id}` — 从落地页跳转，预填落地页筛选
- `?status=NEW` — 工作台待办链入

---

## 表格列 ↔ DDL / API（HANDOFF 必含列）

| 列 | 宽度 | DDL 列 | API 字段 | 展示 |
|----|------|--------|----------|------|
| 姓名 | 120 | `name` | `name` | 主文案；空则 `—`；点击进入详情 drawer |
| 邮箱 | min 160 | `email` | `email` | **列表脱敏**：`s***@domain.com`；详情全量 |
| 电话 | 130 | `phone` | `phone` | **列表脱敏**：保留国家码 + 末 4 位；详情全量 |
| 来源 | 90 | `source` | `source` | Tag 中文 |
| 落地页 | min 160 | `landing_page_id` → join | `landingPageTitle` / `landingPageSlug` | 标题；副行 mono `/slug`；无则 `—` |
| 关键词 | min 140 | `keyword_id` → join | `keywordText` | 英文词；无则 `—` |
| 状态 | 90 | `status` | `status` | Tag 见下 |
| 提交时间 | 160 | `created_at` | `createdAt` | `YYYY-MM-DD HH:mm` |
| 操作 | fixed 80 | — | — | 「详情」 |

可选隐藏列（P2）：`travel_date`、`party_size`（列表紧凑时可只在 drawer 展示）。

### `lead_status` Tag（FR-605 枚举 · M1 只读展示）

| DB 值 | Tag type | 中文 | M1 |
|-------|----------|------|-----|
| `NEW` | `danger` | 新线索 | 默认 |
| `FOLLOWING` | `primary` | 跟进中 | 展示 only |
| `QUOTED` | `warning` | 已报价 | 展示 only |
| `WON` | `success` | 已成交 | 展示 only |
| `LOST` | `info` | 已流失 | 展示 only |

M1 后端写入均为 `NEW`；UI 预留 Tag 映射，**不可编辑**。

### `source` 渠道 Tag

| 值 | 中文 | Tag type | M1 |
|----|------|----------|-----|
| `form` | 表单 | `primary` | ✅ 公开 POST 默认 |
| `whatsapp` | WhatsApp | `success` | FR-602 预留 |
| `manual` | 手工录入 | `info` | FR-605 预留 |
| *(其他)* | 原值 | `info` | fallback |

---

## 行操作（M1）

| 操作 | 行为 |
|------|------|
| 详情 | 打开详情 drawer；`GET .../leads/{leadId}` |
| 复制邮箱 | drawer 内 icon-button（列表不展示，防误触） |

无删除、无编辑、无批量操作（合规：线索为审计数据）。

---

## 详情 Drawer（FR-601）

**触发**：点击姓名 / 「详情」；宽度 `560px`；`destroy-on-close`。

### 区块 1：状态头

- `el-tag` status + 提交时间 `createdAt`（本地时区）
- 线索 ID `#id` 灰色小字

### 区块 2：联系人

| UI 标签 | DDL | API | 说明 |
|---------|-----|-----|------|
| 姓名 | `name` | `name` | |
| 邮箱 | `email` | `email` | `el-link` mailto；复制按钮 |
| 电话 | `phone` | `phone` | 复制按钮 |

### 区块 3：行程需求

| UI 标签 | DDL | API | 说明 |
|---------|-----|-----|------|
| 出行日期 | `travel_date` | `travelDate` | `YYYY-MM-DD`；空 `—` |
| 人数 | `party_size` | `partySize` | 整数 + 「人」 |
| 预算 | `budget` | `budget` | 自由文本 |
| 留言 | `message` | `message` | `readonly textarea`；空 `—` |

字段与 [landing-page-list.md](./landing-page-list.md) FR-505 表单字段 **一一对应**。

### 区块 4：来源归因

| UI 标签 | DDL | API | 说明 |
|---------|-----|-----|------|
| 来源渠道 | `source` | `source` | Tag |
| 落地页 | `landing_page_id` | `landingPageTitle`, `landingPageSlug`, `landingPageId` | 双行标题 + slug；P2 链到落地页预览 |
| 关键词 | `keyword_id` | `keywordText`, `keywordMarket` | 词 + 市场 Tag |

### 区块 5：UTM

从 `utm_json` 解析（camelCase API `utm`）：

| Key | 展示标签 |
|-----|----------|
| `utm_source` | Source |
| `utm_medium` | Medium |
| `utm_campaign` | Campaign |
| `utm_content` | Content |
| `utm_term` | Term |

空对象：`el-empty` size="small"「无 UTM 参数」。

示例 JSON（落库 `utm_json`）：

```json
{
  "utm_source": "google",
  "utm_medium": "cpc",
  "utm_campaign": "cn-tour-us",
  "utm_content": "hero-cta",
  "utm_term": "chongqing private tour"
}
```

### 区块 6：设备

| UI 标签 | DDL | API |
|---------|-----|-----|
| 设备 / UA | `device` | `device` |

`font-family: monospace`；`font-size: 12px`；超 2 行 `line-clamp` + tooltip 全文。

### 区块 7：系统信息（折叠 optional）

- `assignee_id` → `assigneeName`：M1 恒 `—`
- `updated_at`：次要信息

### Drawer 底栏

| 按钮 | M1 |
|------|-----|
| 关闭 | ✅ |
| 变更状态 | disabled + tooltip「CRM 跟进 M2 · FR-605」 |
| AI 跟进建议 | disabled + tooltip「FR-603 M2」 |

---

## 字段 ↔ DDL 完整对照

| UI | DDL 列 | API camelCase | 列表 | 详情 |
|----|--------|---------------|:----:|:----:|
| ID | `id` | `id` | — | ✅ |
| 姓名 | `name` | `name` | ✅ | ✅ |
| 邮箱 | `email` | `email` | ✅ 脱敏 | ✅ |
| 电话 | `phone` | `phone` | ✅ 脱敏 | ✅ |
| 出行日期 | `travel_date` | `travelDate` | P2 | ✅ |
| 人数 | `party_size` | `partySize` | P2 | ✅ |
| 预算 | `budget` | `budget` | — | ✅ |
| 留言 | `message` | `message` | — | ✅ |
| 来源 | `source` | `source` | ✅ | ✅ |
| UTM | `utm_json` | `utm` | — | ✅ |
| 设备 | `device` | `device` | — | ✅ |
| 落地页 | `landing_page_id` | `landingPageId` + join | ✅ | ✅ |
| 关键词 | `keyword_id` | `keywordId` + join | ✅ | ✅ |
| 状态 | `status` | `status` | ✅ | ✅ |
| 负责人 | `assignee_id` | `assigneeId` | — | M2 |
| 提交时间 | `created_at` | `createdAt` | ✅ | ✅ |

---

## 公开表单写入（Admin 只读 · 供联调理解）

Admin **不提供创建入口**。线索由 `POST /api/v1/public/leads` 写入（Java HANDOFF）：

| 请求字段 | DDL | 说明 |
|----------|-----|------|
| `landingPageId` | `landing_page_id` | 必填；解析 tenant/project |
| `name` / `email` / `phone` | 同名列 | 至少 email 或 phone 其一 |
| `travelDate` / `partySize` / `budget` / `message` | 同名列 | 可选 |
| `utm` | `utm_json` | object |
| `device` | `device` | UA 字符串 |
| `source` | `source` | 默认 `form` |

`keyword_id` 由服务端从 `landing_page.keyword_id` **冗余写入**（若页面有关联词）。

---

## 隐私与合规

| 项 | 规范 |
|----|------|
| 列表脱敏 | 邮箱、电话部分掩码（见上） |
| 详情权限 | 沿用若依项目级 Casbin；跨 tenant 403 |
| 导出 | M1 按钮 disabled；M2 脱敏 CSV + 审计日志 |
| 空留言 | 不展示空白 textarea |
| 合规提示 | 页脚 `el-alert type="info"` closable：「线索含个人信息，请勿外传；导出需授权。」 |

---

## 空 / 加载 / 错误

| 状态 | UI |
|------|-----|
| 加载 | `v-loading` |
| 无线索 | `el-empty` description「暂无询盘」；sub「发布落地页并开启表单后，提交将出现在此」；链到 [landing-page-list](./landing-page-list.md) |
| 无项目 | → `/projects` |
| 详情 404 | drawer 内 `ElMessage.error` + 关闭 |
| API 失败 | `ElMessage.error` + 保留筛选条件 |

---

## 响应式

| 断点 | 行为 |
|------|------|
| ≥1200px | 全列 |
| 768–1199px | 隐藏 phone、keyword |
| &lt;768px | 姓名、来源、状态、提交时间、操作；drawer 100% 宽 |

---

## 菜单与路由

```text
父菜单：线索与转化 (path: /leads, icon: user 或 message)
  └─ 询盘线索 (path: /projects/:projectId/leads, component: tourgeo/leads/index)

侧栏快捷 /leads → redirect 到 projectStore.currentProjectId 或最近访问项目
```

**权限标识建议**：`tourgeo:lead:list`（与若依 `@SaCheckPermission` 对齐）。

---

## API 依赖（开发对齐）

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/api/v1/projects/{projectId}/leads` | 分页列表 + 筛选 |
| GET | `/api/v1/projects/{projectId}/leads/{leadId}` | 详情 drawer |

List 响应 join 字段：`landingPageTitle`, `landingPageSlug`, `keywordText`, `keywordMarket`。

分页：`page`, `size`；响应 `{ total, list }`。

---

## 组件与实现提示

| 项 | 建议 |
|----|------|
| 视图 | `inbound-admin/src/views/tourgeo/leads/index.vue` |
| API | `src/api/tourgeo/lead.ts` |
| 脱敏 | `utils/maskPii.ts` — `maskEmail`, `maskPhone` |
| 常量 | `src/constants/lead.ts` — `LEAD_STATUS_LABELS`, `LEAD_SOURCE_LABELS` |
| 参考实现 | `keywords/index.vue`、`landing/index.vue` 列表模式 |

---

## 版本

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-01 | UI 设计 | EPIC-7 M1 初版 · FR-601 · ADR-20260701-14 |
