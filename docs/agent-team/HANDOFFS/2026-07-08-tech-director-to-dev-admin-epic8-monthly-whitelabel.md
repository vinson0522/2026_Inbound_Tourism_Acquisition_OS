# HANDOFF | 技术总监 → 开发（Admin）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-08 | EPIC-8 M2 · FR-703/704 · ADR-20260708-21 |

## 上下文

M1 `/reports` 已有周报；「月报」「模板配置」按钮 disabled。M2 启用并新增设置页。

**相关文件**：
- `inbound-admin/src/views/tourgeo/reports/index.vue`
- `inbound-admin/src/api/tourgeo/report.ts`
- `inbound-admin/src/views/tourgeo/settings/billing/` — 设置页参考

## 交付请求

**验收标准**：
- [x] `report.ts`：`createMonthlyReport` · `getReportTemplate` · `saveReportTemplate`
- [x] `/reports`：启用「生成月报」dialog（年月选择 · 默认上月）
- [x] 列表支持 `type=MONTHLY` · Tag「增长月报」
- [x] 新建 `/settings/report-template`：Logo URL · 封面/公司/页脚 · 主色 · 章节开关 · 保存
- [x] 侧栏「系统设置 → 报告模板」
- [x] `pnpm build:prod` ✅

## Prompt

```
角色：开发 Admin。必读 reports/index.vue、report-template-settings 线框、HANDOFF 2026-07-08-tech-director-to-dev-admin-epic8-monthly-whitelabel.md。
任务：月报 dialog + 报告模板设置页 · build:prod。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-08
- **结果摘要**：
  - `report.ts` 月报 + 模板 API
  - `/reports` 月报 dialog · MONTHLY 预览 drawer（MoM · CRM 五态 · 5 条建议）
  - `/settings/report-template` 白标表单 + 封面 mock · 恢复默认 · dirty 提示
  - 路由「系统设置 → 报告模板」
  - `pnpm build:prod` ✅
- **遗留**：若依菜单权限 `tourgeo:report:monthly/template` 待配置
