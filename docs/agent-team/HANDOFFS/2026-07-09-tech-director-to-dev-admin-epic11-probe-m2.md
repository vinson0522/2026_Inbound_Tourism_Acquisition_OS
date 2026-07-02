# HANDOFF | 技术总监 → 开发（Admin）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-09 | EPIC-11 M2 · FR-115/116 · ADR-20260709-22 |

## 交付请求

- [ ] `/settings/probe-adapters` — 列表 · 编辑 drawer · JSON 字段 · enabled 开关
- [ ] `diagnostics/detail.vue` 新增 **「校准对比」** Tab — 调 calibration API · 偏差率 · 对比表
- [ ] 诊断创建表单：`calibration_ratio` 滑块/输入（0–30%）已有 UI 则接后端
- [ ] `pnpm build:prod` ✅

## Prompt

```
角色：开发 Admin。必读 probe-nodes 设置页、diagnostics/detail、HANDOFF 2026-07-09-tech-director-to-dev-admin-epic11-probe-m2.md。
任务：probe-adapters 页 + 校准 Tab · build:prod。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-09
- **结果摘要**：
  - `/settings/probe-adapters` — 列表 · 640px 编辑 drawer · JSON 三块 textarea 格式化/校验 · enabled 开关 · 复制 JSON
  - `diagnostics/detail.vue` — 「校准对比」Tab（可见性：calibrationRatio>0 + 双模式）· KPI · 展开双栏对比 · footnote 链 adapter 设置
  - 创建诊断：`calibration_ratio` 滑块接后端（0–30% → 0–0.3）· 未选 extension 时 disabled
  - `api/tourgeo/probe.ts` + `diagnostic.ts` · 路由 · `pnpm build:prod` ✅
- **遗留**：权限 `tourgeo:probe:adapter:edit` 待 Casbin 配置（M2 登录用户默认可编辑）
