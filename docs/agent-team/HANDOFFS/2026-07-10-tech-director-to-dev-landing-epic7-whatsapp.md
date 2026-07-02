# HANDOFF | 技术总监 → 开发（Landing / inbound-landing）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-10 | EPIC-7 M3 · FR-602 · ADR-20260710-25 |

## 交付请求

- [x] `WhatsAppBar.astro` — 点击 wa.me 前 `fetch POST /api/v1/public/lead-events`（sendBeacon 或 fetch keepalive）· payload 含 projectId · landingPageId · utm from URL
- [x] 失败不阻塞跳转 · env `PUBLIC_API_BASE_URL`
- [x] `pnpm build` ✅

## Prompt

```
角色：开发 Landing。必读 WhatsAppBar.astro、public leads 模式、HANDOFF 2026-07-10-tech-director-to-dev-landing-epic7-whatsapp.md。
任务：WhatsApp 点击 beacon · build。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-02
- **结果摘要**：`WhatsAppBar.astro` + `scripts/whatsapp-beacon.ts` · capture 阶段 `sendBeacon`/`fetch keepalive` · UTM 解析 · 不 `preventDefault` · `pnpm build` ✅
- **遗留**：—
