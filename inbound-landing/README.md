# inbound-landing

Astro 4 落地页站点（SSG/SSR），SEO/GEO 友好。

## 职责

英文落地页渲染、表单提交、WhatsApp CTA、Turnstile 防刷、PostHog 埋点。

## 集成

- 页面数据 → `GET /api/v1/public/landing-pages/{slug}?projectId=`
- 表单 → `POST /api/v1/public/leads`（`X-Turnstile-Token` 可选）
- 内容来源 → `landing_page.content_json`（GrapesJS/Puck 编辑输出）
- 托管 → 本地 Docker `:4321`；生产 MinIO 静态桶 + Nginx/CDN

## 环境变量

| 变量 | 示例 | 说明 |
|------|------|------|
| `PUBLIC_API_BASE_URL` | `http://localhost:8080` | Astro → Java 公网 API（浏览器与 SSR fetch） |
| `PUBLIC_TURNSTILE_SITE_KEY` | `0x***` | Cloudflare Turnstile **站点**公钥；空则跳过 widget |
| `LANDING_PUBLIC_BASE_URL` | `http://localhost:4321` | **Java 侧**生成 `published_url`（非 Astro 运行时变量） |
| `TURNSTILE_SECRET_KEY` | `0x***` | **Java 侧** Turnstile 服务端校验（`CLOUDFLARE_TURNSTILE_SECRET`） |

本地复制：`cp .env.example .env`

Docker 使用 `deploy/.env` 中的 `PUBLIC_API_BASE_URL` / `TURNSTILE_SITE_KEY`（compose 映射为 `PUBLIC_TURNSTILE_SITE_KEY`）。

## 本地开发

```bash
cd inbound-landing
pnpm install
pnpm dev          # http://localhost:4321
```

已发布页（M2）：`http://localhost:4321/p/{projectId}/{slug}`

## Docker（运维）

```bash
cd deploy
docker compose -f docker-compose.yml -f docker-compose.local-d.yml up -d --build inbound-landing
curl.exe http://localhost:4321/
```

详见 `deploy/LOCAL_DOCKER.md` § EPIC-6 M2 落地页。

## 对应 EPIC

EPIC-6 落地页 Agent（M2 发布阶段）

## 状态

✅ **M2** — `/p/{projectId}/{slug}` SSR · 八模块 · Turnstile LeadForm → `POST /api/v1/public/leads`
