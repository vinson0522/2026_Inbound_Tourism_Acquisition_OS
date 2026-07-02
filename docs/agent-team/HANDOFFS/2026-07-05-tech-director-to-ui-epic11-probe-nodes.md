# HANDOFF | 技术总监 → UI 设计

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | UI 设计 | 2026-07-05 | EPIC-11 M1 · FR-113 · ADR-20260705-18 |

## 上下文

EPIC-9 M1 计费已完成待 C14。下一 Sprint 为 **EPIC-11 浏览器扩展探针 M1**：运营在 Admin 查看已注册探针节点（地区、平台、在线状态）。诊断详情页「探针进度」Tab 已存在，本 Sprint **新增独立节点管理页**。

**相关文件**：
- `inbound-admin/src/views/tourgeo/diagnostics/detail.vue` — 探针进度 Tab（参考状态 tag 样式）
- `PRD_商业化版_V2.0.md` §7.6.4 · FR-113
- `database/ddl/001_schema.sql` — `probe_node` 字段

## 交付请求

产出线框 **`docs/design/wireframes/probe-nodes.md`**，并写 UI→开发 HANDOFF。

**验收标准**：
- [x] 路由建议：`/settings/probe-nodes`（系统设置分组，与 billing 并列）
- [x] 列表列：节点 ID/node_key · 地区 region · 支持平台（tags）· 扩展版本 · 在线状态（heartbeat ≤60s 在线）· 最后心跳时间 · 状态 ACTIVE/INACTIVE
- [x] 空态：无节点时引导「安装 Chrome 扩展并登录同一租户账号」
- [x] 只读 M1：无编辑/删除；可选「复制安装说明」链接占位
- [x] 合规提示 footnote：扩展仅处理下发任务，不上传其他会话
- [x] 完成后新建 `2026-07-05-ui-to-developer-probe-nodes.md`

## Prompt

```
角色：UI 设计。必读 FR-113、probe_node DDL、billing-settings.md 版式参考。
任务：probe-nodes.md 线框 · 在线/离线状态 · 平台 tags · 空态引导。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-05
- **结果摘要**：`probe-nodes.md` 只读节点列表 + 在线/离线 + 空态安装引导 + 合规 footnote；HANDOFF 开发
- **遗留**：今日任务量 M2 · 节点 CRUD · 诊断详情链入 P2
