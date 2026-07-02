# HANDOFF | 技术总监 → 开发（Admin）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-07 | EPIC-7 M2 · FR-605 · ADR-20260707-20 |

## 上下文

M1 `/leads` 列表 + 只读详情 drawer 已实现。M2 接 CRM API，使销售可操作线索。

**相关文件**：
- `inbound-admin/src/views/tourgeo/leads/index.vue`
- `inbound-admin/src/api/tourgeo/lead.ts`
- `docs/design/wireframes/leads-list.md` — UI 完成 M2 节后对齐

## 交付请求

**验收标准**：
- [x] API：`patchLead` · `listFollowups` · `createFollowup`
- [x] 详情 drawer：状态下拉 + 保存 · 负责人展示/指派 · 跟进 timeline · 添加跟进表单
- [x] 列表：状态 Tag 五色 · 筛选联动已有 query
- [x] 状态变更成功 toast · 终态禁用编辑
- [x] `pnpm build:prod` ✅

## Prompt

```
角色：开发 Admin。必读 leads/index.vue、leads-list.md M2、HANDOFF 2026-07-07-tech-director-to-dev-admin-epic7-leads-crm.md。
任务：CRM drawer 状态/跟进/负责人 · build:prod。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-07
- **结果摘要**：`lead.ts` CRM API · drawer 双 Tab（CRM 跟进/线索信息）· 状态保存 · 指派给我 · 跟进 timeline · `pnpm build:prod` ✅
- **遗留**：无 · C17 commit 待用户指令
