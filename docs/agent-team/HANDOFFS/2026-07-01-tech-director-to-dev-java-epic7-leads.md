# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-01 | EPIC-7 M1 · FR-601 · ADR-20260701-14 |

## 交付

- [x] `PublicLeadController` — `POST /api/v1/public/leads`（`@SaIgnore` + `security.excludes`）
- [x] `GET /api/v1/projects/{projectId}/leads` · `GET .../leads/{leadId}`
- [x] Turnstile M1：`inbound.turnstile.enabled` + `secret-key`；无 secret 时 skip 校验并 log warn
- [x] 限流：`@RateLimiter` IP + `#bo.landingPageId`（10/min）
- [x] `tenant.excludes` += `lead`；public 写入从 `landing_page` 解析 `tenant_id`/`project_id`/`keyword_id`
- [x] smoke：`deploy/scripts/test_public_leads_api.py` ✅

## Prompt

```
角色：开发 Java。必读 lead DDL、EPIC-7 HANDOFF、ARCHITECTURE 公开端点约定。
任务：public POST leads + Admin 列表。不调 LLM。
```

## Done

- **完成时间**：2026-07-01
- **结果摘要**：`ruoyi-project` 新增 Lead 实体/Mapper/Service；`PublicLeadController` + `LeadController`；Turnstile M1 stub；`test_public_leads_api.py` smoke 通过（public POST → Admin list/detail）
- **遗留**：ServiceException 校验仍返回 HTTP 200 + code 500（若依默认）；M2 Turnstile 真 siteverify HTTP
