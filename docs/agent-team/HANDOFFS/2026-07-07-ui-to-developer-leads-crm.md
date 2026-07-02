# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-07-07 |
| **优先级** | High |
| **关联** | EPIC-7 M2 · FR-605 · [技术总监 → UI](2026-07-07-tech-director-to-ui-epic7-leads-crm.md) · ADR-20260707-20 |

## 上下文

**当前状态**：M1 `/leads` 列表 + 只读详情 drawer ✅。M2 补销售侧 CRM 操作。

**相关文件**：
- `docs/design/wireframes/leads-list.md` — **§ M2 增量**（状态/跟进/负责人）
- M1 实现：`inbound-admin/src/views/tourgeo/leads/index.vue`
- Java HANDOFF：`2026-07-07-tech-director-to-dev-java-epic7-leads-crm.md`

**约束**：
- 状态机：NEW→FOLLOWING→QUOTED→WON；非终态→LOST；WON/LOST 锁定
- 列表 PII 脱敏不变
- 无 CSV 导出 · 无 AI 跟进 FR-603

## 交付请求

**需要什么**：在现有 leads 页详情 drawer 上叠加 CRM 能力。

**验收标准**：
- [ ] Drawer 640px · Tab「CRM 跟进」|「线索信息」（或等价分区）
- [ ] 状态下拉 + 保存 · 非法/终态 disabled + tooltip
- [ ] 负责人展示 + 下拉/「指派给我」→ PATCH
- [ ] 添加跟进：content 必填 · channel 可选（email/phone/whatsapp/meeting）
- [ ] `el-timeline` 降序 · 操作人 + 时间 + 渠道 Tag
- [ ] 列表五色 status Tag · 筛选联动
- [ ] 权限 `tourgeo:lead:edit` · 只读角色禁用表单
- [ ] 合规 footnote 保留
- [ ] `pnpm build:prod` ✅

## 后端依赖

- [ ] `PATCH .../leads/{leadId}` — status / assigneeId
- [ ] `GET/POST .../leads/{leadId}/followups`

## 质量 / 证据

**必须提供**：状态 NEW→FOLLOWING→QUOTED 截图；跟进时间线 + 添加记录；终态 WON disabled 截图

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
