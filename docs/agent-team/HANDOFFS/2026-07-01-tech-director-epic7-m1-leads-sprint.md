# EPIC-7 M1 线索 MVP Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-01 |
| **优先级** | High |
| **关联** | EPIC-7 · **FR-601** · ADR-20260701-14 |
| **前置** | EPIC-6 M1 落地页 ✅ · EPIC-1 项目 ✅ |

## 目标（M1 MVP）

**公开询盘表单 → 落库 `lead` → Admin 线索列表/详情**

| 范围 | M1 做 | M1 不做 |
|------|-------|---------|
| FR-601 | `POST /api/v1/public/leads`（限流 + Turnstile 占位校验） | 完整 CRM 流转 FR-605 |
| FR-602 | — | WhatsApp 点击追踪 |
| Admin | 线索列表 + 详情只读 + 状态 NEW | 跟进话术 AI FR-603 |
| 归因 | `landing_page_id` / `keyword_id` / `utm_json` / `device` 落库 | 归因报表 FR-606 |
| 落地页 | — | Astro 公网页（→ EPIC-6 M2 + `inbound-landing`） |

## 任务拆分

| # | 角色 | HANDOFF | 依赖 | 验收 |
|---|------|---------|------|------|
| **1** | **UI 设计** | [→ 线索列表线框](2026-07-01-tech-director-to-ui-epic7-leads-list.md) | — | `leads-list.md` |
| **2** | **开发 Java** | [→ lead API](2026-07-01-tech-director-to-dev-java-epic7-leads.md) | DDL | public POST + Admin GET |
| **3** | **开发 Admin** | [→ 线索列表](2026-07-01-tech-director-to-dev-admin-epic7-leads.md) | #1+#2 | 列表 + 详情 drawer |

**无 Python M1**（跟进 AI → M2）。

## DDL / API

- 表：`lead`（`lead_status`: NEW/…）
- `POST /api/v1/public/leads` — 无 JWT；`X-Turnstile-Token` 占位；rate limit
- `GET /api/v1/projects/{projectId}/leads` — 分页、status 筛选

## 窗口 Prompt 摘要

| 角色 | Prompt |
|------|--------|
| UI | `必读 FR-601 与 lead DDL → leads-list.md` |
| Java | `public leads + Admin list；Turnstile stub` |
| Admin | `线索列表 + 详情 drawer` |

## 完成后

- 各 HANDOFF Done + `MEMORY.md` EPIC-7 M1 ✅
- **技术总监签核（2026-07-01）**：功能 ✅ · **C11 commit ⏳**
- **下一 Sprint**：EPIC-8 M1 报告 或 EPIC-6 M2 Astro（C11 后定案）
