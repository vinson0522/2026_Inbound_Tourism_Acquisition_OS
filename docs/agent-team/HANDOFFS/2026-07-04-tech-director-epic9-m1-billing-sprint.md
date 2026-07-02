# EPIC-9 M1 套餐计费 Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-04 |
| **优先级** | High |
| **关联** | EPIC-9 · **FR-804** · ADR-20260704-17 |
| **前置** | EPIC-1~8 M1 闭环 ✅ · C13 `71c374d` |

## 目标（M1 MVP）

**查询套餐额度 + 超额拦截（402）+ Admin 用量展示**

| 范围 | M1 做 | M1 不做 |
|------|-------|---------|
| FR-804 | `GET .../subscription` · `quota_json`/`used_json` · 创建前扣额校验 | 支付网关/Stripe |
| 拦截点 | 建项目 · 创建诊断 · AI 生成（关键词/内容/落地页）· 周报 | 全 API AOP 覆盖 |
| Admin | `/settings/billing` 额度卡片 + 进度条 | 套餐 CRUD 后台 |
| 存储 | PG `subscription` + `used_json` 原子更新 | Redis 计数（M2） |
| FR-802/806/805 | — | 模型配置/审计/白标 |

## 任务拆分

| # | 角色 | HANDOFF | 依赖 | 验收 |
|---|------|---------|------|------|
| **1** | **UI 设计** | [→ 计费设置线框](2026-07-04-tech-director-to-ui-epic9-billing-settings.md) | — | `billing-settings.md` |
| **2** | **开发 Java** | [→ billing API + QuotaService](2026-07-04-tech-director-to-dev-java-epic9-billing.md) | DDL + seed | GET + 402 拦截 |
| **3** | **开发 Admin** | [→ 计费页](2026-07-04-tech-director-to-dev-admin-epic9-billing.md) | #1+#2 | build |
| — | 总览 | Sprint 索引 | [EPIC-9 M1](2026-07-04-tech-director-epic9-m1-billing-sprint.md) | — |

**无 Python / 无运维 M1**（复用 demo `subscription` seed）。

## quota_json 键（M1）

| 键 | 拦截动作 |
|----|----------|
| `projects` | `POST .../projects` |
| `diagnostics_per_month` | `POST .../diagnostics` |
| `keywords_per_month` | `POST .../keywords/generate` |
| `content_per_month` | `POST .../content-tasks/.../generate` |
| `landing_pages_per_month` | `POST .../landing-pages/.../generate` |
| `reports_per_month` | `POST .../reports/weekly` |

超额：`ServiceException` → HTTP **402** · code `40201` · message 含升级提示。

## 窗口 Prompt 摘要

| 角色 | Prompt |
|------|--------|
| UI | `billing-settings.md 线框 · 额度 6 项 + 周期 + 超额提示` |
| Java | `SubscriptionController + QuotaService + 5 拦截点 + smoke` |
| Admin | `/settings/billing 只读用量页` |

## 完成后

- smoke：`deploy/scripts/test_billing_quota.py`
- commit **C14**：`feat(core,admin): EPIC-9 M1 subscription quota and overage guard`
- 下一 Sprint：**EPIC-11 M1** 浏览器探针 → [Sprint 索引](2026-07-05-tech-director-epic11-m1-probe-sprint.md)
