# HANDOFF | 技术总监 → UI 设计

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | UI 设计 | 2026-07-07 | EPIC-7 M2 · FR-605 · ADR-20260707-20 |

## 上下文

EPIC-7 M1 线索列表/详情只读已完成。完整版交付需销售可 **变更状态、写跟进、看负责人**。DDL 已有 `lead_status` 枚举与 `lead_followup` 表。

**相关文件**：
- `docs/design/wireframes/leads-list.md` — M1 线框（详情 drawer 含 FR-605 disabled 占位）
- `database/ddl/001_schema.sql` — `lead` · `lead_followup`
- `PRD_商业化版_V2.0.md` FR-605

## 交付请求

在 **`leads-list.md` 追加 M2 增量章节**（不新建文件），并写 UI→开发 HANDOFF。

**验收标准**：
- [x] 详情 drawer：状态下拉/步骤（NEW/FOLLOWING/QUOTED/WON/LOST）· Tag 色与 M1 一致扩展
- [x] 「添加跟进」表单：content 必填 · channel 可选（email/phone/whatsapp/meeting）
- [x] 跟进时间线：`el-timeline` · 操作人 · 时间 · 内容
- [x] 负责人：assignee 展示 + 指派（M2 简化为下拉选当前租户成员或「指派给我」）
- [x] 列表：状态列可筛选 · 状态 Tag 五色
- [x] 合规 footnote：PII 仍脱敏列表 · 详情完整展示
- [x] 新建 `2026-07-07-ui-to-developer-leads-crm.md`

## Prompt

```
角色：UI 设计。必读 leads-list.md M1、FR-605。
任务：leads-list.md M2 CRM 增量线框 + HANDOFF 开发。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-07
- **结果摘要**：`leads-list.md` §M2 状态机/跟进时间线/负责人；640px drawer Tab；HANDOFF 开发
- **遗留**：列表 assignee 列 P2；快捷「标记跟进中」P2；首次跟进自动改状态 P2
