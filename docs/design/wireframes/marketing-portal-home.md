# 线框：营销门户 · 首页（Sprint #3 MVP）

> **产品**：Inbound AI Growth Agent / **TourGEO** / **旅获 AI**  
> **Sprint**：[Sprint #3 产品化并行](../../agent-team/HANDOFFS/2026-07-11-tech-director-sprint3-productization-parallel.md)  
> **路由**：`inbound-landing` 根路径 **`/`**（替换 `index.astro` 占位）  
> **区分**：**非** 客户落地页 `/p/{projectId}/{slug}` · 非 Admin `inbound-admin`

---

## 页面目标

为 B2B 入境游服务商提供 **产品营销首页**：说明 TourGEO 如何帮助海外获客（GEO 可见率 → 关键词 → 内容 → 落地页 → 询盘），引导 **预约演示** 或 **登录管理后台**。

**MVP 范围**：
- ✅ 单页 SSG · Hero · 价值主张 3 列 · 4 能力卡片 · 底部 CTA · 合规页脚
- ✅ 英文为主 · 中文副标题/品牌名可选展示
- ✅ 移动端单列 · 桌面 ≥1024px 多列
- ✅ 基础 SEO meta · 无 Turnstile · 无表单落库
- ❌ 定价页 · 博客 · 多语言切换 · PostHog · 在线购买
- ❌ 复用客户 `LandingLayout`（Tour by {brand}）— 使用独立 **MarketingLayout**

**替换对象**：`inbound-landing/src/pages/index.astro` 当前占位 HTML。

---

## 品牌与文案基调

| 项 | 规范 |
|----|------|
| 主品牌（英文） | **TourGEO** |
| 中文副标 | 旅获 AI · 入境游海外获客增长系统 |
| 语气 | B2B 专业 · 结果导向 · **不承诺** AI 排名保证 |
| 受众 | 地接社 / DMC / 入境游运营商 · 海外市场负责人 |

**合规固定句**（页脚 + Hero 下方小字）：
> GEO insights are based on sampled AI answers at a point in time. We do not guarantee search or AI ranking outcomes.

---

## 布局结构（ASCII · 桌面）

```
┌─ MarketingLayout ───────────────────────────────────────────────────────────┐
│ [sticky header]  TourGEO  |  Features  Pricing(disabled)  [Log in →]        │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─ HERO (gradient bg · centered) ────────────────────────────────────────┐ │
│ │  旅获 AI · Inbound AI Growth Agent          (中文副标 · optional)         │ │
│ │  Win overseas travelers where they ask AI                                 │ │
│ │  GEO visibility diagnostics, keyword insights, content & landing pages —    │ │
│ │  one workflow for China inbound tour operators.                             │ │
│ │  [Book a demo] primary    [Log in to Admin →] outline                     │ │
│ │  ℹ Sample-based AI visibility · No ranking guarantees                     │ │
│ └───────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ VALUE PROPS (3 columns · section alt bg) ────────────────────────────────┐ │
│ │  [icon] Measure          [icon] Act              [icon] Convert            │ │
│ │  Know if AI recommends   Turn gaps into         Capture leads on         │ │
│ │  your brand vs rivals    scripts & EN pages     English landing pages    │ │
│ └───────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ CAPABILITIES (2×2 grid → mobile 1 col) ────────────────────────────────┐ │
│ │  GEO Diagnostics    Keyword Intel    Content Agent    Landing Pages       │ │
│ │  (card)             (card)           (card)           (card)              │ │
│ └───────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ HOW IT WORKS (optional strip · 闭环 1 行) ─────────────────────────────┐ │
│ │  Profile → GEO scan → Keywords → Content → Landing page → Leads → Report  │ │
│ └───────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ CTA BAND (primary bg) ─────────────────────────────────────────────────┐ │
│ │  Ready to see your brand in AI answers?                                   │ │
│ │  [Book a demo]  [Log in to Admin]                                         │ │
│ └───────────────────────────────────────────────────────────────────────────┘ │
│ [footer]  © TourGEO · Privacy · Terms · compliance disclaimer               │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 1. 顶栏 Header（sticky）

| 元素 | 说明 |
|------|------|
| Logo 区 | 文字 **TourGEO** · `font-weight: 700` · 色 `#1677A0` · 链 `/` |
| 导航 | MVP：`Features` 锚点 `#capabilities` · `Pricing` **disabled** 或隐藏 · 无下拉 |
| 主 CTA | **Log in** · `outline` 按钮 · `href={PUBLIC_ADMIN_URL}` |
| 移动端 | Logo 左 · 汉堡可选 P2；MVP 仅保留 **Log in** 右对齐 |

**环境变量**：`PUBLIC_ADMIN_URL`（例 `http://localhost:5173` 或生产 Admin 域名）· 未配置时 `#` + dev 注释。

---

## 2. Hero

| 元素 | 文案（英文 · 可微调） | 样式 |
|------|----------------------|------|
| 中文副标 | `旅获 AI · 入境游海外获客增长系统` | `0.875rem` · `#6B7280` · Hero 顶 |
| H1 | `Win overseas travelers where they ask AI` | `clamp(1.75rem, 5vw, 2.75rem)` · bold |
| 副文案 | `GEO visibility diagnostics, keyword insights, and AI-assisted content & landing pages — built for China inbound tour operators.` | `max-width: 42rem` · 居中 |
| 主按钮 | **Book a demo** | primary `#1677A0` · `mailto:hello@tourgeo.ai` 或 env `PUBLIC_DEMO_URL` · MVP 可用 `#contact` 锚点 |
| 次按钮 | **Log in to Admin** | outline · `PUBLIC_ADMIN_URL` |
| 合规小字 | `Sample-based AI visibility insights. No ranking guarantees.` | `0.8125rem` · muted |

**背景**：沿用 `landing.css` `.lp-hero` 渐变 `#eef6fa → #fff`。

**视觉（P2）**：右侧插画/产品截图占位 `hero-visual.svg`；MVP 可纯文案居中。

---

## 3. 价值主张 · 3 列（`#value`）

**区块标题**（可选）：`Why tour operators choose TourGEO`

| 列 | 图标 | 标题 | 正文 |
|----|------|------|------|
| 1 | chart / radar | **Measure** | See how often AI assistants mention your brand vs competitors across real traveler questions. |
| 2 | sparkles | **Act** | Prioritize keyword opportunities and generate review-ready scripts and English landing copy. |
| 3 | inbox | **Convert** | Publish compliant landing pages with lead forms and WhatsApp handoff — track inquiries in one place. |

**布局**：
- 桌面 `≥768px`：`grid 3 columns` · gap `1.5rem`
- 移动：单列 stack · 图标左 + 文案右 或 居中卡

**背景**：`.lp-section--alt`（`#F5F7FB`）

---

## 4. 能力卡片 · 4 项（`#capabilities`）

**区块标题**：`Everything you need for inbound growth`

| 卡片 | 标题 | 要点（3 bullet max） | 链 Admin（P2 tooltip） |
|------|------|----------------------|------------------------|
| GEO | **GEO Diagnostics** | Grounded AI sampling · Brand vs competitor score · Weekly/monthly schedules | `/diagnostics` |
| Keywords | **Keyword Intelligence** | Eight-stage traveler journey · Opportunity scoring · Export to content | `/keywords` |
| Content | **Content Agent** | Social scripts & storyboards · RAG from your knowledge base · Human review flag | `/content` |
| Landing | **Landing Page Agent** | English SSG pages · Turnstile lead forms · Publish in one click | `/landing` |

**卡片结构**（Astro 组件 `CapabilityCard.astro`）：

```
┌─ card (border · radius 8px · hover shadow) ─────────────┐
│ [icon 40px]  GEO Diagnostics                             │
│ · Grounded AI sampling across Perplexity, Gemini, OpenAI   │
│ · Visibility score with citation coverage                │
│ · Scheduled re-runs for US, UK, AU markets               │
└──────────────────────────────────────────────────────────┘
```

**布局**：
- 桌面 `≥1024px`：`2×2 grid`
- 平板 `768–1023px`：`2×2` 或 `2+2` 紧凑
- 移动：**单列**

**合规**：卡片内不出现「保证排名」「#1 in AI」类表述。

---

## 5. 闭环条（可选 · MVP 推荐）

单行流程图（水平 scroll 移动端）：

`Customer profile → GEO diagnosis → Keywords → Content → Landing page → Leads → Weekly report`

样式：小字号 · 箭头 `→` · 色 `#1677A0` 强调首尾。

---

## 6. CTA Band

| 元素 | 说明 |
|------|------|
| 背景 | `--lp-primary` 或 `#1677A0` · 白字 |
| 标题 | `Ready to see your brand in AI answers?` |
| 副文 | `Book a walkthrough with our team or sign in to your workspace.` |
| 按钮 | 同 Hero：**Book a demo**（accent 白底主色字）· **Log in to Admin**（outline 白边） |

**id**：`#contact` — Hero「Book a demo」可锚点到此。

---

## 7. 页脚 Footer

| 行 | 内容 |
|----|------|
| 品牌 | `© {year} TourGEO / 旅获 AI` |
| 链接 | `Privacy` · `Terms` — MVP `href="#"` 占位 · P2 静态页 |
| 合规 | `GEO and AI outputs are sampled at a specific time and platform set; not a guarantee of future visibility. Prices and visa information on generated pages require human confirmation.` |
| 技术 | `Powered by TourGEO Agent` · 不暴露内部 API URL |

**与 `LandingLayout` 页脚差异**：营销页写 **TourGEO** 品牌；客户落地页保留 `Tour by {brandLabel}` + 顾问确认价签/签证文案。

---

## 响应式

| 断点 | 行为 |
|------|------|
| `<768px` | 单列 · Hero 按钮 stack 全宽 · 3 列价值主张 → 1 列 · 顶栏仅 Logo + Log in |
| `768–1023px` | 价值 3 列或 2+1 · 能力 2 列 |
| `≥1024px` | 全宽 max `72rem` 内容区 · 能力 2×2 |

**触控**：按钮最小高度 `44px` · 间距 `≥8px`。

---

## SEO & Meta

| 标签 | 建议值 |
|------|--------|
| `<title>` | `TourGEO — AI Growth for China Inbound Tourism` |
| `description` | `GEO visibility diagnostics, keyword insights, and AI content for tour operators targeting US, UK, and Australia travelers.` |
| `lang` | `en` |
| `og:title` / `og:description` | 同 title/description · MVP 可无 `og:image` |
| canonical | `{PUBLIC_SITE_URL}/` |

**无** Turnstile · **无** `POST` 表单 · **无** 线索 API。

---

## Astro 实现建议

| 项 | 路径 |
|----|------|
| 页面 | `inbound-landing/src/pages/index.astro` |
| 布局 | `src/layouts/MarketingLayout.astro`（新建 · 含 header/footer slot） |
| 组件 | `src/components/marketing/Hero.astro` · `ValueProps.astro` · `CapabilityGrid.astro` · `CtaBand.astro` |
| 样式 | 扩展 `src/styles/landing.css` 或 `marketing.css` · 复用 `--lp-*` token |
| 与 `/p/...` | 路由共存 · `index.astro` 仅根路径 · 不改 `[slug].astro` |

**SSG**：纯静态 · `pnpm build` 无服务端数据依赖。

---

## 环境变量

| 变量 | 用途 | 默认 |
|------|------|------|
| `PUBLIC_ADMIN_URL` | Log in / Admin CTA | `#` |
| `PUBLIC_DEMO_URL` | Book a demo 外链（可选） | `mailto:hello@tourgeo.ai` |
| `PUBLIC_SITE_URL` | canonical | `http://localhost:4321` |

---

## 无障碍（WCAG AA）

- 正文对比度 ≥ 4.5:1（主色按钮白字已满足 tokens.md）
- 焦点环可见 · 跳过链接 P2
- 图标装饰 `aria-hidden` · 按钮文案自解释（避免单独图标按钮）
- 锚点跳转 `#capabilities` / `#contact` 考虑 `scroll-margin-top`（sticky header）

---

## MVP 范围边界

| 包含 | 不包含 |
|------|--------|
| `/` 营销首页 SSG | 定价/注册自助 |
| Hero + 3 价值列 + 4 能力卡 | 客户案例视频 |
| Demo + Admin CTA | Turnstile 联系表单 |
| 合规页脚 | PostHog / GTM |
| 移动单列 | i18n 路由 `/zh` |

---

## 版本

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-11 | UI 设计 | Sprint #3 营销门户 MVP · Hero/价值/能力/CTA/页脚 · 替换 index 占位 |
