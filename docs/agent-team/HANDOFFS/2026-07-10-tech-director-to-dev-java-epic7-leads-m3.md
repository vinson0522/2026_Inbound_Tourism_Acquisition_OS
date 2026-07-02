# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-10 | EPIC-7 M3 · FR-602/603 · ADR-20260710-25 |

## 交付请求

- [x] DDL 增量：`lead_channel_event`（tenant_id · project_id · landing_page_id · event_type · utm_json · device · ip_hash · created_at）
- [x] `POST /api/v1/public/lead-events` — body: `{ eventType: "whatsapp_click", projectId, landingPageId?, utm?, device? }` · 限流 · 无登录
- [x] `GET /api/v1/projects/{projectId}/leads/{leadId}` 增补 `whatsappClickCount` · `lastWhatsappClickAt`（同 landing_page 归因匹配，M3 简化：按 project+landing_page 聚合展示在详情）
- [x] `POST /api/v1/projects/{projectId}/leads/{leadId}/ai-suggestion` — Feign `/ai/followup/generate` · 返回 `{ suggestionEn, suggestionZh, needsHumanReview }`
- [x] smoke：`deploy/scripts/test_leads_whatsapp_ai.py`

## Prompt

```
角色：开发 Java。必读 PublicLeadController、LeadServiceImpl、Feign 模式、ADR-25。
任务：lead-events + ai-suggestion + DDL + smoke。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-02
- **结果摘要**：
  - DDL `lead_channel_event` + `tenant.excludes`
  - `POST /api/v1/public/lead-events`（whatsapp_click · IP 限流 · SHA-256 ip_hash）
  - 线索详情 `whatsappClickCount` / `lastWhatsappClickAt`（project+landing_page 弱关联）
  - `POST .../leads/{leadId}/ai-suggestion` → Feign `/ai/followup/generate`
  - smoke `deploy/scripts/test_leads_whatsapp_ai.py` ✅（leadId=42 · clicks=2）
- **遗留**：Admin / Landing beacon UI 待其他 HANDOFF；C22 未 commit
