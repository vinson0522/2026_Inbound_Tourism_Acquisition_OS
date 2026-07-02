# HANDOFF | 技术总监 → 开发（Admin）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 Admin | 2026-07-04 | EPIC-9 M1 · FR-804 |

## 交付

- [x] `src/api/tourgeo/billing.ts` + types（`SubscriptionVo`, `QuotaItem`）
- [x] `views/tourgeo/settings/billing/index.vue` — 套餐名 + 周期 + 6 项 el-progress
- [x] 超额项标红 + el-alert「联系升级」
- [x] 侧栏「系统设置 → 套餐与额度」
- [x] 全局：Axios 拦截 402 → ElMessage 展示升级提示（若尚未有）
- [x] `pnpm build:prod` ✅

## Prompt

```
角色：开发 Admin。必读 billing-settings 线框 + Java GET subscription API。
任务：只读计费页；M1 无升级按钮（disabled + tooltip）。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-02
- **结果摘要**：
  - `src/api/tourgeo/billing.ts` + `types.ts`（`SubscriptionVo` / `QuotaItemVo`）
  - `src/constants/billing.ts` — 套餐/状态映射
  - `views/tourgeo/settings/billing/index.vue` + `QuotaProgressRow.vue` — 套餐概览 + 6 项进度条 + 超额/预警 alert
  - 路由 `/settings/billing`（系统设置 → 套餐与额度）
  - `utils/quotaError.ts` + `request.ts` — HTTP 402 / code 40201 全局 `ElMessage.error`（3s 防抖）
  - `pnpm build:prod` ✅
- **遗留**：P2 402 toast 内链「查看额度」；`tourgeo:billing:view` 若依菜单权限 DB 配置
