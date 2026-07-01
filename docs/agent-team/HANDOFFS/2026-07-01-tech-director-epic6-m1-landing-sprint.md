# EPIC-6 M1 落地页 Agent Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-01 |
| **优先级** | High |
| **关联** | EPIC-6 · **FR-501~505** · ADR-20260701-13 |
| **前置** | EPIC-3 关键词 ✅ · EPIC-4 内容 ✅ · EPIC-10 RAG ✅ · EPIC-1 项目 ✅ |

## 目标（M1 MVP）

**从关键词创建落地页草稿 → AI 生成 `content_json` + SEO → Admin 列表/预览 → 状态 DRAFT**

| 范围 | M1 做 | M1 不做 |
|------|-------|---------|
| FR-501 | 模板类型选择（destination / route / theme / visa / event） | GrapesJS 可视化编辑 |
| FR-502 | Python `/ai/landing/generate` 模块文案 + FAQ + CTA | 多语言 |
| FR-503 | `content_json` 模块结构（Hero/亮点/路线/FAQ/表单区） | 拖拽排序 UI |
| FR-504 | `seo_meta_json` Title/Description/H1/FAQ Schema 建议 | 自动提交搜索引擎 |
| FR-505 | `form_config_json` + `whatsapp_link` 默认值 | 公开表单提交（→ EPIC-7） |
| FR-506~508 | — | HTML 导出、Astro 发布、A/B、托管 |
| Java | `landing_page` CRUD + generate 同步 | MQ `ai.landing` |
| Admin | 列表 + 创建 + JSON 预览 drawer | 在线预览 slug URL |

## 任务拆分（推荐顺序）

| # | 角色 | HANDOFF | 依赖 | 验收一句话 |
|---|------|---------|------|------------|
| **1** | **UI 设计** | [→ 落地页列表线框](2026-07-01-tech-director-to-ui-epic6-landing-list.md) | — | `landing-page-list.md` |
| **2** | **开发 Python** | [→ /ai/landing](2026-07-01-tech-director-to-dev-ai-epic6-landing.md) | EPIC-10 RAG | POST generate 返回 content_json + seo |
| **3** | **开发 Java** | [→ landing CRUD](2026-07-01-tech-director-to-dev-java-epic6-landing.md) | DDL + #2 | CRUD + generate 落库 |
| **4** | **开发 Admin** | [→ 列表+预览](2026-07-01-tech-director-to-dev-admin-epic6-landing.md) | #1+#3 | 从关键词创建 + JSON 预览 |
| — | **可选并行** | [EPIC-3 M2 FR-203](2026-06-29-tech-director-epic3-m2-keyword-score-sprint.md) | 不阻塞 #1–4 | 机会词评分 |

**并行**：#1 与 #2 可并行；EPIC-3 M2 由独立窗口并行。

## DDL / API 基线

- 表：`landing_page`（`keyword_id`, `template_type`, `title`, `slug`, `content_json`, `seo_meta_json`, `form_config_json`, `whatsapp_link`, `status`）
- Java：`GET/POST/DELETE /api/v1/projects/{projectId}/landing-pages` · `POST .../landing-pages/{pageId}/generate`
- Python：`POST /ai/landing/generate`（内网 Token）
- `content_json` 模块结构见 PRD §20.3

## 窗口激活 Prompt 摘要

| 角色 | 首行 Prompt |
|------|-------------|
| UI | `角色：UI 设计。必读 FR-501~505 与 landing_page DDL。任务：landing-page-list.md` |
| Python | `角色：开发 inbound-ai。必读 EPIC-6 landing HANDOFF。任务：/ai/landing/generate` |
| Java | `角色：开发 inbound-core。必读 landing_page DDL。任务：landing CRUD+generate` |
| Admin | `角色：开发 Admin。必读 landing 线框。任务：落地页列表+JSON 预览` |

## 完成后

- 各 HANDOFF Done + `MEMORY.md` EPIC-6 M1 ✅
- **技术总监签核（2026-07-01）**：
  - **功能验收 ✅**：UI 线框 · pytest 6 · `test_landing_api` · Admin build · ADR-13 合规（无公开表单）
  - **仓库签核 ⏳**：**C9+C10 未 commit**（~40 文件工作区）
- **下一主 Sprint**：[EPIC-7 M1 线索](2026-07-01-tech-director-epic7-m1-leads-sprint.md)
- M2 排期：Astro SSG · FR-506 导出
