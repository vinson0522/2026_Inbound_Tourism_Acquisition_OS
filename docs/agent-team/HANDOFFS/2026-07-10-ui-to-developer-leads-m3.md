# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-07-10 |
| **优先级** | High |
| **关联** | EPIC-7 M3 · FR-602/603 · [技术总监 → UI](2026-07-10-tech-director-to-ui-epic7-leads-m3.md) · ADR-20260710-25 |

## 上下文

**当前状态**：M2 `/leads` CRM drawer（状态/跟进/负责人）✅。M3 补 WhatsApp 点击归因展示 + AI 跟进话术。

**相关文件**：
- `docs/design/wireframes/leads-list.md` — **§M3 增量**（WhatsApp 归因 · AI 弹窗 · beacon footnote）
- M2 实现：`inbound-admin/src/views/tourgeo/leads/index.vue`
- [landing-page-publish.md](../../design/wireframes/landing-page-publish.md) §B.5 — WhatsApp CTA M3 beacon
- Java HANDOFF：`2026-07-10-tech-director-to-dev-java-epic7-leads-m3.md`
- Landing HANDOFF：`2026-07-10-tech-director-to-dev-landing-epic7-whatsapp.md`
- Admin HANDOFF：`2026-07-10-tech-director-to-dev-admin-epic7-leads-m3.md`

**约束**：
- WhatsApp 按 landing_page 聚合 · 不强绑 lead_id（ADR-25）
- AI 填入跟进 **不自动 POST** · 不自动发 WhatsApp
- 列表 PII 脱敏不变 · 无 CSV · 无归因报表
- `needsHumanReview` 必须展示

## 交付请求

**需要什么**：在现有 leads CRM drawer 上启用 M3 能力。

**验收标准**：
- [ ] **线索信息 Tab** — 归因区「WhatsApp 点击」：次数 · 最近时间 · 空态
- [ ] info alert / 页 footnote — beacon 机制 · 链 `POST /api/v1/public/lead-events`
- [ ] **CRM Tab 底栏** — 「AI 跟进建议」启用（`lead:edit`）
- [ ] 弹窗 560px · 中英 `el-tabs` · readonly textarea
- [ ] `POST .../leads/{id}/ai-suggestion` · loading · `needsHumanReview` Tag
- [ ] 「复制」当前 Tab · 「填入跟进内容」→ CRM textarea · 不自动提交
- [ ] 合规 disclaimer 固定展示
- [ ] 只读角色隐藏 AI 按钮
- [ ] `lead.ts` 增补 · `pnpm build:prod` ✅

## 后端依赖

- [ ] `GET .../leads/{id}` — `whatsappClickCount`, `lastWhatsappClickAt`
- [ ] `POST .../leads/{id}/ai-suggestion` — `{ suggestionEn, suggestionZh, needsHumanReview }`
- [ ] `POST /api/v1/public/lead-events` — Landing beacon（非 Admin）
- [ ] smoke `test_leads_whatsapp_ai.py`

## API 封装建议（`lead.ts`）

```typescript
generateLeadAiSuggestion(projectId, leadId)
// GET lead detail 类型增补 whatsappClickCount / lastWhatsappClickAt
```

## 质量 / 证据

**必须提供**：
- 线索信息 Tab WhatsApp 次数/时间截图（含空态）
- AI 弹窗中英 Tab + 填入跟进截图
- footnote/beacon 说明可见截图
- 只读角色无 AI 按钮截图

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
