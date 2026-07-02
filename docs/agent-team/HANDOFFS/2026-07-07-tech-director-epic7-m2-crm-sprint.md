# EPIC-7 M2 轻量 CRM Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-07 |
| **优先级** | High |
| **关联** | EPIC-7 · **FR-605** · ADR-20260707-20 |
| **前置** | EPIC-7 M1 ✅ · EPIC-6 M2 落地页 ✅ · C16 `20b7a87` |

## 商业决策（技术总监）

完整版交付必须跑通 **落地页询盘 → 销售跟进 → 状态闭环**。M1 仅只读列表，客户无法「用起来」；M2 补 **状态流转 + 跟进记录 + 负责人**，是交付 SOP 最短路径，优先于月报白标/探针增强/支付后台。

## 目标（M2 MVP）

**线索状态机 + 跟进时间线 + 负责人 → Admin 可操作 CRM**

| 范围 | M2 做 | M2 不做 |
|------|-------|---------|
| FR-605 | 状态 NEW→FOLLOWING→QUOTED→WON/LOST · `lead_followup` CRUD · assignee | 复杂权限/公海池 |
| Admin | 详情 drawer 变更状态 · 跟进记录时间线 · 指派负责人（成员下拉 M2 简化为当前用户/手动 ID） | 导出 CSV · 批量操作 |
| Java | PATCH lead · GET/POST followups | FR-603 AI 话术 |
| FR-602/603/606/607 | — | WhatsApp 追踪 · AI 跟进 · 归因报表 · 广告接入 |

## 任务拆分

| # | 角色 | HANDOFF | 依赖 | 验收 |
|---|------|---------|------|------|
| **1** | **UI 设计** | [→ 线索 CRM 线框增量](2026-07-07-tech-director-to-ui-epic7-leads-crm.md) | — | `leads-list.md` M2 节 |
| **2** | **开发 Java** | [→ CRM API](2026-07-07-tech-director-to-dev-java-epic7-leads-crm.md) | DDL 已有 | smoke |
| **3** | **开发 Admin** | [→ CRM UI](2026-07-07-tech-director-to-dev-admin-epic7-leads-crm.md) | #1+#2 | build |

**无 Python / 无运维 M2**

## 状态机（M2）

```
NEW → FOLLOWING → QUOTED → WON
  ↘___________ LOST __________↗
```

- 允许：`NEW→FOLLOWING` · `FOLLOWING→QUOTED` · `QUOTED→WON` · 任意非终态→`LOST`
- 终态 `WON`/`LOST` 不可再改（M2）

## 窗口派发（复制到各 Cursor 窗口）

| # | 窗口 | HANDOFF | 激活 Prompt（Custom Instructions 首行） |
|---|------|---------|----------------------------------------|
| **1** | **UI 设计** | [→ 线索 CRM 线框](2026-07-07-tech-director-to-ui-epic7-leads-crm.md) | `角色：UI 设计。必读 leads-list.md、FR-605、HANDOFF 2026-07-07-tech-director-to-ui-epic7-leads-crm.md。任务：leads-list.md 追加 M2 CRM 增量（状态变更、跟进时间线、负责人）+ UI→开发 HANDOFF。` |
| **2** | **开发 Java** | [→ Java CRM API](2026-07-07-tech-director-to-dev-java-epic7-leads-crm.md) | `角色：开发 Java。必读 LeadServiceImpl、lead_followup DDL、ADR-20、HANDOFF 2026-07-07-tech-director-to-dev-java-epic7-leads-crm.md。任务：PATCH lead 状态/负责人 + followup CRUD + test_leads_crm.py。` |
| **3** | **开发 Admin** | [→ Admin CRM UI](2026-07-07-tech-director-to-dev-admin-epic7-leads-crm.md) | `角色：开发 Admin。必读 leads/index.vue、leads-list.md M2、HANDOFF 2026-07-07-tech-director-to-dev-admin-epic7-leads-crm.md。任务：详情 drawer 状态变更 + 跟进时间线 + 负责人 · build:prod。` |

**并行**：#1 UI 与 #2 Java 可并行；#3 等 Java smoke。

## 完成后

- smoke：`deploy/scripts/test_leads_crm.py`
- commit **C17**：`feat(core,admin): EPIC-7 M2 light CRM lead status and followups`
- 下一 Sprint：**EPIC-8 M2** 月报+白标 → [Sprint 索引](2026-07-08-tech-director-epic8-m2-reports-sprint.md)
