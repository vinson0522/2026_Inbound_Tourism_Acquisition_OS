# EPIC-7 M3 线索 WhatsApp + AI 跟进 Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-10 |
| **优先级** | High |
| **关联** | EPIC-7 · **FR-602/603** · ADR-20260710-25 |
| **前置** | EPIC-7 M2 ✅ C17 · EPIC-5 M1 C21 `fb28a96` |

## 商业决策（完整版路线图 #6）

M2 CRM 闭环已跑通；客户需 **WhatsApp 点击可归因** + **销售 AI 跟进话术**，提升落地页→私域转化可见性与跟进效率。

## 目标（M3 MVP）

| 范围 | M3 做 | M3 不做 |
|------|-------|---------|
| FR-602 | 公网 `POST /api/v1/public/lead-events`（`whatsapp_click`）· DDL `lead_channel_event` · Astro WhatsApp CTA 点击 beacon · Admin 线索详情展示点击次数/最近点击 | FR-606 归因报表 · 广告接入 |
| FR-603 | Python `POST /ai/followup/generate` · Java `POST .../leads/{id}/ai-suggestion` · Admin CRM drawer「AI 跟进建议」弹窗 · 一键填入跟进表单 | 自动发送 WhatsApp · FR-604 老客提醒 |
| 落地页 | `WhatsAppBar.astro` 点击前 fetch beacon（失败不阻塞跳转） | 短链重定向服务 |

## 任务拆分

| # | 角色 | HANDOFF | 依赖 | 验收 |
|---|------|---------|------|------|
| **1** | **UI 设计** | [→ 线索 M3 线框](2026-07-10-tech-director-to-ui-epic7-leads-m3.md) | — | `leads-list.md` §M3 |
| **2** | **开发 Java** | [→ lead-events API](2026-07-10-tech-director-to-dev-java-epic7-leads-m3.md) | DDL 增量 | smoke |
| **3** | **开发 Python** | [→ followup AI](2026-07-10-tech-director-to-dev-ai-epic7-followup.md) | — | pytest |
| **4** | **开发 Landing** | [→ WhatsApp beacon](2026-07-10-tech-director-to-dev-landing-epic7-whatsapp.md) | #2 | curl/beacon |
| **5** | **开发 Admin** | [→ CRM AI UI](2026-07-10-tech-director-to-dev-admin-epic7-leads-m3.md) | #1+#2+#3 | build |

## 窗口派发（复制到各 Cursor 窗口）

| # | 窗口 | HANDOFF | 激活 Prompt（Custom Instructions 首行） |
|---|------|---------|----------------------------------------|
| **1** | **UI 设计** | [→ 线框](2026-07-10-tech-director-to-ui-epic7-leads-m3.md) | `角色：UI 设计。必读 leads-list.md M2、FR-602/603、ADR-25、HANDOFF 2026-07-10-tech-director-to-ui-epic7-leads-m3.md。任务：leads-list §M3 WhatsApp 点击归因 + AI 跟进建议弹窗 + UI→开发 HANDOFF。` |
| **2** | **开发 Java** | [→ Java API](2026-07-10-tech-director-to-dev-java-epic7-leads-m3.md) | `角色：开发 Java。必读 PublicLeadController、LeadServiceImpl、001_schema lead、ADR-25、HANDOFF 2026-07-10-tech-director-to-dev-java-epic7-leads-m3.md。任务：lead_channel_event DDL + public lead-events + ai-suggestion proxy + test_leads_whatsapp_ai.py。` |
| **3** | **开发 Python** | [→ followup AI](2026-07-10-tech-director-to-dev-ai-epic7-followup.md) | `角色：开发 Python。必读 content generate mock、template_service、ADR-25、HANDOFF 2026-07-10-tech-director-to-dev-ai-epic7-followup.md。任务：POST /ai/followup/generate 中英文话术 + FOLLOWUP_MOCK_LLM + pytest。` |
| **4** | **开发 Landing** | [→ WhatsApp beacon](2026-07-10-tech-director-to-dev-landing-epic7-whatsapp.md) | `角色：开发 Landing。必读 WhatsAppBar.astro、PublicLeadController 模式、HANDOFF 2026-07-10-tech-director-to-dev-landing-epic7-whatsapp.md。任务：WhatsApp 点击 beacon 调 public lead-events · 不阻塞 wa.me 跳转 · pnpm build。` |
| **5** | **开发 Admin** | [→ Admin UI](2026-07-10-tech-director-to-dev-admin-epic7-leads-m3.md) | `角色：开发 Admin。必读 leads/index.vue CRM drawer、HANDOFF 2026-07-10-tech-director-to-dev-admin-epic7-leads-m3.md。任务：启用 AI 跟进建议 + WhatsApp 点击展示 · build:prod。` |

**并行**：#1 #2 #3 可并行；#4 依赖 Java public API；#5 依赖 #1+#2+#3。

## 完成后

- smoke：`deploy/scripts/test_leads_whatsapp_ai.py`
- commit **C22**：`feat(core,ai,admin,landing): EPIC-7 M3 WhatsApp tracking and AI followup`
- **技术总监签核（2026-07-10）**：✅ smoke leadId=43 clicks=4 · pytest 7 · build ✅ · **C22 待入库**
- 下一 Sprint：**EPIC-2 M3** 定时诊断（路线图 #7）
