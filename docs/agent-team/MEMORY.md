# Agent 团队共享记忆（Single Source of Truth）

> **所有 Cursor Agent 窗口在开始任务前必读，结束任务后必更新。**  
> 各窗口会话互不共享上下文；本文件 + `DECISIONS.md` + `HANDOFFS/` 即「共享记忆」。

| 字段 | 值 |
|------|-----|
| **最后更新** | 2026-06-29 |
| **更新角色** | UI 设计 |
| **Git 远程** | ⚠️ 本地无 `origin` remote；`git push` 待配置仓库 URL |
| **当前 EPIC 焦点** | **EPIC-3 M1** ✅ 功能签核 · ⏳ Admin commit 后仓库关闭 · **EPIC-4 M1** 内容 Agent 已排期 |

---

## 全局状态

| 项 | 状态 |
|----|------|
| 底座 | RuoYi-Vue-Plus 5.6.2 → `inbound-core`；plus-ui → `inbound-admin` |
| 数据库规划 | PostgreSQL 16 + pgvector（`database/ddl/001_schema.sql`） |
| 若依默认库 | PG 配置 ✅；**本机 Docker `sys_user` ✅**（64 表） |
| 基础设施 | 服务器 **全绿** ✅；**本机 Docker Desktop + 4 容器 healthy** ✅（ADR-09） |
| EPIC-1 最小闭环 | ✅ Story 2 完成（项目 CRUD + Admin `/projects`） |
| AI 服务 | `inbound-ai` Phase 1 ✅ · **Phase 2 embed MVP** ✅（ai.embed + `/ai/rag/search`） |
| 落地页/探针/门户 | 目录骨架就绪，待开发 |

## 技术总监

### EPIC-1 Sprint-1 任务拆分（3 条，2026-06-25 定案）

| # | 角色 | 任务 | HANDOFF | 状态 |
|---|------|------|---------|:----:|
| 1 | **运维** | 验证 compose 全绿（PG 28 表 + demo + 6 服务 healthy） | [→运维 compose](HANDOFFS/2026-06-25-tech-director-to-devops-compose.md) | ✅ 服务器 |
| 2 | **开发** | 若依切 PG + 系统表 + **spring-boot 启动验通** | [运维→开发 PG](HANDOFFS/2026-06-25-ops-to-dev-pg-ruoyi.md) | ✅ |
| 3 | **UI** | 品牌 token + 工作台 + GEO 诊断列表线框 | [UI→开发 Admin](HANDOFFS/2026-06-25-ui-to-developer-admin-pages.md) | ✅ |

**Sprint-1 附加（并行，不阻塞关 Sprint）**：

| 任务 | 角色 | 状态 |
|------|------|:----:|
| Admin 页实现（token/工作台/GEO 列表） | 开发 | ✅ |
| 项目列表 + FR-001 线框 | UI | ✅ [projects-list.md](docs/design/wireframes/projects-list.md) |

### Story 2（EPIC-1 最小闭环）

| 任务 | 角色 | HANDOFF | 状态 |
|------|------|---------|:----:|
| FR-001 项目 API + Admin 列表/创建 | 开发 | [Story 2](HANDOFFS/2026-06-25-tech-director-to-dev-story2-fr001.md) | ✅ 2026-06-25 23:15 |

**EPIC-1 验收结论（技术总监 2026-06-25 复核）**：

| 范围 | 状态 | 说明 |
|------|:----:|------|
| 登录 → 建项目 → 列表 | ✅ | smoke test + Admin 联调 |
| 租户隔离 FR-807 | ⚠️ 部分 | `BusinessTenantHelper` 固定 `tenant_id=1` |
| 知识库上传 FR-004/005 | ✅ MVP | embed 管道 + Admin 知识库 Tab 上传/状态 |
| 竞品/路线 FR-002/003 | ✅ | Story 3 详情页 + Java CRUD API |
| 工作台真实数据 FR-006 | ✅ MVP | `getDashboard` 前端聚合 `listDiagnosticRuns`（KPI + 最近 5 条） |
| API 统一响应 `{code,trace_id}` | ⏳ | 仍用 RuoYi `R`/`TableDataInfo` |

### EPIC-10 Sprint（Phase 1 — **审核通过** 2026-06-26）

| # | 角色 | 任务 | HANDOFF | 状态 |
|---|------|------|---------|:----:|
| 1 | **开发** | FastAPI + LiteLLM gateway + `/health` + Dockerfile | [→dev scaffold](HANDOFFS/2026-06-25-tech-director-to-dev-epic10-scaffold.md) | ✅ |
| 2 | **运维** | compose `ai-api` 集成 + 凭证文档 | [→ops compose](HANDOFFS/2026-06-25-tech-director-to-devops-epic10-ai-compose.md) | ✅ 服务器 |
| 3 | **开发** | Phase 2：embed/RAG/worker | [→embed](HANDOFFS/2026-06-29-tech-director-to-dev-epic10-phase2-embed.md) | ✅ 2026-06-29 |

### EPIC-2 Sprint M1（代码 Done — 本地验收集）

| # | 角色 | 任务 | HANDOFF | 状态 |
|---|------|------|---------|:----:|
| 1 | **开发 Python** | `/ai/diagnose` + parse + score + MQ worker | [→AI](HANDOFFS/2026-06-26-tech-director-to-dev-ai-epic2-diagnose.md) | ✅ |
| 2 | **开发 Java** | diagnostic CRUD + MQ + callback | [→Java](HANDOFFS/2026-06-26-tech-director-to-dev-java-epic2-diagnostic.md) | ✅ |
| 3 | **开发 Admin** | 详情页 | [UI 详情](HANDOFFS/2026-06-26-ui-to-developer-diagnostic-detail.md) | ✅ |
| 4 | **运维** | **本机 D 盘 Docker 收尾**（import 若依 + smoke） | [→local docker](HANDOFFS/2026-06-27-tech-director-to-devops-local-docker-finish.md) | ✅ 2026-06-27 |
| — | **总览** | Sprint 索引 | [EPIC-2 Sprint](HANDOFFS/2026-06-26-tech-director-epic2-geo-sprint.md) | — |

**M1 范围**：grounded-api · Gemini（ADR-08）· demo 3 题 · 无报告导出  
**M2 FR-106（2026-06-29）** ✅ DOCX + **PDF**（HTML→Gotenberg chromium）；`application-dev.yml` 默认 `GOTENBERG_BASE_URL=http://localhost:3002`；未起 Gotenberg 时 400 友好提示  
**E2E**：隧道 runId=10 ✅；**本地 runId=2 ✅** mock LLM geo_score=85

### EPIC-2 M2.2 Sprint — FR-108 诊断趋势（2026-06-29 排期）

| # | 角色 | 任务 | HANDOFF | 状态 |
|---|------|------|---------|:----:|
| 1 | UI | 趋势线框 | [→UI](HANDOFFS/2026-06-29-tech-director-to-ui-diagnostic-trends.md) | ✅ 线框 + [UI→开发](HANDOFFS/2026-06-29-ui-to-developer-diagnostic-trends.md) |
| 2 | 开发 Java | trends API | [→Java FR-108](HANDOFFS/2026-06-29-tech-director-to-dev-java-fr108-trends.md) | ✅ 2026-06-29 |
| 3 | 开发 Admin | ECharts 趋势页 | [→Admin FR-108](HANDOFFS/2026-06-29-tech-director-to-dev-admin-fr108-trends.md) | ✅ 2026-06-29 |
| — | 总览 | Sprint 索引 | [M2.2 Sprint](HANDOFFS/2026-06-29-tech-director-epic2-m22-fr108-sprint.md) | — |

**技术总监签核（2026-06-29）**：✅ **M2.2 关闭** — smoke `test_diagnostic_trends` 2 点 ASC + 六分项；ADR-10 合规；P2 遗留（时间筛选/dashboard 链）不阻塞 EPIC-3。⚠️ 代码**未 commit**（见 B-04）。

**ADR-10**：复用 `diagnostic_run`，不建新表

### EPIC-3 M1 Sprint — FR-201/202 关键词 MVP（2026-06-29 排期）

| # | 角色 | 任务 | HANDOFF | 状态 |
|---|------|------|---------|:----:|
| 1 | UI | 关键词列表线框 | [→UI](HANDOFFS/2026-06-29-tech-director-to-ui-epic3-keywords-list.md) | ✅ |
| 2 | 开发 Python | `/ai/keywords/generate` | [→AI](HANDOFFS/2026-06-29-tech-director-to-dev-ai-epic3-keywords.md) | ✅ |
| 3 | 开发 Java | keyword CRUD + generate | [→Java](HANDOFFS/2026-06-29-tech-director-to-dev-java-epic3-keywords.md) | ✅ 2026-06-29 |
| 4 | 开发 Admin | 列表 + 生成 | [→Admin](HANDOFFS/2026-06-29-tech-director-to-dev-admin-epic3-keywords.md) | ✅ 实现 · ⏳ **未 commit** |
| — | 总览 | Sprint 索引 | [EPIC-3 M1](HANDOFFS/2026-06-29-tech-director-epic3-m1-keywords-sprint.md) | 功能 ✅ · 仓库 ⏳ |

**技术总监签核（2026-06-29）**：功能四棒 ✅ · smoke 8/8 · ADR-11 ✅；**仓库关闭待 Admin commit（C6）**

**ADR-11**：M1 仅生成+列表；FR-203 评分 → [M2 可选](HANDOFFS/2026-06-29-tech-director-epic3-m2-keyword-score-sprint.md)

### EPIC-4 M1 Sprint — FR-301/302 内容 Agent（2026-06-29 排期）

| # | 角色 | 任务 | HANDOFF | 状态 |
|---|------|------|---------|:----:|
| 1 | UI | 内容任务列表线框 | [→UI](HANDOFFS/2026-06-29-tech-director-to-ui-epic4-content-list.md) | ⏳ |
| 2 | 开发 Python | `/ai/content/generate` | [→AI](HANDOFFS/2026-06-29-tech-director-to-dev-ai-epic4-content.md) | ⏳ |
| 3 | 开发 Java | content CRUD + generate | [→Java](HANDOFFS/2026-06-29-tech-director-to-dev-java-epic4-content.md) | ⏳ |
| 4 | 开发 Admin | 列表 + 脚本预览 | [→Admin](HANDOFFS/2026-06-29-tech-director-to-dev-admin-epic4-content.md) | ⏳ |
| — | 总览 | Sprint 索引 | [EPIC-4 M1](HANDOFFS/2026-06-29-tech-director-epic4-m1-content-sprint.md) | — |

**ADR-12**：M1 仅选题+脚本；分镜导出/排期/多语言 → M2

### 未提交增量 — commit 批次（技术总监 2026-06-29 定案）

| 批次 | 范围 | 建议 message 前缀 | smoke |
|:----:|------|-------------------|-------|
| **C1** | FR-108 trends（Java Vo/Aggregator/test + Admin trends.vue + diagnostic.ts + wireframe/HANDOFF + `test_diagnostic_trends.py`） | `feat(core,admin): FR-108 diagnostic trends` | `test_diagnostic_trends` |
| **C2** | FR-005 RAG 预览（KnowledgeAsset search + Admin drawer + ai-client RAG models + `test_knowledge_rag_search.py`） | `feat(core,admin): FR-005 knowledge RAG search preview` | `test_knowledge_rag_search` |
| **C3** | Phase 2.2 Docling（document_parser/embed/file_storage + tests + pyproject） | `feat(ai): EPIC-10 Phase 2.2 Docling embed pipeline` | `pytest inbound-ai/tests/test_embed_docling_pipeline.py` |
| **C4** | FR-106 PDF/Gotenberg + dashboard 跳转 + report export smoke | `feat(core,admin,deploy): FR-106 PDF export and dashboard fixes` | `test_diagnostic_report_export` |
| **C5** | Sprint HANDOFFs + MEMORY + DECISIONS ADR-10/11 + design README | `docs: M2.2/EPIC-3 Sprint HANDOFFs and MEMORY` | — |
| **C6** | EPIC-3 Admin 关键词页（`keyword.ts` / `keywords/index.vue` / router / detail 入口 + HANDOFF Done） | `feat(admin): EPIC-3 M1 keywords list page` | 浏览器 `/keywords` |

**执行**：C1–C5 ✅ 已完成；**C6 ⏳** 为 EPIC-3 M1 仓库关闭前置。不含 `deploy/.env`。

### 本机 Docker 决策（ADR-09，2026-06-27）

| 项 | 值 |
|----|-----|
| 磁盘镜像 | `D:\Dev\SDKs\Docker\wsl-data` |
| 业务数据 | `D:\Dev\SDKs\Docker\inbound-growth\` |
| 手册 | `deploy/LOCAL_DOCKER.md` |
| 启动 | `deploy/scripts/local_docker_bootstrap.ps1` |
| 清理 | `deploy/scripts/local_docker_cleanup.ps1`（Level A–E） |

---

## 运维

- **负责目录**：`deploy/`、`cert/`、`docs/INFRA_ACCESS.local.md`
- **验证时间**：2026-06-27（本机 Docker bootstrap ✅）

### 本机（资源提供人 Windows — **主开发路径 ADR-09**）

| 项 | 结果 |
|----|------|
| WSL2 + Ubuntu | ✅ |
| Docker Desktop | ✅；Disk image → `D:\Dev\SDKs\Docker\wsl-data` |
| Compose 4 服务 | ✅ postgres / redis / rabbitmq / ai-api **healthy** |
| 若依系统表 | ✅ `sys_user` 存在；public **64 表**（`import_ruoyi_pg_local.ps1`） |
| `deploy/.env` | ✅ `INBOUND_DOCKER_DATA` + worker/callback + GEMINI（gitignore） |
| Smoke | ✅ `/health` 200 · `test_ai_health.py` · pg_isready |
| SSH 隧道 | **已关闭**（5432/6379/5672/8090 → Docker，非 ssh） |
| 应用 smoke | ✅ `test_projects_api.py` + **`test_diagnostic_e2e.py`**（开发 2026-06-29，本地 mock LLM） |

**手册**：`deploy/LOCAL_DOCKER.md` · 启动 `scripts/local_docker_bootstrap.ps1`

### 共享服务器 `18.139.209.10`（staging ✅）

| 服务 | 绑定 | 健康 |
|------|------|:----:|
| PostgreSQL | `127.0.0.1:5432` | ✅ |
| Redis | `127.0.0.1:6380` | ✅ |
| RabbitMQ | `127.0.0.1:5672` | ✅ |
| **ai-api** | `127.0.0.1:8090` | ✅（worker 未启；本机开发不依赖） |

---

## 开发

- **负责目录**：`inbound-core/`、`inbound-ai/`、`database/`、`inbound-probe-extension/`
- **6/29 增量二（2026-06-29）** ✅ **已提交** `a87b780` Story3 · `91c215b` Phase2.1 · `06c4660` docs wireframes
- **Sprint HANDOFF 归档（2026-06-29）** ✅ **已提交** `3ac6853` — 2026-06-25~27 HANDOFF + INFRA/DECISIONS
- **Push 状态** ⏳ 本地 **8 个 feat** + **5 个 docs** 待 push；需先 `git remote add origin <url>`
- **EPIC-2 M2 FR-106（2026-06-29）** ✅ DOCX + PDF 报告导出 runId=2；`test_diagnostic_report_export.py` docx/pdf；PDF ~32KB 含 geo_score
- **EPIC-2 M2.2 FR-108 trends API（2026-06-29）** ✅ `GET /api/v1/projects/{id}/diagnostics/trends?limit=12` · 租户隔离 · 从 `diagnostic_result` 聚合 6 分项 · `DiagnosticMetricsAggregatorTest` + `test_diagnostic_trends.py`（runId=2/3 · 2 点 ASC）
- **EPIC-2 M2.2 FR-108 Admin 趋势页（2026-06-29）** ✅ `/diagnostics/trends` · ECharts 折线+分项柱图 · run 多选 2–6 · 空态「至少需要 2 次成功诊断」· 详情页链趋势
- **FR-005 知识库 RAG 检索预览（2026-06-29）** ✅ `POST .../knowledge-assets/search` → `/ai/rag/search` · Admin drawer top-3 + chunk_id · `test_knowledge_rag_search.py`（asset#1 · 1 hit）
- **EPIC-10 Phase 2.2 Docling（2026-06-29）** ✅ PDF/DOCX 解析替换 mock 切片 · `file_storage`（HTTP/MinIO/本地）· embed 优先 `file_url` · DLQ 已有 · pytest fixture PDF + `test_embed_docling_pipeline`
- **EPIC-3 M1 FR-201 keywords AI（2026-06-29）** ✅ `POST /ai/keywords/generate` · mock/无 Key 回退 · 可选 RAG top-3 · `template_service` · `test_keywords_generate.py` 7 passed
- **EPIC-3 M1 FR-201/202 keywords Java（2026-06-29）** ✅ 并入 `ruoyi-project` · `GET/POST/DELETE .../keywords` + `POST .../generate` · Feign `/ai/keywords/generate` · `tenant.excludes` 加 `keyword_opportunity` · `test_keywords_api.py` ✅
- **EPIC-3 M1 FR-201/202 Admin 关键词页（2026-06-29）** ✅ `/keywords/index` + `/projects/:id/keywords` · 八阶段 Tab · AI 生成确认+loading · `keyword.ts` API · 项目详情入口
- **EPIC-10 Phase 2 embed MVP（2026-06-29）** ✅ **已提交** `f40cf8d` — `ai.embed` worker · asset#1 READY
- **M2 代码（2026-06-29）** ✅ **已提交** `f40cf8d` / `e22cd43` / `f96ba7e`（待 push）
- **EPIC-2 M1 代码** ✅ **已提交** `54d8ca5` / `6ba5e1e` / `48926d2`（待 push）
- **Admin 浏览器走查（2026-06-29）** ✅ `pnpm dev` :5173 · admin/admin123 · Java :8080 + Docker ADR-09

  | 路径 | 结果 | 说明 |
  |------|:----:|------|
  | `/dashboard` | ✅→🔧 | KPI 4 卡 + 最近诊断 5 条 OK；**已修**「查看」无跳转 → `goRunDetail` |
  | `/projects/index` → `:id` | ✅ | 「进入」→ 品牌/竞品/知识库三 Tab；竞品≥5 提示；知识库 asset#1 READY · **检索预览** top-3 |
  | `/diagnostics/runs` | ✅ | 新建抽屉 → 提交进详情；四 Tab（概览/问题/竞品/探针） |
  | 详情「导出 DOCX」 | ✅ | SUCCESS run 可点；API smoke 2841B（runId=2/3） |
  | 已知 MVP 占位 | ⚠️ | 工作台任务/预警/漏斗按钮无后端；知识库上传需 OSS |

- **6/29 全量 smoke 回归（2026-06-29 19:40）** ✅ ADR-09 本机 · 4 容器 healthy + Java :8080 · **7/7 通过**
  - `test_projects_api` ✅（project id=5 新建）
  - `test_diagnostic_e2e` ✅ runId=**3** · SUCCESS · geo_score=85 · mock LLM
  - `test_embed_e2e` ✅ READY + RAG top-3（`EMBED_MOCK=true`）
  - `test_diagnostic_report_export` ✅ runId=2 DOCX 2841B
  - `test_diagnostic_trends` ✅ runId=2/3 · 2 点 · metrics 六分项
  - `test_ai_health` ✅ litellm=ready
  - `test_knowledge_rag_search` ✅ asset#1 · 1 hit · chunkId=5
  - `test_keywords_api` ✅ inspiration · insertedCount=3 · total 4→7
- **E2E（本机 Docker ADR-09）** ✅ runId=2/3 · `SUCCESS` · `geo_score=85.00` · **DIAGNOSE_MOCK_LLM**（Docker ai-api）
- **工作台 FR-006 MVP（2026-06-26）** ✅ `getDashboard` 聚合诊断列表；`/dashboard` 编译与运行正常
- **待办**：真 embedding 生产 Key 轮换；真 Gemini E2E（Q12 配额恢复后）

### 开发联调步骤（本机 Docker — **首选**，ADR-09）

**前提**：运维完成 [local-docker HANDOFF](HANDOFFS/2026-06-27-tech-director-to-devops-local-docker-finish.md)

1. 确认 Docker 4 容器 healthy；**不开 SSH 隧道**
2. 环境变量（本地 compose 默认值）：
   - `INBOUND_PG_PASSWORD=inbound_dev_pass`
   - `INBOUND_REDIS_PASSWORD=`（空）
   - `INBOUND_RABBITMQ_PASSWORD=inbound_dev_pass`
   - `AI_SERVICE_BASE_URL=http://localhost:8090`
   - `AI_SERVICE_INTERNAL_TOKEN=dev_internal_token_change_me`
3. `cd inbound-core && mvn -pl ruoyi-admin spring-boot:run -Dspring-boot.run.profiles=dev`
4. `cd inbound-admin && pnpm dev`
5. Smoke（6/29 全量）：`test_projects_api` / `test_diagnostic_e2e` / `test_embed_e2e` / `test_diagnostic_report_export` / `test_diagnostic_trends` / `test_ai_health` / `test_knowledge_rag_search` / `test_keywords_api`

### 开发联调步骤（SSH 隧道 — **备用**，ADR-05）

见 `docs/INFRA_ACCESS.local.md` §6；Redis **6380** + 密码；PG 可能 **15432**。

---

## UI 设计

- **已完成**：… keywords-list / **content-task-list（EPIC-4 M1 预研）**
- **HANDOFF**：[内容任务 → 开发](HANDOFFS/2026-06-29-ui-to-developer-content-task-list.md)（预研，待技术总监定稿）
- **待办**：内容任务详情/脚本编辑线框（M2）；落地页线框（EPIC-6）

---

## 阻塞项

| ID | 描述 | 负责人 | 状态 |
|----|------|--------|------|
| B-01 | ~~本机不装 Docker，隧道联调~~ | 用户 | **已关闭** → ADR-09 本机 Docker |
| B-02 | ~~本机若依表未 import~~ | 运维 | **已关闭** 2026-06-27 |
| B-03a | 本地 GEO E2E **mock LLM**（`DIAGNOSE_MOCK_LLM=true`） | 开发 | ✅ 2026-06-29 runId=2/3 · geo_score=85 |
| B-03b | 本地 GEO E2E **真 Gemini**（grounded-api） | 开发 | ⏳ 配额用尽；`DIAGNOSE_MOCK_LLM=false` 时 FAILED |
| B-04 | ~~工作区未 commit（C1–C6）~~ | 开发 | **已关闭** 2026-07-01 · `2ffa3d7` Admin 关键词页 |
| B-05 | EPIC-3 M1 **仓库签核** | 技术总监 | ⏳ 待全栈验收签核 |

---

## 下一步（跨角色）

| 优先级 | 窗口 | 动作 | HANDOFF |
|:------:|------|------|---------|
| **P0** | **开发 Admin** | **C6** commit 关键词页 → EPIC-3 M1 仓库关闭 | — |
| **P1** | **UI 设计** | EPIC-4 内容任务列表线框 | [→UI content](HANDOFFS/2026-06-29-tech-director-to-ui-epic4-content-list.md) |
| **P2** | **开发 Python** | `/ai/content/generate`（可与 UI 并行） | [→AI EPIC-4](HANDOFFS/2026-06-29-tech-director-to-dev-ai-epic4-content.md) |
| **P3** | **开发 Java** | content CRUD + generate | [→Java EPIC-4](HANDOFFS/2026-06-29-tech-director-to-dev-java-epic4-content.md) |
| **P4** | **开发 Admin** | 内容列表 + 脚本预览 | [→Admin EPIC-4](HANDOFFS/2026-06-29-tech-director-to-dev-admin-epic4-content.md) |
| **P5** | **运维** | `git remote` + push（C6 后） | — |
| **P6** | **开发** | EPIC-3 M2 FR-203 评分（可选，不阻塞 EPIC-4） | [EPIC-3 M2](HANDOFFS/2026-06-29-tech-director-epic3-m2-keyword-score-sprint.md) |

---

## 更新日志（最近 5 条）

| 日期 | 角色 | 摘要 |
|------|------|------|
| 2026-06-29 | 技术总监 | EPIC-3 M1 **功能签核 ✅** · 仓库 ⏳ C6；排 EPIC-4 M1 HANDOFF + ADR-12 |
| 2026-07-01 | 开发 | commit `2ffa3d7` EPIC-3 M1 Admin 关键词页 · B-04 全关 |
| 2026-06-29 | 开发 | EPIC-3 M1 Admin 关键词列表页：八阶段 Tab + AI 生成 + 项目详情入口 |
| 2026-06-29 | 开发 | 分批 commit C1–C5 + EPIC-3 keywords（579b668…95ab73c）· 工作区干净 |
| 2026-06-29 | 开发 | EPIC-3 M1 `POST /ai/keywords/generate` · mock + RAG · pytest 7 passed |
| 2026-06-29 | 技术总监 | **EPIC-2 M2.2 签核 ✅**；定 commit 批次 C1–C5；B-04 未 commit；焦点转 EPIC-3 M1 |
| 2026-06-29 | UI 设计 | EPIC-4 M1 content-task-list 预研线框 · FR-205 从关键词创建 |
| 2026-06-29 | UI 设计 | EPIC-3 M1 keywords-list 线框 FR-201/202 · HANDOFF 开发 |
| 2026-06-29 | 技术总监 | 下一 Sprint HANDOFF：EPIC-2 M2.2 FR-108 + EPIC-3 M1 FR-201/202；ADR-10/11 |
| 2026-06-29 | 开发 | EPIC-10 Phase 2.2 Docling：PDF/DOCX embed 管道 · MinIO/本地 file · pytest 11 passed |
| 2026-06-29 | 开发 | FR-005 知识库 RAG 检索预览：Java 代理 + Admin drawer · smoke 1 hit |
| 2026-06-29 | 开发 | FR-108 trends API：`GET .../diagnostics/trends` · runId=2/3 smoke · Java 单测 aggregator |
| 2026-06-29 | 开发 | 6/29 增量二 commit：`a87b780` Story3 · `91c215b` Phase2.1 · docs HANDOFF（未 push） |
| 2026-06-29 | 开发 | EPIC-10 Phase 2.1：OpenAI embedding + bge-reranker top-3；pytest + test_embed_e2e ✅ |
| 2026-06-29 | 开发 | EPIC-2 M2 FR-106：DOCX 报告导出 + Admin 按钮；runId=2 smoke 通过 |
| 2026-06-29 | 开发 | EPIC-10 Phase 2 embed MVP：`ai.embed` + chunk/pgvector + `/ai/rag/search`；E2E asset#1 READY |
| 2026-06-29 | 开发 | M1 代码分批 commit：`54d8ca5` ai · `6ba5e1e` core · `48926d2` admin+deploy（未 push） |
| 2026-06-29 | UI 设计 | FR-108 诊断趋势对比线框 + HANDOFF 开发 |
| 2026-06-29 | UI 设计 | Story 3 项目详情线框（品牌/竞品/知识库 Tab）+ HANDOFF 开发 |
| 2026-06-29 | 开发 | EPIC-2 M1 本地 E2E ✅ runId=2 SUCCESS geo_score=85 mock LLM；4 容器 healthy |
| 2026-06-26 | 开发 | Admin 工作台 `getDashboard` + `DashboardData`；FR-006 MVP 前端聚合 |
| 2026-06-27 | 技术总监 | 复核：本机 Docker+若依+Java+projects smoke ✅；E2E FAILED=Gemini 配额 |
| 2026-06-27 | 运维 | 本机 Docker 收尾：import 若依 64 表 + smoke；fix import_ruoyi_pg_local.ps1 |
| 2026-06-27 | 技术总监 | ADR-09 本机 D 盘 Docker；HANDOFF 运维收尾；bootstrap 四容器 healthy |
| 2026-06-27 | 用户+运维 | Docker Desktop + WSL2 Ubuntu；`local_docker_bootstrap.ps1` 成功 |
| 2026-06-26 | 运维 | 服务器 ai-api EPIC-2 镜像；CORE_CALLBACK 文档 |
| 2026-06-26 | 开发 | EPIC-2 M1 E2E（隧道 mock LLM）runId=10 SUCCESS |
| 2026-06-26 | 开发 | EPIC-2 Java + Admin 详情页 Done |
