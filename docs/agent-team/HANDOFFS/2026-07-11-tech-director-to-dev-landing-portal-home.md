# HANDOFF | 技术总监 → 开发（Landing）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 Landing | 2026-07-11 | 门户 MVP · [Sprint #3](2026-07-11-tech-director-sprint3-productization-parallel.md) |

## 上下文

替换 `index.astro` 占位页。UI 线框可能并行完成——可先按 HANDOFF 模块实现，UI 完成后对齐视觉。

**相关文件**：
- `inbound-landing/src/pages/index.astro`
- `inbound-landing/src/layouts/`（如有 LandingLayout 复用）
- `docs/design/wireframes/marketing-portal-home.md`（UI 产出后）

## 交付请求

- [x] Astro SSG 营销首页 · 与 `/p/[projectId]/[slug]` 路由共存
- [x] Hero + 能力卡片 + CTA 链到 Admin 登录 URL（env `PUBLIC_ADMIN_URL` 或占位 `#`）
- [x] 基础 SEO meta · 无 Turnstile
- [x] `pnpm build` ✅ · curl localhost:4321 200

## Done（由 To 角色填写）

- **完成时间**：2026-07-11
- **结果摘要**：`MarketingLayout` + Hero/ValueProps/CapabilityGrid/WorkflowStrip/CtaBand · `index.astro` prerender SSG · `PUBLIC_ADMIN_URL`/`PUBLIC_DEMO_URL` · `pnpm build` ✅ · HTTP 200
