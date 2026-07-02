# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-04 | EPIC-9 M1 · FR-804 · ADR-20260704-17 |

## 交付

- [x] `Subscription` 实体 + Mapper（表 `subscription` 已存在）
- [x] `GET /api/v1/settings/billing` 或 `GET /api/v1/tenants/current/subscription` — 当前租户 ACTIVE 订阅 + quota/used/period
- [x] `QuotaService`：
  - `checkAndConsume(tenantId, QuotaType, amount)` — 事务内读 subscription → 比较 → 更新 `used_json`
  - 无 ACTIVE 订阅：M1 放行 + warn log（dev 友好）或默认 TRIAL 额度
- [x] **拦截点**（超额抛 402）：
  - `CustomerProjectServiceImpl.insert` → `projects`
  - `DiagnosticRunServiceImpl` 创建 run → `diagnostics_per_month`
  - `KeywordOpportunityServiceImpl.generate` → `keywords_per_month`
  - `ContentTaskServiceImpl.generate` → `content_per_month`
  - `LandingPageServiceImpl.generate` → `landing_pages_per_month`
  - `ReportServiceImpl.createWeeklyReport` → `reports_per_month`
- [x] 统一错误：`code=40201` · message「套餐额度不足，请升级」
- [x] `tenant.excludes` += `subscription`（若 MyBatis 租户插件影响）
- [x] smoke：`deploy/scripts/test_billing_quota.py` — 将某 quota 设为 used=quota → 下次 generate 期望 402

## quota/used JSON 键

与 PRD §13 一致：`projects`, `diagnostics_per_month`, `keywords_per_month`, `content_per_month`, `landing_pages_per_month`, `reports_per_month`

## Prompt

```
角色：开发 Java。必读 subscription DDL、002_seed_demo、AGENTS.md 402 约定。
任务：QuotaService + GET subscription + 6 拦截点。不调 Python。
模块：新建 ruoyi-billing 或并入 ruoyi-project（M1 最小：ruoyi-project 子包 billing）。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-02
- **结果摘要**：
  - `ruoyi-project/billing/`：`QuotaType`、`QuotaExceededException`（40201）、`BillingExceptionHandler`（HTTP 402）
  - `Subscription` 实体 + `PgSubscriptionStatusTypeHandler` + `SubscriptionMapper`
  - `GET /api/v1/settings/billing` → 6 quota 项（used/limit/percentage/status）
  - `QuotaService.checkAndConsume` — 无 ACTIVE 订阅 M1 放行 + warn（ADR-17）
  - 6 拦截点：`CustomerProjectServiceImpl.insertByBo`、`DiagnosticRunServiceImpl.createRun`、`KeywordOpportunityServiceImpl.generateKeywords`、`ContentTaskServiceImpl.generate`、`LandingPageServiceImpl.generate`、`ReportServiceImpl.createWeeklyReport`
  - `tenant.excludes` += `subscription`
  - smoke：`deploy/scripts/test_billing_quota.py` ✅（GET billing + 耗尽 `reports_per_month` → POST weekly → 402/40201）
- **遗留**：Admin `/settings/billing` UI（→ 开发 Admin HANDOFF）；C14 commit；M2 支付/套餐 CRUD
