# HANDOFF | 技术总监 → UI 设计

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | UI 设计 | 2026-07-03 | EPIC-6 M2 · [Sprint](2026-07-03-tech-director-epic6-m2-landing-publish-sprint.md) |

## 交付

- [x] `docs/design/wireframes/landing-page-publish.md` — M2 发布/预览/下线（可引用并扩展 `landing-page-list.md`）
- [x] Admin 预览 drawer 增补：「发布」「打开公网预览」「下线」按钮态（DRAFT / PUBLISHED / ARCHIVED）
- [x] Astro 公网页线框：Hero → 八模块 → 表单区（Turnstile）→ WhatsApp CTA
- [x] 空态/错误：slug 冲突、未发布 404、Turnstile 失败 toast 文案
- [x] HANDOFF → `2026-07-03-ui-to-developer-landing-publish.md`

## 约束

- 公网 URL：`/p/{projectId}/{slug}`（非裸 slug）
- M2 不做 GrapesJS、A/B、自定义域名

## Prompt

```
角色：UI 设计。必读 landing-page-list.md、EPIC-6 M2 Sprint、ADR-16。
任务：landing-page-publish.md — Admin 发布流 + Astro 页面结构 + 表单 Turnstile 区。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-03
- **结果摘要**：`landing-page-publish.md` Admin 发布/下线/外链 + Astro 八模块 + Turnstile 表单；HANDOFF 开发
- **遗留**：WhatsApp 点击追踪 FR-602 M3；PostHog M3；GrapesJS 编辑
