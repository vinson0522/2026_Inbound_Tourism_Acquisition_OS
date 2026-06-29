# EPIC-3 M1 关键词机会词 Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-3 · **FR-201** · **FR-202** · ADR-20260629-11 |
| **前置** | EPIC-1 项目 ✅ · EPIC-10 RAG ✅（生成时可选 RAG）· EPIC-2 诊断 ✅（可选引用 geo 上下文） |

## 目标（M1 MVP）

**按项目 + 八阶段生成机会词 → 落库 `keyword_opportunity` → Admin 列表/筛选**

| 范围 | M1 做 | M1 不做 |
|------|-------|---------|
| FR-201 | Python `/ai/keywords/generate`；Java 触发 + 异步 MQ 或同步 MVP | 批量导入 Excel |
| FR-202 | 八阶段枚举 + 每阶段 ≥5 词（M1）；全 PRD ≥10 词/stage 可 M1.1 | 八阶段词库运营后台 |
| FR-203 | 占位 `score`/`priority` 字段；**不算**完整机会分 | 完整评分公式、竞品词频 |
| Admin | 列表、阶段 Tab、生成按钮、状态 | 编辑/合并/导出 |
| UI | 列表线框 | 词云、详情页 |

## 任务拆分（推荐顺序）

| # | 角色 | HANDOFF | 依赖 | 验收一句话 |
|---|------|---------|------|------------|
| **1** | **UI 设计** | [→ 关键词列表线框](2026-06-29-tech-director-to-ui-epic3-keywords-list.md) | — | `keywords-list.md` |
| **2** | **开发 Python** | [→ /ai/keywords](2026-06-29-tech-director-to-dev-ai-epic3-keywords.md) | EPIC-10 LLM | POST generate 返回结构化 stage+words |
| **3** | **开发 Java** | [→ keyword CRUD](2026-06-29-tech-director-to-dev-java-epic3-keywords.md) | DDL 已有表 | CRUD + 调 AI + 落库 |
| **4** | **开发 Admin** | [→ 列表页](2026-06-29-tech-director-to-dev-admin-epic3-keywords.md) | #1+#3 | 生成 + 列表 + 阶段筛选 |

**并行**：#1 与 #2 可并行；#3 依赖 #2 契约；#4 依赖 #1+#3。

## DDL / API 基线

- 表：`keyword_opportunity`（`keyword`, `keyword_en`, `keyword_cn`, `intent`, `market`, `stage`, `score`, `score_detail_json`, `source_json`, `status`）
- 八阶段 CHECK：`INSPIRATION` … `REPURCHASE`（与 PRD 一致，DDL 已定义）
- Java：`/api/v1/projects/{projectId}/keywords` CRUD + `POST .../keywords/generate`
- Python：`POST /ai/keywords/generate`（内网 Token）

## 窗口激活 Prompt 摘要

| 角色 | 首行 Prompt |
|------|-------------|
| UI | `角色：UI 设计。必读 FR-201/202 与 PRD 八阶段。任务：keywords-list.md` |
| Python | `角色：开发 inbound-ai。必读 HANDOFF epic3-keywords-ai。任务：/ai/keywords/generate` |
| Java | `角色：开发 inbound-core。必读 keyword_opportunity DDL。任务：keyword 模块 CRUD+generate` |
| Admin | `角色：开发 Admin。必读 keywords 线框。任务：关键词列表+生成` |

## 完成后

- 各 HANDOFF Done + `MEMORY.md` EPIC-3 M1 ✅
- M2 排期：FR-203 机会评分、编辑、与 GEO 联动
