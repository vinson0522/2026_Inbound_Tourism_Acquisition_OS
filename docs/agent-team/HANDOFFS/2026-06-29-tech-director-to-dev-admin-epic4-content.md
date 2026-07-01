# HANDOFF | 技术总监 → 开发（Admin）

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-4 M1 · **FR-301/302** |

## 上下文

**当前状态**：关键词页已有「创建内容」占位或 P2 链；Java API 由 [Java HANDOFF](2026-06-29-tech-director-to-dev-java-epic4-content.md) 提供。

**相关文件**：
- `docs/design/wireframes/content-task-list.md`
- `inbound-admin/src/views/tourgeo/keywords/index.vue` — 入口
- `inbound-admin/src/api/tourgeo/keyword.ts` — 参考 API 封装

## 交付请求

**需要什么**：内容任务列表 + 创建 + 脚本预览（FR-301/302 MVP UI）。

**验收标准**：
- [ ] 路由与线框一致；`src/api/tourgeo/content.ts`
- [ ] 列表 + 分页 + status 筛选
- [ ] 「从关键词创建」弹窗（keyword 下拉 / query keywordId 预填）
- [ ] 「生成脚本」→ loading → drawer 展示 hook/script/storyboard/CTA
- [ ] `needs_human_review` 标签醒目
- [ ] 关键词页「创建内容」跳转生效

## 窗口激活 Prompt 摘要

```
角色：开发 Admin。必读 content-task-list 线框与 Java content API。
任务：内容任务列表 + 脚本预览 drawer。
```

---

## Done（由 To 角色填写）

- **完成时间**：2026-07-01
- **结果摘要**：
  - `src/api/tourgeo/content.ts` + `constants/content.ts` + types
  - 路由 `/content-tasks/index`、隐藏 `/projects/:projectId/content-tasks`
  - 列表 + 筛选（status/platform 服务端；keyword/needsHumanReview 当前页客户端）+ 创建抽屉 + 关键词选择
  - 「生成脚本」loading → 预览 drawer（hook/script/voiceover/storyboard/CTA + 待审核 Tag）
  - 关键词页「创建内容」→ `?action=create&keywordId=` 预填
  - `pnpm vite build` ✅
- **遗留**：M2 详情页编辑、采纳/驳回 API、服务端 needsHumanReview 筛选
