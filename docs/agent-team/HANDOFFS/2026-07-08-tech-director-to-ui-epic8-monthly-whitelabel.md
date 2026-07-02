# HANDOFF | 技术总监 → UI 设计

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | UI 设计 | 2026-07-08 | EPIC-8 M2 · FR-703/704 · ADR-20260708-21 |

## 上下文

M1 `reports-list.md` 已有周报 dialog；月报/模板配置为 disabled。M2 启用月报生成与租户白标模板配置页。

**相关文件**：
- `docs/design/wireframes/reports-list.md`
- `docs/design/wireframes/billing-settings.md` — 设置页版式参考
- `PRD` FR-703/704

## 交付请求

**验收标准**：
- [x] `reports-list.md` 追加 **§M2**：「生成月报」dialog（自然月选择 · MoM 说明 · 聚合章节预览）
- [x] 新建 **`report-template-settings.md`**：Logo URL · 封面标题 · 公司名称 · 主色 · 页脚 · 章节开关 checklist
- [x] 路由建议：`/settings/report-template`（系统设置分组）
- [x] 导出预览示意：DOCX/PDF 封面区展示 Logo + 公司名
- [x] 新建 `2026-07-08-ui-to-developer-report-monthly-whitelabel.md`

## Prompt

```
角色：UI 设计。必读 reports-list.md M1、FR-703/704。
任务：M2 月报线框 + report-template-settings.md + HANDOFF 开发。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-08
- **结果摘要**：`reports-list.md` §M2 月报 dialog + MONTHLY 预览 drawer · 新建 `report-template-settings.md` 白标表单 + 封面 mock · UI→开发 HANDOFF
- **遗留**：高级自定义 daterange（≤62 天）标 P2 UI；Logo 上传 MinIO → 后续 Sprint
