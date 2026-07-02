# HANDOFF | 技术总监 → 开发（Admin）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-06 | EPIC-3 M2 · FR-203 · ADR-20260706-19 |

## 上下文

`/keywords/index` 已实现机会分列，M1 为占位（`score == null` → `—` + tooltip「FR-203 待上线」）。Java M2 提供 score API 后，Admin 接真实数据与操作。

**相关文件**：
- `inbound-admin/src/views/tourgeo/keywords/index.vue`
- `inbound-admin/src/api/tourgeo/project.ts` 或 keywords API
- `docs/design/wireframes/keywords-list.md` — 机会分列已定义

**约束**：无新路由；最小 diff 改现有页。

## 交付请求

**验收标准**：
- [x] API 封装：`scoreKeyword(projectId, keywordId)` · `scoreKeywordsBatch(projectId, keywordIds?)`
- [x] 工具栏增加 **「刷新评分」** 按钮（对当前 Tab/筛选结果批量，或全项目 Top 50）
- [x] 去掉 M1 占位 tooltip/hint；有 score 时 tooltip 展示 `score_detail_json` 五维摘要
- [x] 表格默认排序：**机会分 DESC**（无 score 排最后）
- [x] 单条操作列可选「评分」链接（调用单条 API + loading）
- [x] `pnpm build:prod` ✅

## Prompt

```
角色：开发 Admin。必读 keywords/index.vue、keywords-list.md 机会分列。
任务：接 score API · 刷新评分 · 分项 tooltip · 默认 score 排序。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-06
- **结果摘要**：`keyword.ts` score/batch API · `keywords/index.vue` 刷新评分 + 行内评分 · 五维 tooltip · 默认 `orderByColumn=score&isAsc=desc` · `pnpm build:prod` ✅
- **遗留**：无
