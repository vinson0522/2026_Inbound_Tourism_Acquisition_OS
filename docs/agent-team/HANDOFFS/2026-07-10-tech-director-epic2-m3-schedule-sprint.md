# EPIC-2 M3 定时诊断 Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-10 |
| **优先级** | High |
| **关联** | EPIC-2 · **FR-109** · ADR-20260710-26 |
| **前置** | EPIC-2 M1/M2 ✅ · C22 `18b17c0` |

## 商业决策（完整版路线图 #7 · 最后一项 Sprint）

客户需 **按周/月自动 GEO 诊断**，减少手工创建；M3 用 Spring `@Scheduled` 复用现有 `createRun`，不做 XXL-Job 集群。

## 目标（M3 MVP）

| 范围 | M3 做 | M3 不做 |
|------|-------|---------|
| FR-109 | DDL `diagnostic_schedule` · CRUD · `@Scheduled` 每小时触发 due 项 → `createRun` · Admin 列表/编辑 | 邮件/企微通知 · XXL-Job · cron 表达式自由编辑 |
| 频率 | `WEEKLY` · `MONTHLY` · `enabled` 开关 | 自定义 cron · 多 schedule 冲突合并 |
| Admin | 诊断列表「定时计划」Tab 或侧栏入口 · 下次执行时间 · 最近 run 链入 | 项目级多计划 |
| 配额 | 触发前 `QuotaService.checkAndConsume(diagnostics_per_month)` | 超额静默跳过+日志 |

## 任务拆分

| # | 角色 | HANDOFF | 依赖 | 验收 |
|---|------|---------|------|------|
| **1** | **UI 设计** | [→ 定时诊断线框](2026-07-10-tech-director-to-ui-epic2-schedule-m3.md) | — | `diagnostics-list.md` §M3 |
| **2** | **开发 Java** | [→ schedule API+Job](2026-07-10-tech-director-to-dev-java-epic2-schedule-m3.md) | — | smoke |
| **3** | **开发 Admin** | [→ schedule UI](2026-07-10-tech-director-to-dev-admin-epic2-schedule-m3.md) | #1+#2 | build |

**无 Python / 无运维 M3**

## 窗口派发（复制到各 Cursor 窗口）

| # | 窗口 | HANDOFF | 激活 Prompt（Custom Instructions 首行） |
|---|------|---------|----------------------------------------|
| **1** | **UI 设计** | [→ 线框](2026-07-10-tech-director-to-ui-epic2-schedule-m3.md) | `角色：UI 设计。必读 diagnostics-list.md、diagnostic_run、FR-109、ADR-26、HANDOFF 2026-07-10-tech-director-to-ui-epic2-schedule-m3.md。任务：diagnostics-list §M3 定时计划 Tab + UI→开发 HANDOFF。` |
| **2** | **开发 Java** | [→ Java API+Job](2026-07-10-tech-director-to-dev-java-epic2-schedule-m3.md) | `角色：开发 Java。必读 DiagnosticRunServiceImpl.createRun、SubscriptionPeriodResetJob 模式、ADR-26、HANDOFF 2026-07-10-tech-director-to-dev-java-epic2-schedule-m3.md。任务：diagnostic_schedule DDL+CRUD+DiagnosticScheduleJob+test_diagnostic_schedule.py。` |
| **3** | **开发 Admin** | [→ Admin UI](2026-07-10-tech-director-to-dev-admin-epic2-schedule-m3.md) | `角色：开发 Admin。必读 diagnostics/index.vue、HANDOFF 2026-07-10-tech-director-to-dev-admin-epic2-schedule-m3.md。任务：定时计划 Tab/抽屉 · build:prod。` |

**并行**：#1 与 #2 可并行；#3 依赖 #1+#2。

## 完成后

- smoke：`deploy/scripts/test_diagnostic_schedule.py` ✅ runId=11
- commit **C23**：`feat(core,admin): EPIC-2 M3 scheduled diagnostic runs FR-109` ✅
- **完整版路线图 #1–#7 全部关闭** · 进入维护轨（smoke 9/9 · 扩展真 hook）
