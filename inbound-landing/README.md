# inbound-landing

Astro 4 落地页站点（SSG），SEO/GEO 友好。

## 职责

英文落地页渲染、表单提交、WhatsApp CTA、Turnstile 防刷、PostHog 埋点。

## 集成

- 表单 → `POST /api/v1/public/leads`
- 内容来源 → `landing_page.content_json`（GrapesJS/Puck 编辑输出）
- 托管 → MinIO 静态桶 + Nginx/CDN

## 对应 EPIC

EPIC-6 落地页 Agent（发布阶段）

## 状态

⏳ **待 scaffold**
