# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-07-03 |
| **优先级** | High |
| **关联** | EPIC-6 M2 · FR-505/507 子集 · [技术总监 → UI](2026-07-03-tech-director-to-ui-epic6-landing-publish.md) · ADR-20260703-16 |

## 上下文

**当前状态**：EPIC-6 M1 列表 + Admin JSON 预览 ✅；EPIC-7 public leads ✅。缺公网 Astro 页与发布按钮。

**相关文件**：
- `docs/design/wireframes/landing-page-publish.md` — Admin 发布/下线 + Astro 八模块 + Turnstile
- `docs/design/wireframes/landing-page-list.md` — M1 预览 drawer（M2 底栏增补）
- `docs/design/wireframes/leads-list.md` — public leads 字段
- `docs/design/tokens.md` — 公网页品牌色
- Java/Astro HANDOFF：`2026-07-03-tech-director-to-dev-java-epic6-landing-publish.md`、`2026-07-03-tech-director-to-dev-landing-epic6-astro.md`

**约束**：
- 公网 URL `/p/{projectId}/{slug}`（ADR-16）
- 八模块 key 与 AI 一致：`hero` … `whatsapp_cta`
- Turnstile → `X-Turnstile-Token` → existing `POST /api/v1/public/leads`
- 无 GrapesJS、无自定义域名、无 PostHog

## 交付请求

### Admin

- [ ] 预览 drawer：发布 / 打开公网预览 / 下线（按 status 矩阵）
- [ ] 发布/下线 confirm dialog
- [ ] 列表：`published_url`、`published_at` 列；行操作公网预览
- [ ] 发布成功 toast 含复制链接
- [ ] `landing.ts`：`publishLandingPage` · `unpublishLandingPage`

### Astro（`inbound-landing`）

- [ ] `/p/[projectId]/[slug].astro` fetch public API
- [ ] `LandingModuleRenderer` — 8 模块
- [ ] `LeadForm` + Turnstile + submit leads
- [ ] SEO meta + FAQ JSON-LD
- [ ] 404 友好页 · Turnstile/lead 错误文案

## 后端依赖

- [ ] `GET /api/v1/public/landing-pages/{slug}?projectId=`
- [ ] `POST .../publish` · `POST .../unpublish`
- [ ] CORS + Turnstile siteverify（secret 有时）

## 验收标准

- [ ] READY 页发布 → `PUBLISHED` + `publishedUrl`
- [ ] 公网页八模块可见 + 表单提交 → Admin `/leads`
- [ ] 下线 → 公网 404
- [ ] 无 Turnstile secret 时 dev 可提交
- [ ] `pnpm build:prod`（Admin）+ `pnpm build`（Astro）

## 质量 / 证据

**必须提供**：发布成功截图；公网页移动视图；表单提交 + leads 列表截图

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
