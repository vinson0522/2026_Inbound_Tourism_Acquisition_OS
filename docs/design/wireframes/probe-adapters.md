# 线框：系统设置 · 平台 Adapter（EPIC-11 M2）

> **PRD**：§7.6.4 浏览器扩展探针 · **FR-116** 平台 Adapter 配置  
> **EPIC**：EPIC-11 M2 · **ADR-20260709-22**  
> **路由**：`/settings/probe-adapters`（侧栏「系统设置 → 平台 Adapter」）  
> **数据表**：`platform_adapter`（`dom_selectors_json` · `api_patterns_json` · `parse_rules_json`）

---

## 页面目标

在 **租户级** 配置各 AI 平台的 **DOM 选择器、接口特征、解析规则**，供 Chrome 扩展 poll 拉取并热更新（FR-116）。平台改版时运营可改 JSON 而无需发版全系统。

**M2 范围**：
- ✅ 只读列表 + **编辑 drawer**（platform · version · enabled · 三块 JSON 编辑器）
- ✅ 保存后扩展下次 poll `/probe/adapters` 生效
- ❌ 版本灰度 / 多版本并存选择 · 国内平台 adapter · 节点 CRUD
- ❌ FR-117 截图 · FR-118 Headless · Adapter 可视化表单（M2 纯 JSON）

**入口**：
- 侧栏「系统设置 → 平台 Adapter」
- [probe-nodes.md](./probe-nodes.md) 概览卡片 link「配置 Adapter →」（P2）
- [diagnostic-detail.md](./diagnostic-detail.md) 校准 Tab footnote「检查 Adapter 配置 →」

**权限**：`tourgeo:probe:adapter:edit`（编辑）；`tourgeo:probe:view`（只读列表）

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 面包屑：系统设置 / 平台 Adapter                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─ 概览 el-card shadow="hover" ────────────────────────────────────────────┐ │
│ │ 平台 Adapter    已配置 2 · 启用 2 · 停用 0                                │ │
│ │ ℹ 扩展探针每 30s poll 拉取最新 enabled adapter；grounded-api 不读取本配置。  │ │
│ │ [管理探针节点 →]  [刷新]                                                   │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ el-table border v-loading ──────────────────────────────────────────────┐ │
│ │ 平台 | 版本 | 状态 | 更新时间 | 操作                                        │ │
│ │ Perplexity | 1.0 | [启用] | 2026-07-09 10:20 | [编辑] [复制 JSON]          │ │
│ │ ChatGPT    | 1.0 | [启用] | 2026-07-09 09:00 | [编辑] [复制 JSON]          │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 合规 el-alert type="info" show-icon :closable="false" ──────────────────┐ │
│ │ 修改解析规则可能影响 citations/品牌识别准确性。保存前请在测试环境验证；     │ │
│ │ 错误配置将导致 browser-extension 子任务 FAILED。（AGENTS.md §9.3）         │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘

编辑 Adapter (el-drawer 640px · FR-116):
┌─ 编辑平台 Adapter · Perplexity ─────────────────────────── [×] │
│ 平台        perplexity  (只读 mono)                              │
│ 版本        1.0  (只读 · M2 upsert 按 platform 覆盖当前版本)    │
│ 启用        [el-switch enabled]                                  │
│ ── DOM 选择器 (dom_selectors_json) ──                           │
│ [ textarea monospace · min-height 120px · JSON ]  [格式化] [校验]│
│ 示例: {"input":"textarea","submit":"button[type=submit]"}        │
│ ── 接口特征 (api_patterns_json) ──                              │
│ [ textarea · min-height 100px ]  [格式化] [校验]                 │
│ ── 解析规则 (parse_rules_json) ──                               │
│ [ textarea · min-height 160px ]  [格式化] [校验]                 │
│ 示例: {"citationsPath":"citations","answerPath":"..."}           │
│ ℹ 保存后扩展节点将在下次 poll（≤30s）获取新版本。                 │
│                              [取消]  [保存] primary              │
└──────────────────────────────────────────────────────────────────┘
```

**响应式**：&lt;768px drawer 100% 宽；JSON 区 `font-size: 12px`。

---

## 概览卡片

| 项 | 计算 | 展示 |
|----|------|------|
| 已配置 | `list.length` | 数字 |
| 启用 | `enabled === true` | 绿色 |
| 停用 | `enabled === false` | 灰色 |

| 按钮 | 行为 |
|------|------|
| 刷新 | `GET .../platform-adapters` |
| 管理探针节点 → | `/settings/probe-nodes` |
| 新增平台 | M2 **disabled** · tooltip「M2 由 seed 预置 perplexity/chatgpt · 新增平台 M3+」 |

---

## 表格列 ↔ DDL / API

| 列 | 宽度 | DDL 列 | API 字段 | 展示 |
|----|------|--------|----------|------|
| 平台 | min 140 | `platform` | `platform` | Tag + 中文映射（同 probe-nodes） |
| 版本 | 90 | `version` | `version` | semver mono |
| 状态 | 90 | `enabled` | `enabled` | `el-switch` **只读** 或 Tag；编辑在 drawer |
| 更新时间 | 160 | `updated_at` | `updatedAt` | `YYYY-MM-DD HH:mm` |
| 操作 | fixed 160 | — | — | 「编辑」「复制 JSON」 |

**M2 列表不展示** JSON 正文（过长）；编辑 drawer 内三块 textarea。

### 平台 Tag 映射

与 [probe-nodes.md](./probe-nodes.md) **一致**：

| 值 | Tag 文案 | M2 |
|----|----------|-----|
| `perplexity` | Perplexity | ✅ seed |
| `chatgpt` | ChatGPT | ✅ M2 seed |
| `gemini` | Gemini | 预留 disabled |
| `doubao` | 豆包 | 预留 |

### `enabled` 展示

| 值 | Tag type | 中文 |
|----|----------|------|
| `true` | `success` | 启用 |
| `false` | `info` | 停用 |

停用行：表格行浅灰底（可选）；扩展 poll 仍可见但不应下发给节点（后端 filter `enabled=true`）。

---

## 编辑 Drawer 字段 ↔ API

| UI 标签 | DDL / JSON 键 | 组件 | 必填 | 说明 |
|---------|---------------|------|:----:|------|
| 平台 | `platform` | 只读文本 | — | drawer 标题 `{PLATFORM_LABELS}` |
| 版本 | `version` | 只读 | — | M2 固定展示；PUT 可带同 version upsert |
| 启用 | `enabled` | `el-switch` | — | 默认 true |
| DOM 选择器 | `dom_selectors_json` | `el-input` textarea monospace | ✅ | 合法 JSON object |
| 接口特征 | `api_patterns_json` | textarea | ✅ | 合法 JSON object |
| 解析规则 | `parse_rules_json` | textarea | ✅ | 合法 JSON object；与扩展 `adapters/*.ts` 对齐 |

**保存**：`PUT /api/v1/settings/platform-adapters/{platform}`

**请求体示例**：

```json
{
  "version": "1.0",
  "enabled": true,
  "domSelectorsJson": {
    "input": "textarea",
    "submit": "button[type=submit]"
  },
  "apiPatternsJson": {
    "chatApi": "/api/chat"
  },
  "parseRulesJson": {
    "citationsPath": "citations",
    "answerPath": "message.content"
  }
}
```

**校验（前端）**：
- 三块 JSON `JSON.parse` 成功且为 object（非 array）
- 失败：字段下 `el-form-item error`「JSON 格式无效」
- 「格式化」按钮：`JSON.stringify(JSON.parse(v), null, 2)`

**成功**：toast「Adapter 已保存」· 关闭 drawer · 刷新列表 · `updatedAt` 更新。

**只读角色**：drawer 打开但 textarea disabled · 无保存按钮。

---

## 行操作

| 操作 | 行为 |
|------|------|
| 编辑 | 打开 drawer · `GET .../platform-adapters/{platform}` 填充表单 |
| 复制 JSON | clipboard 合并三块 JSON 或仅 `parseRulesJson` · toast |

无删除（软删 M3+）；无「新建平台」M2。

---

## 空态

**条件**：`list.length === 0`（新租户无 seed 时罕见）

| 元素 | 内容 |
|------|------|
| `el-empty` | 「暂无平台 Adapter」 |
| 说明 | 请联系管理员初始化 seed 或运行 DDL seed |
| 按钮 | 「刷新」 |

---

## 与探针节点 / 扩展关系

| 组件 | 关系 |
|------|------|
| 扩展 background poll | `GET /api/v1/probe/adapters`（内网/Node Key）· **非 Admin JWT 路径** |
| Admin 本页 | `GET/PUT /api/v1/settings/platform-adapters` · 租户 JWT |
| [probe-nodes.md](./probe-nodes.md) | 节点在线与否不影响 adapter 编辑；无在线节点时校准仍可读配置 |

**热更新时序**：保存 → DB 更新 → 扩展 poll（≤30s）→ content script 用新 parse_rules。

---

## 字段 ↔ DDL 完整对照

| UI | DDL 列 | API camelCase |
|----|--------|---------------|
| ID | `id` | `id` |
| 租户 | `tenant_id` | — |
| 平台 | `platform` | `platform` |
| 版本 | `version` | `version` |
| DOM | `dom_selectors_json` | `domSelectorsJson` |
| 接口 | `api_patterns_json` | `apiPatternsJson` |
| 解析 | `parse_rules_json` | `parseRulesJson` |
| 启用 | `enabled` | `enabled` |
| 更新时间 | `updated_at` | `updatedAt` |

---

## 空 / 加载 / 错误

| 状态 | UI |
|------|-----|
| 加载 | `v-loading` |
| GET 失败 | `ElMessage.error` + 重试 |
| PUT 409/400 | drawer 内 error message |
| 403 | `el-result` |

---

## 菜单与路由

```text
父菜单：系统设置 (path: /settings)
  ├─ 探针节点 (/settings/probe-nodes)
  └─ 平台 Adapter (/settings/probe-adapters, component: tourgeo/settings/probe-adapters/index)
```

---

## API 依赖

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/settings/platform-adapters` | 租户 adapter 列表 |
| GET | `/api/v1/settings/platform-adapters/{platform}` | 单条详情（编辑前拉取） |
| PUT | `/api/v1/settings/platform-adapters/{platform}` | upsert |

**List 响应示例**：

```json
{
  "list": [
    {
      "id": 1,
      "platform": "perplexity",
      "version": "1.0",
      "enabled": true,
      "domSelectorsJson": { "input": "textarea" },
      "apiPatternsJson": { "chatApi": "/api/chat" },
      "parseRulesJson": { "citationsPath": "citations" },
      "updatedAt": "2026-07-09T10:20:00+08:00"
    }
  ]
}
```

扩展侧（Admin 不调用）：`GET /api/v1/probe/adapters?platform=perplexity`（M1 已有）。

---

## 实现参考

| 项 | 建议 |
|----|------|
| 视图 | `inbound-admin/src/views/tourgeo/settings/probe-adapters/index.vue` |
| API | `src/api/tourgeo/probe.ts` — `listPlatformAdapters` · `getPlatformAdapter` · `savePlatformAdapter` |
| 常量 | 复用 `src/constants/probe.ts` — `PLATFORM_LABELS` |
| 版式 | [probe-nodes.md](./probe-nodes.md) 概览卡片 + table + footnote |
| JSON 编辑 | M2 用 `el-input type="textarea"` + 格式化/校验；P2 可接 Monaco |

---

## 版本

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-09 | UI 设计 | EPIC-11 M2 初版 · FR-116 · ADR-20260709-22 |
