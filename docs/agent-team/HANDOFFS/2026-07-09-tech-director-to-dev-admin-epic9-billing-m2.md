# HANDOFF | 技术总监 → 开发（Admin）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-09 | EPIC-9 M2 · FR-804 扩展 · ADR-20260709-23 |

## 交付请求

- [ ] `/settings/billing` — 「编辑套餐」按钮 · drawer：plan 下拉 · 6 quota 表单项 · 保存调 `PUT .../billing/subscription`
- [ ] 开发/管理员可见「重置本周期用量」— 确认 dialog · 调 internal period-reset 或公开 admin-only endpoint（与 Java 对齐）
- [ ] 保存后刷新用量卡片 · toast
- [ ] `pnpm build:prod` ✅

## Prompt

```
角色：开发 Admin。必读 settings/billing/index.vue、billing-settings 线框 §M2、HANDOFF 2026-07-09-tech-director-to-dev-admin-epic9-billing-m2.md。
任务：billing M2 编辑+重置 UI · build:prod。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-09
- **结果摘要**：编辑 drawer 520px · 重置 confirm dialog · footnote · `pnpm build:prod` ✅ · 依赖 Java `POST /period-reset` 租户端点
- **遗留**：无
