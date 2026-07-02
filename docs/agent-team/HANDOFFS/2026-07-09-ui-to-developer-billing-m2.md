# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-07-09 |
| **优先级** | High |
| **关联** | EPIC-9 M2 · FR-804 扩展 · [技术总监 → UI](2026-07-09-tech-director-to-ui-epic9-billing-m2.md) · ADR-20260709-23 |

## 上下文

**当前状态**：M1 `/settings/billing` 只读 6 quota + 超额 alert + 402 全局提示 ✅。M2 需运营切换 plan、调 quota、周期重置演示。

**相关文件**：
- `docs/design/wireframes/billing-settings.md` — **§M2 增量**（编辑 drawer · 重置 dialog · 周期 footnote）
- M1 实现：`inbound-admin/src/views/tourgeo/settings/billing/index.vue`
- Vo：`SubscriptionVo` · `QuotaItemVo`（GET 响应已有）
- Java HANDOFF：`2026-07-09-tech-director-to-dev-java-epic9-billing-m2.md`
- Admin HANDOFF：`2026-07-09-tech-director-to-dev-admin-epic9-billing-m2.md`

**约束**：
- M2 无 Stripe/发票/FR-802/FR-806 UI
- 重置仅月度 5 键；`projects` 不重置（ADR-23）
- 升级/购买/发票按钮仍 disabled
- 402 文案与 M1 一致

## 交付请求

**需要什么**：在现有 billing 页叠加套餐编辑 + 手动周期重置。

**验收标准**：
- [ ] 套餐概览「编辑套餐」→ drawer 520px（`billing:edit`）
- [ ] 表单：planCode 下拉 · 6×`el-input-number` quota · periodStart/periodEnd
- [ ] 「套用模板默认额度」按钮 · `PLAN_QUOTA_PRESETS` 常量
- [ ] 保存 → `PUT /api/v1/settings/billing/subscription` → toast · 刷新进度条
- [ ] dirty 离开 confirm
- [ ] 额度卡「重置本周期用量」→ confirm dialog（`billing:reset`）
- [ ] 重置 → `POST .../billing/period-reset`（或与 Java 对齐的 admin endpoint）→ 月度 used 归零
- [ ] 页底追加「计费周期与重置」footnote（Job 02:00 · projects 例外）
- [ ] 只读角色隐藏编辑/重置
- [ ] `pnpm build:prod` ✅

## 后端依赖

- [ ] `PUT /api/v1/settings/billing/subscription` — planCode · quotaJson · periodStart/End
- [ ] `SubscriptionPeriodResetJob` 或 `POST .../period-reset` — 月度 used 归零 + 可选 period 推进
- [ ] plan 白名单校验
- [ ] smoke `test_billing_period_reset.py`

## API 封装建议（`billing.ts`）

```typescript
updateSubscription(body: SubscriptionUpdateDto)
resetBillingPeriod()
```

## 质量 / 证据

**必须提供**：
- 编辑 drawer 改 quota 保存后进度条更新截图
- 重置 dialog 确认后月度 used 归零截图
- 只读角色无编辑/重置按钮截图
- footnote「计费周期与重置」可见截图

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
