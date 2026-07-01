# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-01 | EPIC-7 M1 · FR-601 · ADR-20260701-14 |

## 交付

- [ ] `LeadController` — `POST /api/v1/public/leads`（Sa-Token 白名单 / 独立 Security 配置）
- [ ] `GET /api/v1/projects/{projectId}/leads` · `GET .../leads/{leadId}`
- [ ] Turnstile M1：配置开关；无 secret 时 skip 校验并 log warn
- [ ] 限流：IP + landing_page_id
- [ ] `tenant.excludes` 若需；public 写入需从 `landing_page` 解析 `tenant_id`/`project_id`
- [ ] smoke：`deploy/scripts/test_public_leads_api.py`

## Prompt

```
角色：开发 Java。必读 lead DDL、EPIC-7 HANDOFF、ARCHITECTURE 公开端点约定。
任务：public POST leads + Admin 列表。不调 LLM。
```

## Done

- **完成时间**：
- **结果摘要**：
- **遗留**：
