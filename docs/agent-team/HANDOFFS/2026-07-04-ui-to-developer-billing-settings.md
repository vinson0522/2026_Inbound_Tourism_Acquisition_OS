# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-07-04 |
| **优先级** | High |
| **关联** | EPIC-9 M1 · FR-804 · [技术总监 → UI](2026-07-04-tech-director-to-ui-epic9-billing-settings.md) · ADR-20260704-17 |

## 上下文

**当前状态**：EPIC-1~8 闭环 ✅；各生成 API 无额度展示。DDL `subscription` + demo seed 已就绪。

**相关文件**：
- `docs/design/wireframes/billing-settings.md` — 线框 + 6 quota + alert 规范
- `database/ddl/001_schema.sql` — `subscription`
- `database/ddl/002_seed_demo.sql` — `quota_json` 示例
- Java HANDOFF：`2026-07-04-tech-director-to-dev-java-epic9-billing.md`

**约束**：
- 租户级只读页；无项目选择器
- M1 无升级/购买/发票按钮（disabled + tooltip）
- 402 全局提示与页内超额文案一致

## 交付请求

**需要什么**：Admin `/settings/billing` 套餐与额度只读页。

**验收标准**：
- [ ] 路由 `/settings/billing`；菜单「系统设置 → 套餐与额度」
- [ ] 展示：planName/planCode、status、periodStart~periodEnd、剩余天数
- [ ] 6 项 `el-progress`：projects + 5× monthly（键与 seed 一致）
- [ ] 颜色：<80% 正常 · 80–99% warning · ≥100% exception + danger Tag
- [ ] 页顶 error alert（任一超额）；warning alert（≥80% 无超额）
- [ ] 升级/购买/发票 disabled + tooltip
- [ ] Axios 402 → `ElMessage.error`（message 来自后端）
- [ ] `pnpm build:prod` ✅

## 后端依赖

- [ ] `GET /api/v1/settings/billing`（或等价 current subscription）
- [ ] 响应含 quota/used 或预计算 `quotas[]`

## 质量 / 证据

**必须提供**：正常用量截图；模拟超额（landing 20/20）截图 + 402 toast 截图

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
