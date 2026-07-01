# EPIC-4 M1 内容 Agent Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-4 · **FR-301** · **FR-302** · ADR-20260629-12 |
| **前置** | EPIC-3 M1 关键词 ✅ · EPIC-10 RAG ✅ · EPIC-1 项目 ✅ |

## 目标（M1 MVP）

**从关键词创建内容任务 → AI 生成短视频脚本 → 落库 `content_task` + `generated_content` → Admin 列表/预览**

| 范围 | M1 做 | M1 不做 |
|------|-------|---------|
| FR-301 | 按 keyword + platform + duration 创建任务 | 一次批量 5–20 选题 |
| FR-302 | 15/30/60s 脚本：hook / script / voiceover / on_screen_text / cta | 多语言 FR-307 |
| FR-303 | `storyboard_json` 最小结构（3–5 镜） | 可导出表格、拆帧 |
| FR-304~308 | — | 封面/排期/语气/图文 |
| Python | `POST /ai/content/generate`；Prompt 读 `template`；RAG 可选 | LangGraph 复杂 DAG（M1 单步 service） |
| Java | `content_task` CRUD + generate 同步调 AI | MQ `ai.content`（M1.1） |
| Admin | 任务列表 + 从关键词创建 + 脚本预览 drawer | TipTap 编辑、版本对比 |

## 任务拆分（推荐顺序）

| # | 角色 | HANDOFF | 依赖 | 验收一句话 |
|---|------|---------|------|------------|
| **1** | **UI 设计** | [→ 内容任务列表线框](2026-06-29-tech-director-to-ui-epic4-content-list.md) | — | `content-task-list.md` |
| **2** | **开发 Python** | [→ /ai/content](2026-06-29-tech-director-to-dev-ai-epic4-content.md) | EPIC-10 LLM/RAG | POST generate 返回脚本 JSON |
| **3** | **开发 Java** | [→ content CRUD](2026-06-29-tech-director-to-dev-java-epic4-content.md) | DDL + #2 契约 | CRUD + generate 落库 |
| **4** | **开发 Admin** | [→ 列表+预览](2026-06-29-tech-director-to-dev-admin-epic4-content.md) | #1+#3 | 从关键词创建 + 预览脚本 |

**并行**：#1 与 #2 可并行；#3 依赖 #2；#4 依赖 #1+#3。

## DDL / API 基线

- 表：`content_task`（`keyword_id`, `platform`, `format`, `duration`, `tone`, `language`, `target_market`, `status`）
- 表：`generated_content`（`script`, `hook`, `storyboard_json`, `needs_human_review` 默认 true）
- Java：`GET/POST/DELETE /api/v1/projects/{projectId}/content-tasks` · `POST .../content-tasks/{taskId}/generate`
- Python：`POST /ai/content/generate`（内网 Token）

## 窗口激活 Prompt 摘要

| 角色 | 首行 Prompt |
|------|-------------|
| UI | `角色：UI 设计。必读 FR-301/302 与 content_task DDL。任务：content-task-list.md` |
| Python | `角色：开发 inbound-ai。必读 EPIC-4 content HANDOFF。任务：/ai/content/generate` |
| Java | `角色：开发 inbound-core。必读 content_task DDL。任务：content CRUD+generate` |
| Admin | `角色：开发 Admin。必读 content 线框。任务：内容任务列表+脚本预览` |

## 完成后

- 各 HANDOFF Done + `MEMORY.md` EPIC-4 M1 ✅
- M2 排期：FR-303 分镜导出、FR-306 语气、MQ 异步
