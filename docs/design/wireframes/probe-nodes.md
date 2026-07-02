# 线框：系统设置 · 探针节点（EPIC-11 M1）

> **PRD**：§7.6.4 浏览器扩展探针 · **FR-113** 探针节点管理  
> **EPIC**：EPIC-11 M1 · **ADR-20260705-18**  
> **路由**：`/settings/probe-nodes`（侧栏「系统设置 → 探针节点」）  
> **数据表**：`probe_node`

---

## 页面目标

在 **租户级** 只读展示已注册的 **Chrome 扩展探针节点**：地区、支持平台、扩展版本、在线/离线（心跳）与实体状态（FR-113）。供运营确认 `browser-extension` 诊断任务可被调度。

**M1 范围**：
- ✅ 节点列表 + 在线状态（heartbeat ≤60s）+ 平台 Tags + 空态安装引导
- ✅ 合规 footnote（扩展最小权限）
- ❌ 节点编辑/删除/禁用 CRUD
- ❌ 今日任务量统计 / 图表（FR-113 子项 → M2）
- ❌ FR-116 adapter 后台 · FR-117 截图 · 节点限速配置 UI

**入口**：
- 侧栏「系统设置 → 探针节点」
- [diagnostic-detail.md](./diagnostic-detail.md) 探针进度 Tab 链入「管理探针节点 →」（P2）
- 创建诊断选 `browser-extension` 且无在线节点 → warning 链本页（P2）

**权限**：`tourgeo:probe:view`（租户管理员 / 运营）；与 billing 设置页同级。

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 面包屑：系统设置 / 探针节点                                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─ 概览 el-card shadow="hover" ────────────────────────────────────────────┐ │
│ │ 探针节点池    已注册 2 · 在线 1 · 离线 1                                  │ │
│ │ ℹ 浏览器扩展节点用于 browser-extension 探针；grounded-api 不依赖节点。      │ │
│ │ [查看安装说明]  [刷新]                                                     │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ el-table border v-loading ──────────────────────────────────────────────┐ │
│ │ 节点 | 地区 | 支持平台 | 扩展版本 | 在线 | 最后心跳 | 状态 | 操作          │ │
│ │ dev-probe-1 | us-east | [Perplexity] | 0.1.0 | ● 在线 | 10:32:05 | 启用 │ │
│ │ office-cn-2 | cn-east | [Perplexity] | 0.1.0 | ○ 离线 | 昨天 18:20 | 启用│ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 合规 el-alert type="info" show-icon :closable="false" ──────────────────┐ │
│ │ 扩展仅处理系统下发的诊断任务问题，不上传您浏览器中的其他对话内容。安装前   │ │
│ │ 请阅读授权说明。（PRD §16 · AGENTS.md §9）                                │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘

空态 (无节点):
┌─ el-empty ──────────────────────────────────────────────────────────────────┐
│  暂无探针节点                                                                │
│  1. 在 Chrome 安装「旅获 GEO 探针」扩展                                      │
│  2. 配置 API 地址与 Node Key（与租户一致）                                   │
│  3. 扩展启动后将自动注册并发送心跳                                           │
│  [查看安装说明]  [复制 Node Key 配置示例]                                    │
└─────────────────────────────────────────────────────────────────────────────┘

安装说明 (el-drawer 480px · 占位 M1):
┌─ 探针扩展安装说明 ──────────────────────────────────────── [×] │
│ 仓库路径：inbound-probe-extension/                           │
│ 环境变量：PLASMO_PUBLIC_API_BASE · PLASMO_PUBLIC_NODE_KEY    │
│ 开发加载：pnpm dev → Chrome 加载 unpacked extension            │
│ [关闭]  [打开 README] P2 → 仓库文档链接                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 概览卡片

| 项 | 计算 | 展示 |
|----|------|------|
| 已注册 | `list.length` | 数字 |
| 在线 | `online === true` 计数 | 绿色强调 |
| 离线 | 其余 | 灰色 |

**工具栏**（卡片内或表格上方）：

| 按钮 | M1 | 行为 |
|------|-----|------|
| 刷新 | ✅ | 重新 `GET .../probe/nodes` |
| 查看安装说明 | ✅ | 打开安装 drawer |
| 注册节点 | disabled | tooltip「由扩展自动注册 · FR-113」 |
| 禁用节点 | disabled | M2 |

---

## 表格列 ↔ DDL / API

| 列 | 宽度 | DDL 列 | API 字段 | 展示 |
|----|------|--------|----------|------|
| 节点 | min 140 | `node_key` | `nodeKey` | 主文案 mono；副行 `#id` 灰色小字 |
| 地区 | 100 | `region` | `region` | Tag 或 plain；如 `us-east` · `cn-east` |
| 支持平台 | min 160 | `platforms_json` | `platforms[]` | `el-tag` 组；见平台映射 |
| 扩展版本 | 90 | `extension_version` | `extensionVersion` | semver；空 `—` |
| 在线 | 90 | 计算 | `online` | 见在线指示器 |
| 最后心跳 | 160 | `last_heartbeat_at` | `lastHeartbeatAt` | `YYYY-MM-DD HH:mm:ss`；空「从未」 |
| 状态 | 80 | `status` | `status` | `entity_status` Tag |
| 操作 | fixed 100 | — | — | 「详情」只读 drawer（P2 可选 M1 省略，仅复制 nodeKey） |

**M2 可选列**：`tasksToday`（今日任务量 · FR-113）、`rateLimit`（来自 `rate_limit_json`）。

### 在线指示器（heartbeat ≤60s）

| 条件 | UI | 文案 |
|------|-----|------|
| `online === true` | 绿色圆点 `●` + `--tg-color-success` | 在线 |
| `online === false` 且有心跳 | 灰色圆点 `○` | 离线 |
| 无 `lastHeartbeatAt` | 灰色 | 未连接 |
| `status !== ACTIVE` | 不显示在线绿点 | 以实体状态为准 |

**计算**（后端推荐）：`online = status === 'ACTIVE' && lastHeartbeatAt >= now() - 60s`

**相对时间**（P2）：在线行副文案「刚刚」；离线 >5min 显示「5 分钟前」。

### `platforms_json` Tag

DB 存小写 slug；UI 映射（可复用/扩展 `diagnostic.ts`）：

| 值 | Tag 文案 | M1 扩展 |
|----|----------|---------|
| `perplexity` | Perplexity | ✅ 唯一实现平台 |
| `chatgpt` | ChatGPT | 预留 |
| `gemini` | Gemini | 预留 |
| `openai` | OpenAI | 预留 |
| `doubao` | 豆包 | FR-112 国内 · M2+ |
| *(其他)* | 原值 | fallback |

多个平台：`el-tag size="small"` 横向排列，max 3 可见 + `+N`。

### `entity_status` Tag（`probe_node.status`）

| DB 值 | Tag type | 中文 |
|-------|----------|------|
| `ACTIVE` | `success` | 启用 |
| `INACTIVE` | `info` | 停用 |
| `SUSPENDED` | `warning` | 暂停 |
| `ARCHIVED` | `info` | 已归档 |

M1 扩展注册默认 `ACTIVE`。

---

## 行操作（M1）

| 操作 | 行为 |
|------|------|
| 复制 Node Key | clipboard `nodeKey` + toast |
| 详情 | P2 drawer：`rate_limit_json`、创建时间（M1 可仅复制 key） |

无编辑、删除、强制下线。

---

## 筛选栏（P2 · M1 可省略）

| 字段 | 参数 | 说明 |
|------|------|------|
| 在线 | `online` | true / false / 全部 |
| 地区 | `region` | 模糊 |
| 平台 | `platform` | platforms_json 包含 |

M1 仅「刷新」+ 前端 sort（默认 `lastHeartbeatAt` desc）。

---

## 空态与安装引导

**条件**：`list.length === 0` 且无 loading。

| 元素 | 内容 |
|------|------|
| `el-empty` | description「暂无探针节点」 |
| 步骤列表 | 安装扩展 → 配置 Node Key → 自动注册心跳 |
| 主按钮 | 「查看安装说明」→ drawer |
| 次按钮 | 「复制配置示例」→ clipboard JSON |

**配置示例**（clipboard）：

```json
{
  "PLASMO_PUBLIC_API_BASE": "http://localhost:8080",
  "PLASMO_PUBLIC_NODE_KEY": "dev-probe-1"
}
```

**无在线节点 warning**（有节点但在线=0）：页顶 `el-alert type="warning"`「当前无在线节点，`browser-extension` 诊断任务将无法派发。」

---

## 合规 Footnote（必须展示）

固定 `el-alert type="info"` `:closable="false"`，表格下方：

> 扩展仅处理系统下发的诊断任务问题，不上传您浏览器中的其他对话内容。安装前请阅读授权说明。

链接 P2：`inbound-probe-extension/README.md` 或产品帮助页。

---

## 与诊断详情关系

| 页面 | 关系 |
|------|------|
| [diagnostic-detail.md](./diagnostic-detail.md) Tab「探针进度」 | 单次 run 的 `probe_task` 粒度 |
| 本页 | 租户级 **节点池** 健康度 |

`probe_task.probe_node_id` 可 join 展示节点 key（详情 Tab P2）；本页不展示任务列表。

---

## 空 / 加载 / 错误

| 状态 | UI |
|------|-----|
| 加载 | `v-loading` |
| API 403 | `el-result 403` |
| API 500 | `ElMessage.error` + 重试 |
| 心跳 stale | 行显示离线，不报错 |

---

## 响应式

| 断点 | 行为 |
|------|------|
| ≥1200px | 全列 |
| 768–1199px | 隐藏扩展版本 |
| &lt;768px | 节点、在线、状态、操作；平台 Tags 折叠为「2 平台」 |

---

## 菜单与路由

```text
父菜单：系统设置 (path: /settings)
  └─ 套餐与额度 (/settings/billing)
  └─ 探针节点 (/settings/probe-nodes, component: tourgeo/settings/probe-nodes/index)
  └─ 成员/模型/模板 — 若依或 M2
```

---

## API 依赖

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/probe/nodes` | JWT · 当前租户节点列表 |

**响应 Vo 建议**：

```json
{
  "summary": { "total": 2, "online": 1, "offline": 1 },
  "list": [
    {
      "id": 1,
      "nodeKey": "dev-probe-1",
      "region": "us-east",
      "platforms": ["perplexity"],
      "extensionVersion": "0.1.0",
      "status": "ACTIVE",
      "lastHeartbeatAt": "2026-07-05T10:32:05+08:00",
      "online": true,
      "createdAt": "2026-07-04T09:00:00+08:00"
    }
  ]
}
```

扩展侧（Admin 不调用）：`POST /api/v1/probe/nodes/register` + `X-Probe-Node-Key`。

---

## 字段 ↔ DDL 对照

| UI | DDL 列 | API camelCase |
|----|--------|---------------|
| ID | `id` | `id` |
| Node Key | `node_key` | `nodeKey` |
| 地区 | `region` | `region` |
| 平台 | `platforms_json` | `platforms` |
| 扩展版本 | `extension_version` | `extensionVersion` |
| 实体状态 | `status` | `status` |
| 最后心跳 | `last_heartbeat_at` | `lastHeartbeatAt` |
| 在线 | 计算 | `online` |
| 限速 | `rate_limit_json` | M2 `rateLimit` |

---

## M1 范围边界

| 包含 | 不包含 |
|------|--------|
| 只读列表 + 在线/离线 | 节点 CRUD |
| 平台 Tags + region | 今日任务量图表 |
| 空态 + 安装说明 drawer | adapter 编辑 FR-116 |
| 合规 alert | 截图存证 FR-117 |
| 复制 nodeKey / 配置示例 | Headless FR-118 |

---

## 实现参考

| 项 | 建议 |
|----|------|
| 视图 | `inbound-admin/src/views/tourgeo/settings/probe-nodes/index.vue` |
| API | `src/api/tourgeo/probe.ts` — `listProbeNodes()` |
| 常量 | `src/constants/probe.ts` — `PLATFORM_LABELS`, `PROBE_NODE_STATUS_META`, `ONLINE_META` |
| 版式 | [billing-settings.md](./billing-settings.md) 设置页卡片 + footnote |
| 状态 Tag | [diagnostic-detail.md](./diagnostic-detail.md) · `PROBE_TASK_STATUS_META` 色系 |
| Token | [tokens.md](../tokens.md) — success/muted 在线点 |

---

## 版本

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-05 | UI 设计 | EPIC-11 M1 初版 · FR-113 · ADR-20260705-18 |

**M2 交叉引用**：[probe-adapters.md](./probe-adapters.md)（FR-116 平台 Adapter 编辑 · 本页仍只读节点列表）
