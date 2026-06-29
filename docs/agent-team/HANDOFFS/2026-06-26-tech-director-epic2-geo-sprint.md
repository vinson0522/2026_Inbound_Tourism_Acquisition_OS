# EPIC-2 GEO 诊断 Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-06-26 |
| **优先级** | High |
| **关联** | EPIC-2 · FR-103~105 · ADR-20260626-07 |
| **前置** | EPIC-1 ✅ · EPIC-10 Phase 1 ✅ · ai-api 服务器 ✅ · UI 线框 ✅ |

## 目标（M1 最小闭环）

**Admin 创建 grounded-api 诊断 → 后台执行 → 列表/详情可见真实 GEO 分数与问题明细**

| 范围 | M1 做 | M1 不做 |
|------|-------|---------|
| 探针模式 | **`grounded-api` 唯一** | browser-extension、headless |
| 问题来源 | demo `question_bank` 种子（3 题）或项目已启用题 | FR-101 自动生成 100 题 |
| 平台 | **Gemini 主力**（`gemini/gemini-2.0-flash` + grounding） | Perplexity 推迟（成本）；OpenAI 备选 |
| 采样 | `sample_count=1` | 多次采样统计 |
| 报告 | 详情页展示分数 | FR-106 DOCX/PDF 导出 |
| 调度 | Java 拆 `probe_task` → **RabbitMQ** `diag.grounded-api` → Python worker | XXL-Job 定时 |

## 任务拆分（3 窗口并行，有依赖）

| # | 角色 | HANDOFF | 依赖 | 验收一句话 |
|---|------|---------|------|------------|
| **1** | **开发（Python）** | [→ AI diagnose](2026-06-26-tech-director-to-dev-ai-epic2-diagnose.md) | ai-api | ✅ 32 tests；**+ parse_gemini 小补** |
| **2** | **开发（Java）** | [→ Java diagnostic](2026-06-26-tech-director-to-dev-java-epic2-diagnostic.md) | #1 HTTP 可测 | POST 创建 run → SUCCESS + geo_score |
| **3** | **开发（Admin）** | [UI 列表](2026-06-25-ui-to-developer-admin-pages.md) + [UI 详情](2026-06-26-ui-to-developer-diagnostic-detail.md) | #2 API | mock 关闭，列表→详情真实数据 |

**推荐顺序**：Python #1 → Java #2 → Admin #3（#2 与 #3 部分并行）。

## 资源门禁（项目资源提供人）

| 项 | 状态 | 说明 |
|----|:----:|------|
| OpenAI / Gemini Key | ✅ 服务器已配置 | **M1 主力 Gemini**（ADR-08） |
| PERPLEXITY_API_KEY | ⏸️ 用户暂缓 | 成本原因；M2 或后续再配 |
| `AI_SERVICE_INTERNAL_TOKEN` | ✅ | Java ↔ Python |
| RabbitMQ | ✅ 服务器全绿 | 队列 `diag.grounded-api` |

## 设计 / DDL 基线

- 表：`diagnostic_run`、`diagnostic_result`、`probe_task`、`question_bank`、`scoring_rule`
- 线框：`docs/design/wireframes/diagnostics-list.md`、`diagnostic-detail.md`
- 状态机：见 `AGENTS.md` §6.4

## 完成后

- 开发更新各 HANDOFF **Done** + `MEMORY.md`
- 技术总监验收 M1 → 排 M2（报告导出 FR-106、多平台、FR-101 题库生成）

---

## 子 HANDOFF 索引

| 文件 | To |
|------|-----|
| [2026-06-26-tech-director-to-dev-ai-epic2-diagnose.md](2026-06-26-tech-director-to-dev-ai-epic2-diagnose.md) | 开发（inbound-ai） |
| [2026-06-26-tech-director-to-dev-java-epic2-diagnostic.md](2026-06-26-tech-director-to-dev-java-epic2-diagnostic.md) | 开发（inbound-core + inbound-admin API 对接） |
