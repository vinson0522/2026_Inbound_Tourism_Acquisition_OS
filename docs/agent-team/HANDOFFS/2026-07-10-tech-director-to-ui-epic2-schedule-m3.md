# HANDOFF | 技术总监 → UI 设计

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | UI 设计 | 2026-07-10 | EPIC-2 M3 · FR-109 · ADR-20260710-26 |

## 交付请求

- [x] **`docs/design/wireframes/diagnostics-list.md` §M3 增量**：「定时计划」Tab — enabled 开关 · 频率 WEEKLY/MONTHLY · 探针/平台/采样（复用新建表单字段）· 下次执行 · 最近 run 链接
- [x] footnote：超额跳过 · 不承诺排名
- [x] `2026-07-10-ui-to-developer-diagnostic-schedule-m3.md`

## Prompt

```
角色：UI 设计。必读 diagnostics-list、FR-109、ADR-26。
任务：定时诊断线框 + HANDOFF 开发。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-10
- **结果摘要**：`diagnostics-list.md` §M3 页级 Tab「定时计划」· enabled/WEEKLY/MONTHLY · 复用新建 drawer 参数字段 · nextRunAt/lastTriggeredAt/lastRunId 只读区 · 超额 skip + 无通知 footnote；`2026-07-10-ui-to-developer-diagnostic-schedule-m3.md`
- **遗留**：`?tab=schedule` 深链 P2 · 表单组件抽取 P2 · 超额 Admin 内告警（仅日志）
