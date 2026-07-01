# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-03 | EPIC-6 M2 · ADR-20260703-16 |

## 交付

- [x] `PublicLandingPageController` — `GET /api/v1/public/landing-pages/{slug}?projectId=`（`@SaIgnore`；仅 `PUBLISHED`）
  - 返回：`id, projectId, title, slug, contentJson, seoMetaJson, formConfigJson, whatsappLink, turnstileSiteKey`（site key 来自配置，无 Key 时 null）
- [x] `LandingPageController` — `POST .../landing-pages/{pageId}/publish` · `POST .../unpublish`
  - publish：校验 slug 非空、content_json 非空 → `status=PUBLISHED` · `published_at` · `published_url`
  - unpublish：`status=DRAFT` · 清空 `published_url`（可选保留 slug）
- [x] `LandingPublishProperties` — `public-base-url` 默认 `http://localhost:4321`
- [x] **CORS**：允许 `http://localhost:4321`（及 env 配置）访问 `/api/v1/public/**`
- [x] **Turnstile M2**：`TurnstileValidator` 当 `secret-key` 非空时 POST Cloudflare siteverify；失败 400
- [x] smoke：`deploy/scripts/test_landing_publish_e2e.py`

## Public Vo 字段（对齐 Astro）

```json
{
  "id": 1,
  "projectId": 1,
  "title": "...",
  "slug": "chongqing-cyberpunk-tour",
  "contentJson": { "modules": [{ "key": "hero", "content": {...} }] },
  "seoMetaJson": { "title": "...", "description": "..." },
  "formConfigJson": { "fields": ["name","email","phone",...] },
  "whatsappLink": "https://wa.me/...",
  "turnstileSiteKey": "optional"
}
```

## Prompt

```
角色：开发 Java。必读 LandingPageServiceImpl、PublicLeadController、ADR-16。
任务：public landing GET + publish/unpublish + CORS + Turnstile siteverify（有 secret 时真校验）。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-03
- **结果摘要**：`PublicLandingPageController` + publish/unpublish · `LandingPublishProperties` · `PublicApiCorsConfig` · Turnstile siteverify（hutool → Cloudflare）· smoke `test_landing_publish_e2e.py` E2E passed
- **遗留**：Turnstile 真 Key 联调需生产 secret；C13 commit 待用户指令
