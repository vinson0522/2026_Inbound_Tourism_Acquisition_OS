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
| `PUBLIC_LANDING_BASE_URL` | `http://localhost:4321` | 已发布页 canonical 基址 |
| `PUBLIC_SITE_URL` | `http://localhost:4321` | 营销首页 `/` canonical 与 OG |
| `PUBLIC_ADMIN_URL` | `http://localhost:5173` | 营销页「Sign in / Admin」跳转 Admin |
| `PUBLIC_DEMO_URL` | `mailto:hello@tourgeo.ai` | 营销页「Book a demo」链接 |
| `LANDING_PUBLIC_BASE_URL` | `http://localhost:4321` | **Java 侧**生成 `published_url`（非 Astro 运行时变量） |
| `TURNSTILE_SECRET_KEY` | `0x***` | **Java 侧** Turnstile 服务端校验（`CLOUDFLARE_TURNSTILE_SECRET`） |

本地复制：`cp .env.example .env`

Docker 使用 `deploy/.env`（或 compose 默认值）中的上述 `PUBLIC_*`；营销首页在 **build 阶段** bake 进 SSG，改 URL 后需 `--build`。

## 本地开发

```bash
cd inbound-landing
pnpm install
pnpm dev          # http://localhost:4321 — 营销首页 /
pnpm build        # hybrid SSG + Node SSR（/p/...）
```

| 路由 | 说明 |
|------|------|
| `/` · `/zh` | 营销门户首页（Hero / 价值 / 能力 / 工作流 / CTA）· EN + 中文 |
| `/pricing` · `/zh/pricing` | 套餐价格（体验/成长/代理三档 + FAQ） |
| `/contact` · `/zh/contact` | 联系表单 → `POST /api/v1/public/marketing-contact`（Turnstile 可选） |
| `/p/{projectId}/{slug}` | 已发布客户落地页（M2 SSR + LeadForm） |

**i18n**：`src/i18n/ui.ts` 单一词典（`en` / `zh`）；EN 在 `/`，中文在 `/zh/*`；页头右上角语言切换 + `<link hreflang>`。新增文案改词典即可，组件与路由复用。

## Docker（本地 :4321）

```bash
cd deploy
docker compose -f docker-compose.yml -f docker-compose.local-d.yml up -d --build inbound-landing
curl.exe -s -o NUL -w "HTTP %%{http_code}\n" http://localhost:4321/
curl.exe -s http://localhost:4321/ | findstr /i "TourGEO Win overseas"
```

- 容器内访问 Java：`PUBLIC_API_BASE_URL=http://host.docker.internal:8080`（`docker-compose.local-d.yml` 默认）
- 营销 CTA 默认 Admin：`PUBLIC_ADMIN_URL=http://localhost:5173`（compose build args）
- 若 Docker Hub 拉取 `node:20-alpine` 超时，可先：`docker pull docker.m.daocloud.io/library/node:20-alpine && docker tag docker.m.daocloud.io/library/node:20-alpine node:20-alpine`

详见 `deploy/LOCAL_DOCKER.md` § EPIC-6 M2 落地页。

## 对应 EPIC

EPIC-6 落地页 Agent（M2 发布阶段 + 营销门户 `/`）· 交付硬化 Sprint #5 A1（Pricing / Contact / i18n / Turnstile）

## 状态

✅ **M2** — `/p/{projectId}/{slug}` SSR · 八模块 · Turnstile LeadForm → `POST /api/v1/public/leads`  
✅ **营销门户** — `/` `/zh` SSG · TourGEO 首页 · 与 `/p/...` 共存  
✅ **A1 交付硬化** — Pricing / Contact 页 · EN+中文 i18n · Contact → `POST /api/v1/public/marketing-contact`（真集成 Turnstile 可选）
