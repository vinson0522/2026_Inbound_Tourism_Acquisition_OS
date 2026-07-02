# HANDOFF | 技术总监 → UI 设计

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | UI 设计 | 2026-07-04 | EPIC-9 M1 · FR-804 · [Sprint](2026-07-04-tech-director-epic9-m1-billing-sprint.md) |

## 交付

- [x] `docs/design/wireframes/billing-settings.md`
- [x] 路由 `/settings/billing`（侧栏「系统设置 → 套餐与额度」）
- [x] 展示：`plan_code` · 计费周期 · 6 项 quota/used 进度条 · 超额 alert 样式
- [x] M1 disabled：升级购买、发票、套餐切换
- [x] HANDOFF → `2026-07-04-ui-to-developer-billing-settings.md`

## Prompt

```
角色：UI 设计。必读 PRD FR-804、subscription DDL、002_seed_demo quota_json 示例。
任务：billing-settings.md — 只读用量仪表盘；参考 reports-list 卡片布局。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-04
- **结果摘要**：`billing-settings.md` 套餐概览 + 6 额度进度条 + 超额/预警 alert；M1 无购买按钮
- **遗留**：在线升级 M2；周期自动重置 Job；工作台额度链入 P2
