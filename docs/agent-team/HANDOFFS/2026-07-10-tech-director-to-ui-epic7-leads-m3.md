# HANDOFF | 技术总监 → UI 设计

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | UI 设计 | 2026-07-10 | EPIC-7 M3 · FR-602/603 · ADR-20260710-25 |

## 交付请求

- [x] **`docs/design/wireframes/leads-list.md` §M3 增量**：线索详情「归因」区 WhatsApp 点击次数/最近时间 · CRM 区「AI 跟进建议」按钮+弹窗（中英 Tab · 复制/填入跟进）
- [x] 落地页 WhatsApp CTA 点击 beacon 说明（footnote 链 Java public API）
- [x] `2026-07-10-ui-to-developer-leads-m3.md`

## Prompt

```
角色：UI 设计。必读 leads-list M2、FR-602/603、ADR-25。
任务：leads M3 线框增量 + HANDOFF 开发。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-10
- **结果摘要**：`leads-list.md` §M3 WhatsApp 归因（次数/最近时间）· CRM Tab「AI 跟进建议」560px 弹窗（中英 Tab · 复制/填入跟进 · `needsHumanReview`）· beacon footnote 链 `POST /api/v1/public/lead-events`；M2 footer AI 按钮启用；`2026-07-10-ui-to-developer-leads-m3.md`
- **遗留**：FR-606 渠道报表 · FR-607 短链/像素 · 自动发 WhatsApp · followup 自动入库
