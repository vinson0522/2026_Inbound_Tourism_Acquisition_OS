# HANDOFF | 技术总监 → 开发（Admin）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-05 | EPIC-11 M1 · FR-113 · ADR-20260705-18 |

## 上下文

Java 将提供 `GET /api/v1/probe/nodes`。UI 线框见 [→UI HANDOFF](2026-07-05-tech-director-to-ui-epic11-probe-nodes.md)（完成后读 `probe-nodes.md`）。

**相关文件**：
- `inbound-admin/src/views/tourgeo/settings/billing/` — 设置页版式参考
- `inbound-admin/src/router/index.ts` — 侧栏路由
- `inbound-admin/src/views/tourgeo/diagnostics/detail.vue` — probe status tag 参考

## 交付请求

Admin 探针节点只读列表页。

**验收标准**：
- [ ] `src/api/tourgeo/probe.ts` — `listProbeNodes()`
- [ ] `src/constants/probe.ts` — 在线状态 meta · 平台 label
- [ ] `src/views/tourgeo/settings/probe-nodes/index.vue` — 表格 + 空态 + 合规 footnote
- [ ] 路由 `/settings/probe-nodes` · 侧栏「系统设置 → 探针节点」
- [ ] 列：nodeKey · region · platforms tags · extensionVersion · online dot · lastHeartbeatAt · status
- [ ] `pnpm build:prod` ✅
- [ ] 依赖 UI 线框；线框未完成时可按 HANDOFF 列定义先实现

## Prompt

```
角色：开发 Admin。必读 billing 设置页结构、diagnostics detail 探针 Tab。
任务：/settings/probe-nodes 只读列表 · GET /api/v1/probe/nodes。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-05
- **结果摘要**：`/settings/probe-nodes` 只读列表 ✅ · 概览计数 · 表格 + 在线 dot · 空态/安装 drawer · 合规 alert · `pnpm build:prod` ✅
- **遗留**：详情 drawer P2；诊断创建页无在线节点 warning 链入（P2）
