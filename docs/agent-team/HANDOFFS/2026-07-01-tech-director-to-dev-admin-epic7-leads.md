# HANDOFF | 技术总监 → 开发（Admin）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 Admin | 2026-07-01 | EPIC-7 M1 · FR-601 |

## 交付

- [x] `src/api/tourgeo/lead.ts`
- [x] `views/tourgeo/leads/index.vue` — 列表 + status 筛选 + 详情 drawer
- [x] 侧栏「线索与转化」入口
- [x] 空态引导「暂无询盘，发布落地页后可见」

## Prompt

```
角色：开发 Admin。必读 leads-list 线框 + Java leads API。
任务：线索列表页。
```

## Done

- **完成时间**：2026-07-01
- **结果摘要**：`lead.ts` + `constants/lead.ts` + `utils/maskPii.ts`；`/leads/index` 与 `/projects/:id/leads`；列表脱敏、详情 drawer（联系人/行程/UTM/设备）、合规 alert；`pnpm build:prod` ✅
- **遗留**：导出/状态流转/AI 跟进按钮 disabled（M2）；落地页/关键词筛选为当前页客户端过滤
