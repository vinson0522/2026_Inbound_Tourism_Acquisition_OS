# HANDOFF | 技术总监 → 开发（Admin）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 Admin | 2026-07-03 | EPIC-6 M2 · FR-507 子集 |

## 交付

- [x] `landing.ts` 增补 `publishLandingPage` · `unpublishLandingPage`
- [x] `views/tourgeo/landing/index.vue` 预览 drawer：
  - **发布**（DRAFT/READY → confirm → publish API）
  - **打开公网预览**（PUBLISHED → `publishedUrl` 或 `{LANDING_BASE}/p/{projectId}/{slug}` 新窗口）
  - **下线**（PUBLISHED → unpublish）
- [x] 列表 status 列：`PUBLISHED` Tag 绿色 + Link 图标；操作「公网预览 ↗」
- [x] 列表列：公网链接 · 发布时间
- [x] 发布成功 ElNotification 含「复制链接」「打开预览 ↗」
- [x] `pnpm build:prod` ✅

## 依赖

- Java `POST .../publish` · `POST .../unpublish`
- 线框 `landing-page-publish.md`

## Prompt

```
角色：开发 Admin。必读 landing-page-publish 线框 + Java publish API。
任务：落地页发布/下线 + 公网预览外链。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-03
- **结果摘要**：
  - `landing.ts` publish/unpublish · `LandingPublishResult` 类型
  - 列表 + drawer：发布确认 / 下线确认 / 公网预览 / 复制链接 / Admin JSON dialog
  - Java `LandingPageVo` 增补 `publishedUrl` · `publishedAt`（列表展示）
  - `constants/landing.ts` — `resolvePublishedUrl` · `VITE_LANDING_PUBLIC_BASE_URL`
  - `pnpm build:prod` ✅
- **遗留**：C13 全栈 commit 待三端齐 · 真机联调需 Java + Astro :4321
