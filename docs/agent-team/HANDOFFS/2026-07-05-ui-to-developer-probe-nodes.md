# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-07-05 |
| **优先级** | High |
| **关联** | EPIC-11 M1 · FR-113 · [技术总监 → UI](2026-07-05-tech-director-to-ui-epic11-probe-nodes.md) · ADR-20260705-18 |

## 上下文

**当前状态**：grounded-api 诊断已跑通；Admin 可选 `browser-extension` 但无节点列表。EPIC-11 M1 需运营查看扩展注册节点。

**相关文件**：
- `docs/design/wireframes/probe-nodes.md` — 线框 + DDL/API + 在线规则
- `docs/design/wireframes/billing-settings.md` — 系统设置页版式
- `docs/design/wireframes/diagnostic-detail.md` — 探针 Tab Tag 参考
- `database/ddl/001_schema.sql` — `probe_node`
- Java HANDOFF：`2026-07-05-tech-director-to-dev-java-epic11-probe.md`

**约束**：
- 租户级只读；无 CRUD
- 在线 = heartbeat 60s 内 + ACTIVE
- M1 无今日任务量列

## 交付请求

**需要什么**：Admin `/settings/probe-nodes` 探针节点只读列表。

**验收标准**：
- [ ] 路由 `/settings/probe-nodes`；菜单「系统设置 → 探针节点」
- [ ] 概览：已注册 / 在线 / 离线计数
- [ ] 表格：nodeKey、region、platforms Tags、extensionVersion、online 指示器、lastHeartbeatAt、status
- [ ] 空态 + 安装步骤 + 「查看安装说明」drawer
- [ ] 无在线节点时 warning alert
- [ ] 合规 info alert footnote（扩展最小权限）
- [ ] 复制 nodeKey / 配置示例
- [ ] 注册/禁用 disabled + tooltip
- [ ] `pnpm build:prod` ✅

## 后端依赖

- [ ] `GET /api/v1/probe/nodes` — 含 `online` 计算 + 可选 `summary`

## 质量 / 证据

**必须提供**：有节点列表截图；空态截图；在线/离线 dot 对比

---

## Done（由 To 角色填写）

- **完成时间**：2026-07-05
- **结果摘要**：Admin `/settings/probe-nodes` 已实现 · 对齐 billing 设置页结构
- **遗留**：截图证据待浏览器走查
