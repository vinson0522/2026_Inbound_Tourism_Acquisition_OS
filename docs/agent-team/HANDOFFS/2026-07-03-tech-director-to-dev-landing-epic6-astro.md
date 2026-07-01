# HANDOFF | 技术总监 → 开发（Landing / Astro）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-03 | EPIC-6 M2 · `inbound-landing` |

## 上下文

**M1 已有**：AI 生成八模块 `content_json`（`hero` … `whatsapp_cta`）· EPIC-7 `POST /api/v1/public/leads`

**M2 目标**：公网可读页面 + 表单闭环

## 交付

- [x] **Scaffold** Astro 4 + TypeScript + `pnpm`（`inbound-landing/`）
- [x] 路由 `src/pages/p/[projectId]/[slug].astro` — SSR/hybrid fetch `GET /api/v1/public/landing-pages/{slug}?projectId=`
- [x] 组件 `LandingModuleRenderer.astro` — 按 `module.key` 渲染 8 种模块（英文排版、移动优先）
- [x] `LeadForm.astro` — 字段读 `formConfigJson`；Cloudflare Turnstile widget；submit → `POST /api/v1/public/leads` + `X-Turnstile-Token`
- [x] SEO：`<title>` / meta / JSON-LD FAQ 来自 `seoMetaJson`
- [x] UTM 捕获：`utm_*` query → lead body `utm` json
- [x] `.env.example`：`PUBLIC_API_BASE_URL` · `PUBLIC_TURNSTILE_SITE_KEY`
- [x] `pnpm dev :4321` 本地可预览已发布页

## 模块 keys（与 inbound-ai 一致）

`hero`, `why_this_trip`, `itinerary`, `what_we_provide`, `traveler_reviews`, `faq`, `lead_form`, `whatsapp_cta`

## 验收

1. Admin 发布 slug 后访问 `http://localhost:4321/p/1/{slug}` 可见英文页
2. 提交表单 → Java lead 落库 → Admin `/leads` 可见
3. 未发布 slug → 404 友好页

## Prompt

```
角色：开发 Landing。必读 landing-page-publish 线框、PublicLeadController、LANDING_MODULE_KEYS。
任务：Astro 4 scaffold + 公网页 + Turnstile 表单。不调 Python。
依赖 Java public API（可先 mock JSON fixture 并行开发）。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-03
- **结果摘要**：
  - `output: hybrid` + `@astrojs/node` standalone；`pnpm build` ✅
  - `/p/[projectId]/[slug].astro` SSR fetch Java public API（8s timeout）
  - 八模块 `LandingModuleRenderer` + `LeadForm`（Turnstile 可选）→ `POST /api/v1/public/leads`
  - `404.astro` 友好未发布页；smoke `deploy/scripts/test_landing_astro_e2e.py`
  - 404 验收：未发布 slug → HTTP 404 + `lp-not-found` ✅
- **遗留**：
  - 全栈 E2E（已发布页八模块 HTML）需 Java :8080 + PG 在线；本机 PG 断开时仅验 404
  - Admin 发布按钮 → 另 HANDOFF
