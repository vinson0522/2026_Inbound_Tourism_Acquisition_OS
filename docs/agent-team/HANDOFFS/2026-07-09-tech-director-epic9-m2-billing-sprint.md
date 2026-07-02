# EPIC-9 M2 套餐 CRUD + 周期重置 Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-09 |
| **优先级** | High |
| **关联** | EPIC-9 · **FR-804 扩展** · ADR-20260709-23 |
| **前置** | EPIC-9 M1 ✅ C14 `f23e539` · EPIC-11 M2 C19 待入库 |

## 商业决策（完整版路线图 #4）

M1 只读用量 + 402 拦截已跑通；客户/服务商运营需 **切换套餐模板**、**调整 quota 上限**、**周期到期自动重置 used_json**，否则演示环境额度会永久耗尽。

## 目标（M2 MVP）

| 范围 | M2 做 | M2 不做 |
|------|-------|---------|
| 套餐 CRUD | Admin `PUT /api/v1/settings/billing/subscription` — 切换 `plan_code` · 覆盖 `quota_json` · 更新 `period_start/end` | 支付网关/Stripe · 发票 |
| 周期重置 | `@Scheduled` 或 `POST /api/v1/internal/billing/period-reset` — 月度 `used_json` 键归零 · 推进 `period_start/end` | Redis 计数 · 多订阅并存 |
| Admin | `/settings/billing` 增加「编辑套餐」drawer · 计划下拉 · quota 表单 · 手动「重置本周期用量」（仅 dev/admin） | FR-802 模型 Key 配置 |
| 审计 | 写 `subscription` 变更日志到应用 log（结构化 JSON） | FR-806 审计中心 UI |

## 任务拆分

| # | 角色 | HANDOFF | 依赖 | 验收 |
|---|------|---------|------|------|
| **1** | **UI 设计** | [→ billing M2 线框](2026-07-09-tech-director-to-ui-epic9-billing-m2.md) | — | `billing-settings.md` §M2 |
| **2** | **开发 Java** | [→ billing M2 API](2026-07-09-tech-director-to-dev-java-epic9-billing-m2.md) | — | smoke |
| **3** | **开发 Admin** | [→ billing M2 UI](2026-07-09-tech-director-to-dev-admin-epic9-billing-m2.md) | #1+#2 | build |

**无 Python / 无运维 M2**

## 窗口派发（复制到各 Cursor 窗口）

| # | 窗口 | HANDOFF | 激活 Prompt（Custom Instructions 首行） |
|---|------|---------|----------------------------------------|
| **1** | **UI 设计** | [→ 线框](2026-07-09-tech-director-to-ui-epic9-billing-m2.md) | `角色：UI 设计。必读 billing-settings.md FR-804、SubscriptionVo、ADR-23、HANDOFF 2026-07-09-tech-director-to-ui-epic9-billing-m2.md。任务：billing-settings §M2 套餐编辑 drawer + 周期重置说明 + UI→开发 HANDOFF。` |
| **2** | **开发 Java** | [→ Java API](2026-07-09-tech-director-to-dev-java-epic9-billing-m2.md) | `角色：开发 Java。必读 SubscriptionServiceImpl、QuotaServiceImpl、002_seed_demo.sql subscription、ADR-23、HANDOFF 2026-07-09-tech-director-to-dev-java-epic9-billing-m2.md。任务：PUT billing/subscription + period reset job/endpoint + test_billing_period_reset.py。` |
| **3** | **开发 Admin** | [→ Admin UI](2026-07-09-tech-director-to-dev-admin-epic9-billing-m2.md) | `角色：开发 Admin。必读 settings/billing/index.vue、billing-settings 线框 §M2、HANDOFF 2026-07-09-tech-director-to-dev-admin-epic9-billing-m2.md。任务：套餐编辑 drawer + 重置用量按钮 · build:prod。` |

**并行**：#1 与 #2 可并行；#3 依赖 #1+#2。

## 完成后

- smoke：`deploy/scripts/test_billing_period_reset.py`
- commit **C20**：`feat(core,admin): EPIC-9 M2 subscription CRUD and period reset`
- **技术总监签核（2026-07-09）**：✅ smoke billing reset · Admin build · **C20 待入库**
- 下一 Sprint：**EPIC-5 M1** 爆款拆解 → [Sprint 索引](2026-07-09-tech-director-epic5-m1-viral-sprint.md)
