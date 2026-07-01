# HANDOFF | 技术总监 → 开发（Admin）

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-07-01 |
| **优先级** | High |
| **关联** | EPIC-6 M1 · **FR-501~505** |

## 上下文

**当前状态**：内容/关键词页可链落地页；Java API 由 [Java HANDOFF](2026-07-01-tech-director-to-dev-java-epic6-landing.md) 提供。

**相关文件**：
- `docs/design/wireframes/landing-page-list.md`
- `inbound-admin/src/views/tourgeo/content/` — 入口参考

## 交付请求

**需要什么**：落地页列表 + 创建 + AI 生成 + JSON/SEO 预览 drawer。

**验收标准**：
- [ ] `src/api/tourgeo/landing.ts`；路由与线框一致
- [ ] 创建弹窗（keyword、template_type、slug）；「AI 生成页面」
- [ ] 预览 drawer：模块摘要 + seo_meta + 待审核标签
- [ ] 关键词/内容页「创建落地页」入口

## 窗口激活 Prompt 摘要

```
角色：开发 Admin。必读 landing-page-list 线框与 Java landing API。
任务：落地页列表 + 预览 drawer。
```

---

## Done（由 To 角色填写）

- **完成时间**：2026-07-01
- **结果摘要**：
  - `src/api/tourgeo/landing.ts` + `constants/landing.ts` + `types.ts`
  - `views/tourgeo/landing/index.vue` — 列表/筛选/创建 dialog/预览 drawer
  - 路由 `/landing-pages/index` + `/projects/:projectId/landing-pages`
  - 关键词页「转落地页」FR-205 query 入口
  - `pnpm build:prod` ✅
- **遗留**：check-slug 专用 API 未实现（M1 客户端 slug 校验 + 创建错误兜底）；M2 编辑/导出 HTML
