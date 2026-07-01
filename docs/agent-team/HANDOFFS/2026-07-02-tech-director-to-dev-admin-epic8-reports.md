# HANDOFF | 技术总监 → 开发（Admin）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 Admin | 2026-07-02 | EPIC-8 M1 · FR-701/702 |

## 上下文

**当前状态**：诊断详情页有「导出 DOCX/PDF」，无 `/reports` 路由。

**依赖**：UI `reports-list.md` + Java report API（可并行，字段以 HANDOFF/Java 为准）

## 交付请求

**需要什么**：Admin 报告中心页

**验收标准**：
- [x] `src/api/tourgeo/report.ts` + types
- [x] `views/tourgeo/reports/index.vue` — 列表 + type 筛选 + 分页
- [x] 「生成本周报告」→ `POST .../reports/weekly` → 刷新列表
- [x] 行操作：下载 DOCX / PDF（blob 下载）；DIAGNOSTIC 可跳转 `/diagnostics/runs/:runId`
- [x] 侧栏「报告中心」入口；`/reports` 与 `/projects/:id/reports`
- [x] 空态 + loading；`pnpm build:prod` ✅

## Prompt

```
角色：开发 Admin。必读 reports-list 线框 + Java report API。
任务：报告中心列表页；复用 diagnostic 导出 download 模式。
```

## Done

- **完成时间**：2026-07-02
- **结果摘要**：`report.ts` + `constants/report.ts`；`/reports` 侧栏 + 列表/筛选/周报 dialog/预览 drawer/DOCX·PDF 下载；DIAGNOSTIC → 诊断详情；`pnpm build:prod` ✅
- **遗留**：依赖 Java report API 联调；创建时间筛选为当前页客户端过滤
