# EPIC-6 M2 落地页发布 Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-03 |
| **优先级** | High |
| **关联** | EPIC-6 M2 · **FR-505/506/507 子集** · ADR-20260703-16 |
| **前置** | EPIC-6 M1 ✅ · EPIC-7 M1 public leads ✅ · C12 `e127485` |

## 目标（M2 MVP）

**Admin 发布 → Astro 公网预览 → Turnstile 表单 → 线索落库**

| 范围 | M2 做 | M2 不做 |
|------|-------|---------|
| FR-505 | 公网页渲染 `form_config_json` + WhatsApp CTA | 字段后台动态配置 UI |
| FR-506 | Astro HTML 公网预览（非文件导出） | DOCX/Markdown 导出 |
| FR-507 | 本地/staging `inbound-landing :4321` + slug URL | 自定义域名/CDN 托管 |
| Java | `GET /api/v1/public/landing-pages/{slug}` · `POST .../publish` | MQ 异步发布 |
| Turnstile | Astro widget + `X-Turnstile-Token` → 已有 public leads | 生产 Key 轮换 UI |
| Admin | 发布/下线 + 「打开预览」外链 | GrapesJS 编辑 |

## 公网 URL 约定（ADR-16）

slug 在 DDL 为 `(project_id, slug)` 唯一 → **公网路径带 projectId**：

```
http://localhost:4321/p/{projectId}/{slug}
GET /api/v1/public/landing-pages/{slug}?projectId={id}   → 仅 status=PUBLISHED
POST /api/v1/projects/{projectId}/landing-pages/{pageId}/publish
```

`published_url` = `{inbound.landing.public-base-url}/p/{projectId}/{slug}`

## 任务拆分

| # | 角色 | HANDOFF | 依赖 | 验收 |
|---|------|---------|------|------|
| **1** | **UI 设计** | [→ 发布/预览线框](2026-07-03-tech-director-to-ui-epic6-landing-publish.md) | — | `landing-page-publish.md` |
| **2** | **开发 Java** | [→ publish + public API](2026-07-03-tech-director-to-dev-java-epic6-landing-publish.md) | — | smoke |
| **3** | **运维** | [→ landing compose](2026-07-03-tech-director-to-devops-epic6-landing-compose.md) | — | `:4321` healthy |
| **4** | **开发 Landing** | [→ Astro scaffold](2026-07-03-tech-director-to-dev-landing-epic6-astro.md) | #2 | 8 模块 + 表单 |
| **5** | **开发 Admin** | [→ 发布按钮](2026-07-03-tech-director-to-dev-admin-epic6-landing-publish.md) | #1+#2 | build + 链预览 |

**无 Python M2**（复用 M1 `content_json` 八模块）。

## 窗口 Prompt 摘要

| 角色 | Prompt |
|------|--------|
| UI | `landing-page-publish.md 补充 M2 发布/预览/下线` |
| Java | `public landing GET + publish/unpublish + CORS + Turnstile siteverify 可选` |
| 运维 | `inbound-landing Dockerfile + compose :4321 + env 文档` |
| Landing | `Astro 4 /p/[projectId]/[slug] + LeadForm Turnstile → public/leads` |
| Admin | `发布/下线 + 预览外链 + PUBLISHED 状态` |

## 完成后

- 各 HANDOFF Done + `MEMORY.md` EPIC-6 M2 ✅
- **技术总监签核（2026-07-03）**：功能 ✅ · 五棒齐 · **C13 commit ⏳**
- **闭环验收**：发布 → Astro 预览 → 表单 → Admin `/leads`
