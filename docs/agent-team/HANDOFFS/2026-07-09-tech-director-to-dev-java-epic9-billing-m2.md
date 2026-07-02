# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-09 | EPIC-9 M2 · FR-804 扩展 · ADR-20260709-23 |

## 交付请求

- [ ] `PUT /api/v1/settings/billing/subscription` — body: `planCode` · `quotaJson`（6 键）· 可选 `periodStart`/`periodEnd` · 校验 plan 白名单 · 租户隔离
- [ ] 周期重置：`SubscriptionPeriodResetJob`（`@Scheduled` 每日 02:00）— 当 `period_end < today` 时推进周期并重置 `used_json` 月度键；保留 `projects` 累计
- [ ] 内部调试：`POST /api/v1/internal/billing/period-reset?tenantId=`（Bearer internal token）— 供 smoke 手动触发
- [ ] smoke：`deploy/scripts/test_billing_period_reset.py` — PUT 改 quota → 消费 → reset → used 归零

## Prompt

```
角色：开发 Java。必读 QuotaServiceImpl、Subscription、002_seed_demo.sql、ADR-23。
任务：subscription PUT + period reset + smoke。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-09
- **结果摘要**：`PUT /api/v1/settings/billing/subscription` · `SubscriptionPeriodResetJob`（02:00）· `POST /api/v1/internal/billing/period-reset` · `test_billing_period_reset.py` ✅ · mvn install + Java 重启
- **遗留**：Admin 编辑 drawer 待 #3 窗口
