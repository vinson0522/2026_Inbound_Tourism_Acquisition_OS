# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发（Landing） |
| **日期** | 2026-07-11 |
| **优先级** | High |
| **关联** | Sprint #3 · [技术总监 → UI](2026-07-11-tech-director-to-ui-marketing-portal.md) |

## 上下文

**当前状态**：`inbound-landing/src/pages/index.astro` 为 EPIC-6 占位页。需替换为 **TourGEO 营销门户**（非 `/p/...` 客户落地页）。

**相关文件**：
- `docs/design/wireframes/marketing-portal-home.md` — 完整线框
- 客户落地页参考：`src/layouts/LandingLayout.astro` · `src/styles/landing.css`
- Landing HANDOFF：`2026-07-11-tech-director-to-dev-landing-portal-home.md`

**约束**：
- 英文为主 · 中文副标可选一行
- 移动单列 · 复用 `--lp-*` 品牌色 `#1677A0`
- **不**使用客户 `LandingLayout`（`Tour by {brand}`）
- 无 Turnstile · 无 public leads API · 无 PostHog M3
- 不承诺排名 · 页脚合规 disclaimer 必须展示

## 交付请求

**需要什么**：Astro SSG 营销首页 · 与 `/p/[projectId]/[slug]` 共存。

**验收标准**：
- [x] 替换 `index.astro` · 新建 `MarketingLayout.astro`（header + footer）
- [x] **Hero** — H1 + 中文副标 + 双 CTA（Book demo · Log in Admin）
- [x] **价值主张** — 3 列 Measure / Act / Convert · 移动单列
- [x] **能力卡片** — GEO / Keywords / Content / Landing 共 4 张 · 2×2 桌面 · 移动 1 列
- [x] **CTA band** — 底部主色条 + 重复双按钮
- [x] **页脚** — © · Privacy/Terms 占位 · 采样/不承诺排名 disclaimer
- [x] `PUBLIC_ADMIN_URL` 驱动 Log in 链接 · demo 可用 `mailto:` 或 `#contact`
- [x] SEO：`title` + `meta description` + `lang=en`
- [x] Hero/页脚合规小字可见
- [x] `pnpm build` ✅ · `curl localhost:4321/` **200**

## 组件建议

```
src/layouts/MarketingLayout.astro
src/components/marketing/Hero.astro
src/components/marketing/ValueProps.astro
src/components/marketing/CapabilityGrid.astro
src/components/marketing/CtaBand.astro
```

## 质量 / 证据

**必须提供**：
- 桌面全页截图（Hero → footer）
- 移动端 `<768px` 单列截图
- Log in 按钮指向 `PUBLIC_ADMIN_URL` 配置说明
- build 成功 + curl 200 日志

---

## Done（由 To 角色填写）

- **完成时间**：2026-07-11
- **结果摘要**：TourGEO 营销门户 MVP · 与 `/p/...` 共存 · build + HTTP 200
- **遗留**：Privacy/Terms 静态页 P2 · hero 插画 P2
