# 线框：系统设置 · 套餐与额度（EPIC-9 M1）

> **PRD**：§8.10 系统与后台 · **FR-804** 套餐与计费 · §13.3 套餐与额度  
> **EPIC**：EPIC-9 M1 · **ADR-20260704-17**  
> **路由**：`/settings/billing`（侧栏「系统设置 → 套餐与额度」）  
> **数据表**：`subscription`（`quota_json` · `used_json`）

---

## 页面目标

在 **租户级** 只读展示当前套餐名称、计费周期与 **6 项额度** 使用情况（FR-804）；超额项高亮并提示联系升级。M1 **不提供** 在线购买/升级。

**M1 范围**：
- ✅ 套餐名 + 周期 + 6 项 `el-progress` + 超额/预警 alert
- ✅ 全局 402 拦截提示文案对齐（Axios interceptor）
- ❌ 升级购买、发票、套餐切换、支付网关
- ❌ 套餐 CRUD 后台、周期自动重置 Job、Redis 计数

**入口**：
- 侧栏「系统设置 → 套餐与额度」
- 工作台预警「本月诊断额度 80%」→ 本页（P2）
- 业务操作 402 toast「套餐额度不足」→ 链到本页（P2）

**权限**：租户管理员 `TENANT_ADMIN` 可访问；只读角色可查看用量（M1 与若依菜单权限对齐 `tourgeo:billing:view`）。

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 面包屑：系统设置 / 套餐与额度                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─ 超额 alert（任一 quota used ≥ limit 时页顶展示）─────────────────────────┐ │
│ │ ⚠ 套餐额度已用尽：GEO 诊断本月已达 4/4。相关操作已被拦截，请联系升级。    │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 套餐概览 el-card shadow="hover" ──────────────────────────────────────┐ │
│ │ 当前套餐    [增长服务版 Tag success]   growth_service                    │ │
│ │ 订阅状态    [生效中 Tag] ACTIVE                                          │ │
│ │ 计费周期    2026-07-04 至 2026-08-04  （剩余 28 天）                      │ │
│ │ [升级套餐] disabled   [购买加量] disabled   [下载发票] disabled            │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 额度用量 el-card ───────────────────────────────────────────────────────┐ │
│ │ H3 本周期额度使用                                                        │ │
│ │ ℹ 月度额度在周期结束日重置；客户项目数为租户总量上限。                    │ │
│ │                                                                          │ │
│ │ 客户项目数          1 / 5        ████░░░░░░  20%   el-progress success   │ │
│ │ GEO 诊断（月）      3 / 4        ███████░░░  75%   success               │ │
│ │ 关键词生成（月）    420 / 500    ████████░░  84%   warning + 小字预警    │ │
│ │ 内容生成（月）      12 / 100     █░░░░░░░░░  12%   success               │ │
│ │ 落地页生成（月）    20 / 20      ██████████ 100%   exception/danger      │ │
│ │ 报告生成（月）      2 / 8        ██░░░░░░░░  25%   success               │ │
│ │                                                                          │ │
│ │ [刷新用量]                                                                │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 说明 el-card shadow="never" type="info" ────────────────────────────────┐ │
│ │ · 超额时创建诊断、AI 生成等操作将返回「额度不足」并拦截。                 │ │
│ │ · 升级套餐或增购额度请联系您的客户成功经理。（M1 无在线支付）             │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 套餐概览卡片

| UI 标签 | DDL / API | 展示 |
|---------|-----------|------|
| 当前套餐 | `plan_code` | 中文名 Tag + mono `planCode` |
| 订阅状态 | `status` | `subscription_status` Tag |
| 计费周期 | `period_start`, `period_end` | `YYYY-MM-DD 至 YYYY-MM-DD` |
| 剩余天数 | 计算 | `(periodEnd - today)` 天；≤7 天 warning 小字 |

### `plan_code` 中文映射（PRD §13.3 + seed）

| DB 值 | 中文 | 说明 |
|-------|------|------|
| `diagnostic_report` | 诊断报告版 | 按次 |
| `basic_saas` | 基础 SaaS 版 | 月订阅 |
| `growth_service` | 增长服务版 | demo seed 默认 |
| `oem_private` | OEM/私有化版 | 企业 |
| `trial` | 试用版 | dev/staging |
| *(其他)* | 原值 | fallback |

### `subscription_status` Tag

| DB 值 | Tag type | 中文 |
|-------|----------|------|
| `ACTIVE` | `success` | 生效中 |
| `TRIAL` | `primary` | 试用中 |
| `EXPIRED` | `danger` | 已过期 |
| `CANCELLED` | `info` | 已取消 |

**过期/取消态**：页顶 `el-alert type="error"`「订阅已失效，部分功能可能不可用」；额度区仍展示最后快照。

### M1 disabled 工具栏

| 按钮 | tooltip |
|------|---------|
| 升级套餐 | 「在线升级 M2 · 请联系客户成功经理」 |
| 购买加量 | 「增购额度 M2」 |
| 下载发票 | 「发票 M2」 |

---

## 六额度进度条（核心）

与 `002_seed_demo.sql` · PRD §13.3 · Sprint quota 键 **一致**：

| # | JSON 键 | 中文标签 | 单位 | 周期 | 拦截动作 |
|---|---------|----------|------|------|----------|
| 1 | `projects` | 客户项目数 | 个 | **租户总量** | `POST .../projects` |
| 2 | `diagnostics_per_month` | GEO 诊断 | 次/月 | 本周期 | `POST .../diagnostics` |
| 3 | `keywords_per_month` | 关键词 AI 生成 | 次/月 | 本周期 | `POST .../keywords/generate` |
| 4 | `content_per_month` | 内容 AI 生成 | 次/月 | 本周期 | `POST .../content-tasks/.../generate` |
| 5 | `landing_pages_per_month` | 落地页 AI 生成 | 次/月 | 本周期 | `POST .../landing-pages/.../generate` |
| 6 | `reports_per_month` | 报告生成 | 次/月 | 本周期 | `POST .../reports/weekly` |

**数据源**：

```json
{
  "quotaJson": {
    "projects": 5,
    "diagnostics_per_month": 4,
    "keywords_per_month": 500,
    "content_per_month": 100,
    "landing_pages_per_month": 20,
    "reports_per_month": 8
  },
  "usedJson": {
    "projects": 1,
    "diagnostics_per_month": 3,
    "keywords_per_month": 420,
    "content_per_month": 12,
    "landing_pages_per_month": 20,
    "reports_per_month": 2
  }
}
```

缺键：`limit=0` 或隐藏该项（M1 建议展示 0/0 + info「未配置」）。

### 单行进度条结构

```
┌─ QuotaRow ─────────────────────────────────────────────────────────┐
│ [标签 140px]  [used / limit 右对齐]                                 │
│ [el-progress :percentage :status :stroke-width="12"]                │
│ [可选副文案：距超额还剩 N 次 · 或 已超额 N 次]                       │
└─────────────────────────────────────────────────────────────────────┘
```

| 组件 | 规范 |
|------|------|
| `el-progress` | `:percentage="Math.min(100, used/limit*100)"` |
| 文案 | `{used} / {limit} {unit}` |
| 间距 | 行间距 `margin-bottom: 20px` |

### 进度条颜色 / status（tokens 对齐）

| 使用率 | `el-progress` status | 行样式 | 说明 |
|--------|---------------------|--------|------|
| &lt; 80% | `success` 或默认（`#1677A0`） | 正常 | — |
| 80% – 99% | `warning` | 标签旁 `el-tag type="warning" size="small"`「即将用尽」 | 工作台预警同源 |
| ≥ 100% | `exception` | 标签旁 `el-tag type="danger"`「已超额」；进度条 `--tg-color-danger` | 拦截已生效 |

**无障碍**：不仅靠颜色 — Tag 文案「即将用尽 / 已超额」+ progress `aria-valuenow`。

### 超额项行内 alert（可选，与页顶二选一或叠加）

当单项 ≥ 100%：

```
el-alert type="error" :closable="false" show-icon
  title="落地页 AI 生成已达本月上限（20/20）"
  description="请等待下周期重置或联系升级套餐。"
```

---

## 页顶超额 Alert（聚合）

**展示条件**：任一 quota `used >= limit`（且 `limit > 0`）。

| 类型 | 文案模板 |
|------|----------|
| 单项超额 | `套餐额度已用尽：{label}已达 {used}/{limit}。相关操作已被拦截，请联系升级。` |
| 多项超额 | `多项套餐额度已用尽（{label1}、{label2}…）。请联系升级。` |

组件：`el-alert type="error" show-icon :closable="false"` 固定页顶（滚动时 sticky 可选 P2）。

**预警态**（无超额、但有 ≥80%）：`el-alert type="warning"`「部分额度即将用尽，请合理安排本月操作。」

---

## 全局 402 拦截 UI（Axios）

业务 API 超额时 Java 返回：

```json
{ "code": 40201, "message": "套餐额度不足：GEO 诊断本月已达上限，请升级套餐", "data": null }
```

| 场景 | UI |
|------|-----|
| 任意 POST 触发 402 | `ElMessage.error(message)` 5s |
| 可选 P2 | Message 内链「查看额度 →」`/settings/billing` |
| 重复触发 | 防抖 3s 内同 key 不重复弹 |

与 [dashboard.md](./dashboard.md) 预警「本月诊断额度 80%」文案一致。

---

## 空 / 加载 / 错误

| 状态 | UI |
|------|-----|
| 加载 | 卡片区 `v-loading` |
| 无 ACTIVE 订阅 | `el-empty`「暂无有效套餐」+ info「开发环境可能未配置 subscription；请联系管理员。」 |
| API 403 | 非管理员 `el-result 403` |
| API 500 | `ElMessage.error` + 重试按钮 |

---

## 响应式

| 断点 | 行为 |
|------|------|
| ≥992px | 概览卡与额度卡上下全宽；进度条 label 左、数值右 |
| &lt;992px | 标签与数值堆叠；disabled 按钮收进 dropdown「更多」 |

---

## 菜单与路由

```text
父菜单：系统设置 (path: /settings, icon: setting)
  └─ 套餐与额度 (path: /settings/billing, component: tourgeo/settings/billing/index)
  └─ 成员权限 / 模型配置 / 模板 / 审计 — 沿用若依或 M2 placeholder
```

**权限标识**：`tourgeo:billing:view`

---

## API 依赖（开发对齐）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/settings/billing` 或 `/api/v1/tenants/current/subscription` | 当前租户 ACTIVE 订阅 |

**响应 Vo 建议**（camelCase）：

```json
{
  "planCode": "growth_service",
  "planName": "增长服务版",
  "status": "ACTIVE",
  "periodStart": "2026-07-04",
  "periodEnd": "2026-08-04",
  "daysRemaining": 28,
  "quotas": [
    { "key": "projects", "label": "客户项目数", "used": 1, "limit": 5, "unit": "个", "period": "total" },
    { "key": "diagnostics_per_month", "label": "GEO 诊断", "used": 3, "limit": 4, "unit": "次", "period": "monthly" }
  ],
  "hasOverage": true,
  "hasWarning": true,
  "overageKeys": ["landing_pages_per_month"]
}
```

后端可预计算 `percentage`、`status`（`normal` | `warning` | `overage`）减轻前端逻辑。

---

## 字段 ↔ DDL 对照

| UI | DDL 列 | API |
|----|--------|-----|
| 套餐代码 | `plan_code` | `planCode` |
| 额度上限 | `quota_json` | `quotaJson` / `quotas[].limit` |
| 已用 | `used_json` | `usedJson` / `quotas[].used` |
| 周期起 | `period_start` | `periodStart` |
| 周期止 | `period_end` | `periodEnd` |
| 状态 | `status` | `status` |

---

## M1 范围边界

| 包含 | 不包含 |
|------|--------|
| 6 项进度条 + 超额/预警 alert | Stripe/支付 |
| 套餐名 + 周期展示 | 套餐 CRUD |
| 402 文案规范 | 周期 Job 重置 |
| disabled 升级/购买 | FR-802 模型配置页 |
| seed `growth_service` 演示 | FR-806 审计日志 |

---

## 实现参考

| 项 | 建议 |
|----|------|
| 视图 | `inbound-admin/src/views/tourgeo/settings/billing/index.vue` |
| API | `src/api/tourgeo/billing.ts` |
| 常量 | `src/constants/billing.ts` — `PLAN_LABELS`, `QUOTA_META` |
| 进度组件 | 可抽 `QuotaProgressRow.vue` |
| 布局参考 | [reports-list.md](./reports-list.md) KPI 卡片密度；[dashboard.md](./dashboard.md) 额度预警 |
| Token | [tokens.md](../tokens.md) — warning/danger 语义色 |

---

## 版本

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-04 | UI 设计 | EPIC-9 M1 初版 · FR-804 · ADR-20260704-17 |
