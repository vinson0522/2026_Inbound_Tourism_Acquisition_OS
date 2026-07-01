# 线框：落地页 Agent · 发布与公网预览（EPIC-6 M2）

> **PRD**：§8.7 FR-505 表单 · FR-506/507 子集 · §20.3 八模块  
> **EPIC**：EPIC-6 M2 · **ADR-20260703-16**  
> **扩展**：[landing-page-list.md](./landing-page-list.md)（M1 列表 + Admin JSON 预览）  
> **公网**：`inbound-landing` Astro 4 · `/p/{projectId}/{slug}`

---

## 页面目标

在 M1 草稿/AI 预览基础上，完成 **Admin 发布 → Astro 公网页 → Turnstile 表单 → 线索落库** 闭环（FR-507 本地/staging 子集）。

**M2 范围**：
- ✅ Admin：发布 / 下线 / 打开公网预览外链
- ✅ Astro：八模块英文页 + Lead Form + Turnstile + WhatsApp CTA
- ✅ Java：`GET public/landing-pages` · `POST publish/unpublish`
- ❌ GrapesJS/Puck 可视化编辑
- ❌ HTML 文件导出（FR-506 DOCX/Markdown）
- ❌ 自定义域名 / CDN / A/B（FR-508）
- ❌ PostHog 埋点（M3）

**入口**：
- [landing-page-list.md](./landing-page-list.md) 预览 drawer / 列表行操作
- 发布成功后 toast「打开公网预览」
- 公网直接访问 `{LANDING_BASE}/p/{projectId}/{slug}`

---

## 公网 URL 约定（ADR-16）

Slug 在 DDL 为 `(project_id, slug)` 唯一 → **路径必须带 projectId**：

| 项 | 值 |
|----|-----|
| Astro 路由 | `/p/{projectId}/{slug}` |
| 本地示例 | `http://localhost:4321/p/1/chongqing-cyberpunk-tour` |
| Public API | `GET /api/v1/public/landing-pages/{slug}?projectId={id}` |
| 条件 | 仅 `status = PUBLISHED` |
| `published_url` | `{inbound.landing.public-base-url}/p/{projectId}/{slug}` |

---

## Part A — Admin 发布流（扩展 M1）

### A.1 预览 Drawer 底栏（M2 增补）

在 [landing-page-list.md](./landing-page-list.md) 预览 drawer **替换/增补**底栏：

```
┌─ 预览 · Chongqing Cyberpunk Tour ─────────────────── [×] │
│ …（M1 摘要 / SEO / collapse 不变）…                        │
│ ── 发布状态 (M2) el-descriptions ──                        │
│ 状态        [待发布 Tag] READY                             │
│ 公网链接    —（未发布）                                     │
│ 发布时间    —                                               │
│ ── 合规 alert ──                                            │
│ ⚠ AI 生成内容需人工确认价格/签证/政策后再发布。              │
│ [关闭]  [Admin JSON 预览]  [发布]  [导出 HTML] disabled     │
└───────────────────────────────────────────────────────────┘

PUBLISHED 态底栏:
│ 状态        [已发布 Tag] PUBLISHED                         │
│ 公网链接    http://localhost:4321/p/1/chongqing-cyber… [复制]│
│ 发布时间    2026-07-03 11:20                               │
│ [关闭]  [打开公网预览 ↗]  [下线]  [查看线索 →] P2           │
```

### A.2 按钮态矩阵

| 按钮 | DRAFT | EDITING | READY | PUBLISHED | ARCHIVED |
|------|:-----:|:-------:|:-----:|:---------:|:--------:|
| Admin JSON 预览 | ✅ | ✅ | ✅ | ✅ | ✅ |
| **发布** | ⚠️ 无 content | disabled loading | ✅ primary | hidden | disabled |
| **打开公网预览 ↗** | hidden | hidden | hidden | ✅ | hidden |
| **下线** | hidden | hidden | hidden | ✅ danger plain | hidden |
| 导出 HTML | disabled | disabled | disabled | disabled | disabled |

**发布前置校验**（前端 + 后端）：
- `content_json.modules` 非空
- `slug` 非空且合法
- `status !== EDITING`

不满足时「发布」disabled + tooltip「请先完成 AI 生成并确认内容」。

### A.3 发布确认 Dialog（el-message-box 或 420px dialog）

```
┌─ 确认发布 ─────────────────────────────────────── [×] │
│ 即将把页面发布到公网预览环境：                            │
│ · 标题：Chongqing Cyberpunk City Tour                  │
│ · Slug：/p/1/chongqing-cyberpunk-tour                    │
│ · 表单：姓名/邮箱/电话/日期/人数/预算 + Turnstile         │
│ ⚠ 发布后访客可提交询盘；请确认文案合规。                  │
│                              [取消]  [确认发布]          │
└─────────────────────────────────────────────────────────┘
```

| 步骤 | API | UI |
|------|-----|-----|
| 确认 | `POST .../landing-pages/{pageId}/publish` | 按钮 loading |
| 成功 | 返回 `publishedUrl`, `publishedAt` | toast「发布成功」+ 可复制链接 |
| 失败 | 400 如无 content | `ElMessage.error` |

**成功 toast**（含操作）：
> 页面已发布 · [复制链接] [打开预览 ↗]

### A.4 下线确认

```
┌─ 确认下线 ─────────────────────────────────────── [×] │
│ 下线后公网链接将不可访问（404），已有线索保留。           │
│ 页面将恢复为草稿状态。                                    │
│                              [取消]  [确认下线]          │
```

| API | 结果 |
|-----|------|
| `POST .../landing-pages/{pageId}/unpublish` | `status=DRAFT` · 清空 `published_url` |

### A.5 列表行操作（M2 增补）

在 M1 行操作基础上 **追加**：

| 操作 | 条件 | 行为 |
|------|------|------|
| 发布 | READY | 同 drawer 发布 flow |
| 公网预览 ↗ | PUBLISHED | `window.open(publishedUrl)` |
| 下线 | PUBLISHED | unpublish confirm |

**列表列增补**（M2 启用 M1 可选列）：

| 列 | 字段 | 展示 |
|----|------|------|
| 公网链接 | `published_url` | PUBLISHED 时 mono 链 + 复制；否则 `—` |
| 发布时间 | `published_at` | PUBLISHED 时显示 |

### A.6 列表 status Tag（M2 强调 PUBLISHED）

| DB 值 | Tag type | 中文 |
|-------|----------|------|
| `PUBLISHED` | `success` | **已发布**（加 link 图标可选） |

---

## Part B — Astro 公网页（FR-506/507 子集）

### B.1 页面信息架构

**路由**：`src/pages/p/[projectId]/[slug].astro`  
**数据**：SSR/hybrid fetch Public API → 404 友好页（未发布 / 不存在）

```
┌────────────────────────────────────────────────────────────── mobile-first max-w-3xl mx-auto
│ [Brand bar — 项目名小字 · Tour by Dragon Journey]              │
├──────────────────────────────────────────────────────────────
│ ① HERO                                                        │
│   [destination image placeholder / gradient]                  │
│   H1 Discover Chongqing's Futuristic Skyline                  │
│   Subtitle: Private tours for first-time visitors…            │
│   [ Plan My Trip ] primary CTA → scroll #lead-form            │
├──────────────────────────────────────────────────────────────
│ ② WHY THIS TRIP                                               │
│   H2 Why This Trip                                            │
│   body paragraphs…                                            │
├──────────────────────────────────────────────────────────────
│ ③ ITINERARY                                                   │
│   H2 Sample Itinerary · Day 1… Day N                          │
├──────────────────────────────────────────────────────────────
│ ④ WHAT WE PROVIDE                                             │
│   icon list: transfers · hotels · guide · visa help · 24/7    │
├──────────────────────────────────────────────────────────────
│ ⑤ TRAVELER REVIEWS                                            │
│   cards: quote · country · ★★★★★                              │
├──────────────────────────────────────────────────────────────
│ ⑥ FAQ                                                         │
│   accordion: visa · safety · payment · weather · language     │
├──────────────────────────────────────────────────────────────
│ ⑦ LEAD FORM  id="lead-form"                                   │
│   H2 Request a Custom Quote                                   │
│   [Name*] [Email*] [Phone]                                      │
│   [Travel date] [Party size] [Budget]                         │
│   [Message textarea]                                          │
│   ┌─ Cloudflare Turnstile widget ─┐                           │
│   │  [ I'm human checkbox ]       │                           │
│   └────────────────────────────────┘                          │
│   [ Submit Inquiry ] primary full-width mobile                │
│   Privacy: We only use your info to respond to this inquiry.   │
├──────────────────────────────────────────────────────────────
│ ⑧ WHATSAPP CTA                                                │
│   [ Chat on WhatsApp ] sticky bottom bar (mobile) or section  │
├──────────────────────────────────────────────────────────────
│ Footer: © brand · probe disclaimer N/A · powered by TourGEO   │
└──────────────────────────────────────────────────────────────
```

### B.2 视觉 Token（公网页 · 非 Admin）

与 [tokens.md](../tokens.md) 品牌一致，Astro CSS 变量：

| Token | 值 | 用途 |
|-------|-----|------|
| `--lp-primary` | `#1677A0` | CTA、链接 |
| `--lp-primary-dark` | `#0F5575` | CTA hover |
| `--lp-text` | `#1F2937` | 正文 |
| `--lp-muted` | `#6B7280` | 副标题 |
| `--lp-bg` | `#FFFFFF` | 页面底 |
| `--lp-section-alt` | `#F5F7FB` | 交替 section 背景 |
| `--lp-radius` | `8px` | 卡片、按钮 |
| 字体 | `system-ui, Inter, sans-serif` | 英文可读 |

**排版**：移动优先；H1 `clamp(1.75rem, 5vw, 2.5rem)`；section `py-12 px-4`；CTA min-height 44px（触控）。

### B.3 八模块映射（`content_json.modules[]`）

与 `inbound-ai` / Java Public Vo **key 一致**（`module.key` 或 `type`，开发统一为 **key**）：

| key | §20.3 | Astro 组件 | 渲染要点 |
|-----|-------|------------|----------|
| `hero` | Hero | `HeroModule.astro` | `heading`, `subheading`, `ctaLabel`, `imageUrl?` |
| `why_this_trip` | Why This Trip | `WhyModule.astro` | `heading`, `body` (markdown/plain) |
| `itinerary` | Itinerary | `ItineraryModule.astro` | `days[]`: `{ day, title, highlights[] }` |
| `what_we_provide` | What We Provide | `ProvideModule.astro` | `items[]`: `{ icon?, label, description? }` |
| `traveler_reviews` | Traveler Reviews | `ReviewsModule.astro` | `items[]`: `{ quote, author, country, rating }` |
| `faq` | FAQ | `FaqModule.astro` | `items[]`: `{ q, a }` · accordion · JSON-LD |
| `lead_form` | Lead Form | `LeadForm.astro` | 见 B.4 |
| `whatsapp_cta` | WhatsApp CTA | `WhatsAppBar.astro` | `label`, `url` from `whatsappLink` |

**渲染顺序**：按 `modules[]` 数组顺序；缺模块则跳过（不渲染空 section）。

**JSON 示例**（Public API `contentJson`）：

```json
{
  "modules": [
    { "key": "hero", "content": { "heading": "...", "subheading": "...", "ctaLabel": "Plan My Trip" } },
    { "key": "why_this_trip", "content": { "heading": "Why This Trip", "body": "..." } },
    { "key": "itinerary", "content": { "days": [{ "day": 1, "title": "Arrival", "highlights": ["..."] }] } },
    { "key": "what_we_provide", "content": { "items": [{ "label": "Private transfers" }] } },
    { "key": "traveler_reviews", "content": { "items": [{ "quote": "...", "author": "Sarah", "country": "US", "rating": 5 }] } },
    { "key": "faq", "content": { "items": [{ "q": "Visa?", "a": "..." }] } },
    { "key": "lead_form", "content": { "heading": "Request a Custom Quote" } },
    { "key": "whatsapp_cta", "content": { "label": "Chat on WhatsApp" } }
  ]
}
```

### B.4 Lead Form + Turnstile（FR-505 + EPIC-7）

**组件**：`LeadForm.astro` + client island `LeadForm.ts`（或 React/Vue island 若项目选用）

| 字段 key | `formConfigJson.fields` | HTML | 必填 |
|----------|-------------------------|------|:----:|
| `name` | ✅ | text | ✅ |
| `email` | ✅ | email | ✅* |
| `phone` | ✅ | tel | ✅* |
| `travel_date` | ✅ | date | — |
| `party_size` / `pax` | ✅ | number min 1 | — |
| `budget` | ✅ | text | — |
| `notes` / `message` | ✅ | textarea | — |

\* 至少 email 或 phone 其一（与 [leads-list.md](./leads-list.md) public API 一致）。

**Turnstile 区**：

```
┌─ 防机器人 ──────────────────────────────────────────────┐
│  [ Cloudflare Turnstile Widget ]                         │
│  site key: API `turnstileSiteKey` 或 env 兜底            │
│  无 key 时：dev 模式显示灰色占位 + hidden token skip     │
└──────────────────────────────────────────────────────────┘
```

| 项 | 规范 |
|----|------|
| Widget | Cloudflare Turnstile `explicit` render |
| Site Key | Public API `turnstileSiteKey` → fallback `PUBLIC_TURNSTILE_SITE_KEY` |
| Submit Header | `X-Turnstile-Token: {token}` |
| API | `POST /api/v1/public/leads` |
| Body | `landingPageId`, `name`, `email`, `phone`, `travelDate`, `partySize`, `budget`, `message`, `utm`, `device`, `source: "form"` |
| UTM | 页面 load 解析 `window.location.search` → `utm` object |
| device | `navigator.userAgent` |
| 成功 | inline success「Thank you! We'll reply within 24 hours.» + 清空表单 |
| 失败 | inline error + Turnstile reset |

**Submit 按钮态**：
- 未通过 Turnstile（有 site key 时）：disabled
- submitting：loading + disabled
- 成功：绿色 alert 3s

**合规 footer**（表单下小字）：
> Your information is used only to respond to this inquiry. Prices and visa policies require confirmation by our travel advisor.

**价格/签证**：若 module FAQ 含 price/visa，section 顶 `needs_human_review` 等价提示条（黄色细条，非阻塞）。

### B.5 WhatsApp CTA

| 项 | 规范 |
|----|------|
| 数据源 | `whatsappLink`（API 根字段）或 `formConfigJson.whatsapp_url` |
| Mobile | 固定底栏 `position: sticky; bottom: 0` + safe-area |
| Desktop | 独立 section 或 hero 次 CTA |
| 点击 | `window.open(wa.me…)` · FR-602 追踪 **M3**（M2 仅跳转） |
| UTM | 可选 append `?text=Hi, I'm interested in {title}` |

### B.6 SEO（`seoMetaJson`）

| 标签 | 来源 |
|------|------|
| `<title>` | `seoMetaJson.title` |
| `<meta name="description">` | `seoMetaJson.description` |
| `<h1>` | hero `heading`（与 SEO h1 一致） |
| FAQ JSON-LD | `seoMetaJson.faq_schema` 或从 `faq` module 生成 |
| `lang` | `en`（M2 固定英文页） |
| `canonical` | `{LANDING_BASE}/p/{projectId}/{slug}` |

---

## Part C — 错误与空态

### C.1 Astro 公网页

| 状态 | UI |
|------|-----|
| 404 未发布 | 全页友好：`Page not found` ·「This tour page is not available.» · 无 Admin 链 |
| API 5xx | `Something went wrong. Please try again later.` |
| 空 modules | 不应发布；若发生显示 minimal hero + 联系提示 |
| Turnstile 失败 | 「Verification failed. Please try again.» + widget reset |
| Lead 400 | 字段级 error 或顶部 message（email/phone 必填） |
| Lead 429 限流 | 「Too many requests. Please wait a moment.» |

### C.2 Admin

| 状态 | UI |
|------|-----|
| 发布失败 | `ElMessage.error` 展示后端 message |
| 重复发布 | 允许（幂等更新 `published_at`）或 409 — 以前端 toast 为准 |
| 下线后打开旧链接 | 用户侧 404；Admin toast 已下线 |
| 复制链接 | `navigator.clipboard` +「已复制公网链接」 |

---

## Part D — API 与字段对照

### D.1 Admin 发布 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/projects/{projectId}/landing-pages/{pageId}/publish` | → PUBLISHED |
| POST | `/api/v1/projects/{projectId}/landing-pages/{pageId}/unpublish` | → DRAFT |
| GET | `/api/v1/public/landing-pages/{slug}?projectId=` | Astro fetch |

### D.2 Publish 响应字段

| UI | DDL | API |
|----|-----|-----|
| 公网链接 | `published_url` | `publishedUrl` |
| 发布时间 | `published_at` | `publishedAt` |
| 状态 | `status` | `status` |

### D.3 Public Vo（Astro 消费）

见 Java HANDOFF；Admin **不调用** public API（除测试）。

### D.4 DDL 发布相关

| 列 | M2 用途 |
|----|---------|
| `published_url` | 发布后写入 |
| `published_at` | 发布时间 |
| `status` | `PUBLISHED` / unpublish → `DRAFT` |
| `form_config_json` | Lead Form 字段 |
| `whatsapp_link` | WhatsApp CTA |

---

## Part E — 环境变量

| 变量 | 服务 | 说明 |
|------|------|------|
| `inbound.landing.public-base-url` | Java | 默认 `http://localhost:4321` |
| `PUBLIC_API_BASE_URL` | Astro | `http://localhost:8080` |
| `PUBLIC_TURNSTILE_SITE_KEY` | Astro | 与 Java 配置一致 |
| Turnstile secret | Java | 有则 siteverify；无则 skip |

---

## Part F — 闭环验收路径

```
Admin 预览 READY → 发布 → 复制链接
  → 浏览器打开 /p/{projectId}/{slug}
  → 填写表单 + Turnstile → Submit
  → Admin /leads 列表可见新线索（source=form, landing_page_id）
```

Smoke：`deploy/scripts/test_landing_publish_e2e.py`

---

## M2 范围边界

| 包含 | 不包含 |
|------|--------|
| 发布 / 下线 / 公网预览外链 | GrapesJS 编辑 |
| Astro 八模块 + 表单 + Turnstile | FR-506 文件导出 |
| CORS public API | 自定义域名 |
| 线索落库（已有 EPIC-7） | WhatsApp 点击追踪 FR-602 |
| 404 / Turnstile 错误文案 | PostHog |

---

## 实现参考

| 层 | 路径 / 参考 |
|----|-------------|
| Admin 增补 | `inbound-admin/src/views/tourgeo/landing/index.vue` |
| API | `src/api/tourgeo/landing.ts` — `publishLandingPage`, `unpublishLandingPage` |
| Astro | `inbound-landing/src/pages/p/[projectId]/[slug].astro` |
| 模块 | `LandingModuleRenderer.astro` + 8 子组件 |
| 表单 | `LeadForm.astro` + Turnstile |
| M1 线框 | [landing-page-list.md](./landing-page-list.md) |
| 线索 | [leads-list.md](./leads-list.md) |

---

## 版本

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-03 | UI 设计 | EPIC-6 M2 初版 · ADR-16 · Admin 发布流 + Astro 公网页 |
