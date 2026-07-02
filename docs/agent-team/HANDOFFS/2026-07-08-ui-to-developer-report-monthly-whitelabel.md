# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-07-08 |
| **优先级** | High |
| **关联** | EPIC-8 M2 · FR-703/704 · [技术总监 → UI](2026-07-08-tech-director-to-ui-epic8-monthly-whitelabel.md) · ADR-20260708-21 |

## 上下文

**当前状态**：M1 `/reports` 列表 + 周报 dialog + 预览 drawer + 导出 ✅。「月报」「模板配置」按钮 disabled。

**相关文件**：
- `docs/design/wireframes/reports-list.md` — **§M2 增量**（月报 dialog · MONTHLY 预览 · 工具栏启用）
- `docs/design/wireframes/report-template-settings.md` — 白标配置页 + 封面预览 mock
- M1 实现：`inbound-admin/src/views/tourgeo/reports/index.vue` · `src/api/tourgeo/report.ts`
- 设置页参考：`views/tourgeo/settings/billing/` · [billing-settings.md](../../design/wireframes/billing-settings.md)
- Java HANDOFF：`2026-07-08-tech-director-to-dev-java-epic8-monthly-whitelabel.md`
- Admin HANDOFF：`2026-07-08-tech-director-to-dev-admin-epic8-monthly-whitelabel.md`

**约束**：
- M2 无 FR-705 推送 · FR-706 自定义 · XXL-Job · LLM 摘要 · MinIO Logo 上传
- 月报扣额复用 `reports_per_month`（402 链 billing 页）
- 线索 status 中文与 [leads-list.md](../../design/wireframes/leads-list.md) M2 一致
- 导出白标由 Java 渲染；Admin 仅配置 + 预览 mock

## 交付请求

**需要什么**：报告中心 M2 月报入口 + 租户报告模板设置页。

### A. 报告列表 `/projects/:projectId/reports`

**验收标准**：
- [ ] 工具栏启用「生成月报」→ dialog（`el-date-picker type="month"` · 默认上一完整自然月）
- [ ] 只读自然月区间 · MoM 说明文案 · 聚合章节列表（见线框）
- [ ] 未配置模板时 dialog 顶栏 info alert + link「去配置模板」
- [ ] `POST .../reports/monthly` body `{ year, month }` → 刷新列表 → toast
- [ ] 402 额度不足提示 + link `/settings/billing`
- [ ] 「模板配置」→ `/settings/report-template`
- [ ] 列表 `type=MONTHLY` · Tag「增长月报」· 筛选含 MONTHLY
- [ ] 预览 drawer：`type=MONTHLY` 展示 MoM · 五态 CRM · 5 条建议 · `templateSnapshot`
- [ ] 权限 `tourgeo:report:monthly`

### B. 报告模板 `/settings/report-template`

**验收标准**：
- [ ] 侧栏「系统设置 → 报告模板」
- [ ] 表单：logoUrl · coverTitle · companyName · primaryColor · footerText · sections checklist（≥1）
- [ ] 右侧封面预览 mock 随表单实时更新
- [ ] Logo URL 预览 / 加载失败提示
- [ ] `GET/PUT /api/v1/settings/report-template` · 保存 toast · 「恢复默认」confirm
- [ ] 未保存离开 dirty 提示
- [ ] 权限 `tourgeo:report:template` · 只读禁用保存
- [ ] `pnpm build:prod` ✅

## 后端依赖（与 Java 开发对齐）

- [ ] `POST /api/v1/projects/{projectId}/reports/monthly` — `{ year, month }` 或 `{ periodStart, periodEnd }` ≤62 天
- [ ] `GET /api/v1/settings/report-template` — 无记录返回默认 config
- [ ] `PUT /api/v1/settings/report-template` — upsert `template.type=REPORT`
- [ ] 月报 summary JSON 含 `geo.momDelta` · `leads.byStatus` · `leads.wonCount` · `recommendations[5]`
- [ ] 导出 DOCX/PDF 封面 Logo + 页脚（周报/月报/诊断 M2 同步）

## API 封装建议（`report.ts`）

```typescript
createMonthlyReport(projectId, { year, month })
getReportTemplate()
saveReportTemplate(configJson)
```

## 质量 / 证据

**必须提供**：
- 月报 dialog 默认上月 + 生成成功列表新行截图
- MONTHLY 预览 drawer MoM + CRM 五态截图
- 模板设置页保存后封面 mock 更新截图
- 从报告列表跳转模板页截图

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
