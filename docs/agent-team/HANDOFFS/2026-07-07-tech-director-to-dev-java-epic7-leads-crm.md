# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-07 | EPIC-7 M2 · FR-605 · ADR-20260707-20 |

## 上下文

M1 已有 `PublicLeadController` + `LeadController` 列表/详情。M2 增加状态机、负责人、跟进记录。表 `lead_followup` 已存在，无需 DDL 变更。

**相关文件**：
- `LeadServiceImpl.java` · `LeadController.java`
- `001_schema.sql` — `lead_status` · `lead_followup`
- `test_public_leads_api.py` — M1 smoke 参考

## 交付请求

**验收标准**：
- [x] `LeadFollowup` 实体 + Mapper + Vo
- [x] `PATCH /api/v1/projects/{projectId}/leads/{leadId}` — body: `{ status?, assigneeId? }` · 状态机校验（见 Sprint 索引）· 终态不可改
- [x] `GET /api/v1/projects/{projectId}/leads/{leadId}/followups` — 时间 ASC
- [x] `POST /api/v1/projects/{projectId}/leads/{leadId}/followups` — `{ content, channel? }` · `operator_id`=当前用户
- [x] 详情 Vo 增加 `assigneeName` · `followups[]`（或 followup 独立 GET）
- [x] `tenant.excludes` += `lead_followup`（若需要）
- [x] smoke：`deploy/scripts/test_leads_crm.py` — public 创建 lead → PATCH FOLLOWING → POST followup → PATCH QUOTED → GET 详情断言

## Prompt

```
角色：开发 Java。必读 LeadServiceImpl、ADR-20、Sprint 状态机。
任务：PATCH lead + followup CRUD + test_leads_crm.py。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-07
- **结果摘要**：`LeadFollowup` + `LeadStatusTransition` · PATCH lead · GET/POST followups · 详情 `assigneeName`/`followups[]` · `test_leads_crm.py` ✅
- **遗留**：无 · Admin CRM UI → P1 HANDOFF
