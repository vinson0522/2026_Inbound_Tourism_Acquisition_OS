# Agent 团队共享记忆（Single Source of Truth）

> **所有 Cursor Agent 窗口在开始任务前必读，结束任务后必更新。**  
> 各窗口会话互不共享上下文；本文件 + `DECISIONS.md` + `HANDOFFS/` 即「共享记忆」。

| 字段 | 值 |
|------|-----|
| **最后更新** | 2026-07-05 |
| **更新角色** | 开发 |
| **Git 远程** | ✅ `origin/main` · C13 `71c374d` · **C14 `f23e539`** |
| **当前 EPIC 焦点** | **EPIC-11 M1** 浏览器探针（FR-112~114）· Java ∥ Extension → Admin |

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
| 4 | 开发 Admin | 列表 + 生成 | [→Admin](HANDOFFS/2026-06-29-tech-director-to-dev-admin-epic3-keywords.md) | ✅ `75e96cb` |
| — | 总览 | Sprint 索引 | [EPIC-3 M1](HANDOFFS/2026-06-29-tech-director-epic3-m1-keywords-sprint.md) | ✅ **关闭** |

**技术总监签核（2026-07-01）**：✅ **EPIC-3 M1 正式关闭** — C6 `75e96cb` · smoke 8/8 · ADR-11

**ADR-11**：M1 仅生成+列表；FR-203 评分 → [M2 可选](HANDOFFS/2026-06-29-tech-director-epic3-m2-keyword-score-sprint.md)

### EPIC-4 M1 Sprint — FR-301/302 内容 Agent（2026-06-29 排期）

| # | 角色 | 任务 | HANDOFF | 状态 |
|---|------|------|---------|:----:|
| 1 | UI | 内容任务列表线框 | [→UI](HANDOFFS/2026-06-29-tech-director-to-ui-epic4-content-list.md) | ✅ `4d8e1e2` |
| 2 | 开发 Python | `/ai/content/generate` | [→AI](HANDOFFS/2026-06-29-tech-director-to-dev-ai-epic4-content.md) | ✅ `23a46f6` |
| 3 | 开发 Java | content CRUD + generate | [→Java](HANDOFFS/2026-06-29-tech-director-to-dev-java-epic4-content.md) | ✅ `23a46f6` |
| 4 | 开发 Admin | 列表 + 脚本预览 | [→Admin](HANDOFFS/2026-06-29-tech-director-to-dev-admin-epic4-content.md) | ✅ `23a46f6` |
| — | 总览 | Sprint 索引 | [EPIC-4 M1](HANDOFFS/2026-06-29-tech-director-epic4-m1-content-sprint.md) | ✅ **关闭** |

**技术总监签核（2026-07-01）**：✅ **EPIC-4 M1 正式关闭** — C7 `4d8e1e2` docs · C8 `23a46f6` 全栈 · pytest 6 + `test_content_api` ✅ · ADR-12

**ADR-12**：M1 仅选题+脚本；分镜导出/排期/多语言 → M2

### EPIC-6 M1 Sprint — FR-501~505 落地页 Agent（2026-07-01）

| # | 角色 | 任务 | HANDOFF | 状态 |
|---|------|------|---------|:----:|
| 1 | UI | 落地页列表线框 | [→UI](HANDOFFS/2026-07-01-tech-director-to-ui-epic6-landing-list.md) | ✅ |
| 2 | 开发 Python | `/ai/landing/generate` | [→AI](HANDOFFS/2026-07-01-tech-director-to-dev-ai-epic6-landing.md) | ✅ pytest 6 |
| 3 | 开发 Java | landing CRUD + generate | [→Java](HANDOFFS/2026-07-01-tech-director-to-dev-java-epic6-landing.md) | ✅ smoke |
| 4 | 开发 Admin | 列表 + JSON 预览 | [→Admin](HANDOFFS/2026-07-01-tech-director-to-dev-admin-epic6-landing.md) | ✅ build |
| — | 总览 | Sprint 索引 | [EPIC-6 M1](HANDOFFS/2026-07-01-tech-director-epic6-m1-landing-sprint.md) | ✅ **关闭** |

**技术总监签核（2026-07-01）**：✅ **EPIC-6 M1 正式关闭** — C10 `91b3ea4` · pytest 6 + `test_landing_api` · ADR-13

**ADR-13**：M1 草稿+预览；Astro/公开表单 → **M2 本 Sprint**

### EPIC-6 M2 Sprint — Astro 发布 + Turnstile（2026-07-03 排期）

| # | 角色 | 任务 | HANDOFF | 状态 |
|---|------|------|---------|:----:|
| 1 | UI | 发布/预览线框 | [→UI](HANDOFFS/2026-07-03-tech-director-to-ui-epic6-landing-publish.md) | ✅ |
| 2 | 开发 Java | public GET + publish | [→Java](HANDOFFS/2026-07-03-tech-director-to-dev-java-epic6-landing-publish.md) | ✅ smoke |
| 3 | 运维 | landing compose :4321 | [→运维](HANDOFFS/2026-07-03-tech-director-to-devops-epic6-landing-compose.md) | ✅ |
| 4 | 开发 Landing | Astro scaffold | [→Astro](HANDOFFS/2026-07-03-tech-director-to-dev-landing-epic6-astro.md) | ✅ build + 404 |
| 5 | 开发 Admin | 发布/预览按钮 | [→Admin](HANDOFFS/2026-07-03-tech-director-to-dev-admin-epic6-landing-publish.md) | ✅ build |
| — | 总览 | Sprint 索引 | [EPIC-6 M2](HANDOFFS/2026-07-03-tech-director-epic6-m2-landing-publish-sprint.md) | ✅ **关闭** |

**技术总监签核（2026-07-03）**：✅ **EPIC-6 M2 正式关闭** — C13 `71c374d`

### EPIC-9 M1 Sprint — FR-804 套餐计费（2026-07-04 排期）

| # | 角色 | 任务 | HANDOFF | 状态 |
|---|------|------|---------|:----:|
| 1 | UI | 计费设置线框 | [→UI](HANDOFFS/2026-07-04-tech-director-to-ui-epic9-billing-settings.md) | ✅ |
| 2 | 开发 Java | QuotaService + 402 拦截 | [→Java](HANDOFFS/2026-07-04-tech-director-to-dev-java-epic9-billing.md) | ✅ 2026-07-02 |
| 3 | 开发 Admin | `/settings/billing` | [→Admin](HANDOFFS/2026-07-04-tech-director-to-dev-admin-epic9-billing.md) | ✅ 2026-07-02 |
| — | 总览 | Sprint 索引 | [EPIC-9 M1](HANDOFFS/2026-07-04-tech-director-epic9-m1-billing-sprint.md) | — |

**ADR-17**：M1 额度查询 + 6 拦截点；支付/套餐 CRUD → M2

**技术总监签核**：✅ **EPIC-9 M1 正式关闭** — C14 `f23e539` · `test_billing_quota` ✅

### EPIC-11 M1 Sprint — FR-112~114 浏览器探针（2026-07-05 排期）

| # | 角色 | 任务 | HANDOFF | 状态 |
|---|------|------|---------|:----:|
| 0 | 开发 | C14 commit+push | [→C14](HANDOFFS/2026-07-05-tech-director-to-dev-c14-commit.md) | ✅ |
| 1 | UI | 探针节点线框 | [→UI](HANDOFFS/2026-07-05-tech-director-to-ui-epic11-probe-nodes.md) | ✅ |
| 2 | 开发 Java | Probe API + poll 调度 | [→Java](HANDOFFS/2026-07-05-tech-director-to-dev-java-epic11-probe.md) | ⏳ |
| 3 | 开发 Extension | Plasmo MV3 scaffold | [→Extension](HANDOFFS/2026-07-05-tech-director-to-dev-extension-epic11-plasmo.md) | ⏳ |
| 4 | 开发 Admin | `/settings/probe-nodes` | [→Admin](HANDOFFS/2026-07-05-tech-director-to-dev-admin-epic11-probe-nodes.md) | ⏳ |
| — | 总览 | Sprint 索引 | [EPIC-11 M1](HANDOFFS/2026-07-05-tech-director-epic11-m1-probe-sprint.md) | — |

**ADR-18**：M1 poll 闭环 + 1 平台（perplexity）；校准/Headless/adapter CRUD → M2+

### EPIC-7 M1 Sprint — FR-601 线索 MVP（2026-07-01 排期）

| # | 角色 | 任务 | HANDOFF | 状态 |
|---|------|------|---------|:----:|
| 1 | UI | 线索列表线框 | [→UI](HANDOFFS/2026-07-01-tech-director-to-ui-epic7-leads-list.md) | ✅ |
| 2 | 开发 Java | public leads + Admin list | [→Java](HANDOFFS/2026-07-01-tech-director-to-dev-java-epic7-leads.md) | ✅ smoke |
| 3 | 开发 Admin | 线索列表 | [→Admin](HANDOFFS/2026-07-01-tech-director-to-dev-admin-epic7-leads.md) | ✅ build |
| — | 总览 | Sprint 索引 | [EPIC-7 M1](HANDOFFS/2026-07-01-tech-director-epic7-m1-leads-sprint.md) | ✅ **关闭** |

**技术总监签核（2026-07-02）**：✅ **EPIC-7 M1 正式关闭** — C11 `76da501` · `test_public_leads_api` · ADR-14

**ADR-14**：M1 公开 POST + Admin 列表；CRM/归因 → M2

### EPIC-8 M1 Sprint — FR-701/702 报告中心（2026-07-02 排期）

| # | 角色 | 任务 | HANDOFF | 状态 |
|---|------|------|---------|:----:|
| 1 | UI | 报告中心线框 | [→UI](HANDOFFS/2026-07-02-tech-director-to-ui-epic8-reports-list.md) | ✅ |
| 2 | 开发 Java | report list + weekly + export | [→Java](HANDOFFS/2026-07-02-tech-director-to-dev-java-epic8-reports.md) | ✅ smoke |
| 3 | 开发 Admin | 报告中心页 | [→Admin](HANDOFFS/2026-07-02-tech-director-to-dev-admin-epic8-reports.md) | ✅ build |
| — | 总览 | Sprint 索引 | [EPIC-8 M1](HANDOFFS/2026-07-02-tech-director-epic8-m1-reports-sprint.md) | ✅ **关闭** |

**技术总监签核（2026-07-02）**：✅ **EPIC-8 M1 正式关闭** — C12 `e127485` · smoke docx 2885B · ADR-15

**ADR-15**：M1 列表 + 手动周报 + 导出；月报/白标/定时 → M2

### 未提交增量 — commit 批次

| 批次 | 范围 | 建议 message 前缀 | smoke |
|:----:|------|-------------------|-------|
| **C1** | FR-108 trends（Java Vo/Aggregator/test + Admin trends.vue + diagnostic.ts + wireframe/HANDOFF + `test_diagnostic_trends.py`） | `feat(core,admin): FR-108 diagnostic trends` | `test_diagnostic_trends` |
| **C2** | FR-005 RAG 预览（KnowledgeAsset search + Admin drawer + ai-client RAG models + `test_knowledge_rag_search.py`） | `feat(core,admin): FR-005 knowledge RAG search preview` | `test_knowledge_rag_search` |
| **C3** | Phase 2.2 Docling（document_parser/embed/file_storage + tests + pyproject） | `feat(ai): EPIC-10 Phase 2.2 Docling embed pipeline` | `pytest inbound-ai/tests/test_embed_docling_pipeline.py` |
| **C4** | FR-106 PDF/Gotenberg + dashboard 跳转 + report export smoke | `feat(core,admin,deploy): FR-106 PDF export and dashboard fixes` | `test_diagnostic_report_export` |
| **C5** | Sprint HANDOFFs + MEMORY + DECISIONS ADR-10/11 + design README | `docs: M2.2/EPIC-3 Sprint HANDOFFs and MEMORY` | — |
| **C6** | EPIC-3 Admin 关键词页 | `feat(admin): EPIC-3 M1 keywords list page` | ✅ `75e96cb` |
| **C7** | EPIC-4 Sprint HANDOFFs + content-task 线框 + ADR-12 | `docs: EPIC-4 M1 Sprint HANDOFFs…` | ✅ `4d8e1e2` |
| **C8** | EPIC-4 M1 全栈 | `feat(core,ai,admin): EPIC-4 M1 content…` | ✅ `23a46f6` |
| **C9** | EPIC-6 docs + ADR-13/14 + EPIC-7 HANDOFFs | `docs: EPIC-6/7 Sprint HANDOFFs and ADR-13/14` | ✅ |
| **C10** | EPIC-6 M1 全栈 landing | `feat(core,ai,admin): EPIC-6 M1 landing page generate` | ✅ `91b3ea4` |
| **C11** | EPIC-7 M1 全栈 leads | `feat(core,admin): EPIC-7 M1 public leads and Admin list` | ✅ `76da501` |
| **C12** | EPIC-8 M1 报告中心 | `feat(core,admin): EPIC-8 M1 report center and weekly report` | ✅ `e127485` |
| **C13** | EPIC-6 M2 Astro 发布 | `feat(landing,core,admin,deploy): EPIC-6 M2…` | ✅ `71c374d` |
| **C14** | EPIC-9 M1 计费 | `feat(core,admin): EPIC-9 M1 subscription quota and overage guard` | ✅ `f23e539` |
| **C15** | EPIC-11 M1 探针 | `feat(core,admin,extension): EPIC-11 M1 browser probe poll and node registry` | `test_probe_extension_e2e` |

**执行**：C1–C14 ✅ · push ✅ · **C15 ⏳** EPIC-11

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
- **验证时间**：2026-07-03（本机 Docker + **inbound-landing :4321** ✅）
- **Git 远程（2026-07-01）** ✅ `origin` = `git@github.com:vinson0522/2026_Inbound_Tourism_Acquisition_OS.git`（SSH）· `git push -u origin main` 成功 · **25 commits** · `deploy/.env` 未入库（`.gitignore`）
- **EPIC-6 M2 landing（2026-07-03）** ✅ `inbound-landing/Dockerfile` · compose `:4321` · env 文档 · `curl localhost:4321/` **200** · container **healthy**

### 本机（资源提供人 Windows — **主开发路径 ADR-09**）

| 项 | 结果 |
|----|------|
| WSL2 + Ubuntu | ✅ |
| Docker Desktop | ✅；Disk image → `D:\Dev\SDKs\Docker\wsl-data` |
| Compose 4 服务 | ✅ postgres / redis / rabbitmq / ai-api **healthy** |
| **inbound-landing** | ✅ `:4321` healthy（EPIC-6 M2 · `deploy-inbound-landing`） |
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
- **Push 状态** ✅ **2026-07-01** 运维 SSH push 完成（25 commits → `origin/main`）
- **EPIC-2 M2 FR-106（2026-06-29）** ✅ DOCX + PDF 报告导出 runId=2；`test_diagnostic_report_export.py` docx/pdf；PDF ~32KB 含 geo_score
- **EPIC-2 M2.2 FR-108 trends API（2026-06-29）** ✅ `GET /api/v1/projects/{id}/diagnostics/trends?limit=12` · 租户隔离 · 从 `diagnostic_result` 聚合 6 分项 · `DiagnosticMetricsAggregatorTest` + `test_diagnostic_trends.py`（runId=2/3 · 2 点 ASC）
- **EPIC-2 M2.2 FR-108 Admin 趋势页（2026-06-29）** ✅ `/diagnostics/trends` · ECharts 折线+分项柱图 · run 多选 2–6 · 空态「至少需要 2 次成功诊断」· 详情页链趋势
- **FR-005 知识库 RAG 检索预览（2026-06-29）** ✅ `POST .../knowledge-assets/search` → `/ai/rag/search` · Admin drawer top-3 + chunk_id · `test_knowledge_rag_search.py`（asset#1 · 1 hit）
- **EPIC-10 Phase 2.2 Docling（2026-06-29）** ✅ PDF/DOCX 解析替换 mock 切片 · `file_storage`（HTTP/MinIO/本地）· embed 优先 `file_url` · DLQ 已有 · pytest fixture PDF + `test_embed_docling_pipeline`
- **EPIC-3 M1 FR-201 keywords AI（2026-06-29）** ✅ `POST /ai/keywords/generate` · mock/无 Key 回退 · 可选 RAG top-3 · `template_service` · `test_keywords_generate.py` 7 passed
- **EPIC-3 M1 FR-201/202 keywords Java（2026-06-29）** ✅ 并入 `ruoyi-project` · `GET/POST/DELETE .../keywords` + `POST .../generate` · Feign `/ai/keywords/generate` · `tenant.excludes` 加 `keyword_opportunity` · `test_keywords_api.py` ✅
- **EPIC-3 M1 FR-201/202 Admin 关键词页（2026-06-29）** ✅ `/keywords/index` · commit `75e96cb`
- **EPIC-4 M1 FR-301/302 content AI（2026-07-01）** ✅ `POST /ai/content/generate` · hook/script/voiceover/storyboard_json · `CONTENT_MOCK_LLM` · RAG top-3 · `content_script_v1` · `test_content_generate.py` 6 passed
- **EPIC-4 M1 FR-301/302 content Java（2026-07-01）** ✅ 并入 `ruoyi-project` · `GET/POST/DELETE .../content-tasks` + 详情 + `POST .../generate` · Feign `/ai/content/generate` · `tenant.excludes` 加 `content_task`/`generated_content` · `test_content_api.py` ✅
- **EPIC-4 M1 FR-301/302 Admin 内容任务页（2026-07-01）** ✅ `/content-tasks` · 列表/创建/生成/预览 drawer · 关键词「创建内容」FR-205 · `vite build` ✅
- **EPIC-6 M1 FR-502~505 landing AI（2026-07-01）** ✅ `POST /ai/landing/generate` · PRD §20.3 八模块 `content_json` · `seo_meta_json`/`form_config_json` · `LANDING_MOCK_LLM` · `landing_generate_v1` · `test_landing_generate.py` 6 passed
- **EPIC-6 M1 FR-501~505 landing Java（2026-07-01）** ✅ `GET/POST/DELETE .../landing-pages` + 详情 + `POST .../generate` · Feign `/ai/landing/generate` · `tenant.excludes` 加 `landing_page` · `test_landing_api.py` ✅
- **EPIC-6 M1 FR-501~505 Admin 落地页（2026-07-01）** ✅ `/landing-pages` 列表 + 创建 dialog + AI 生成 + JSON/SEO 预览 drawer · 关键词「转落地页」· `pnpm build:prod` ✅
- **EPIC-6 M2 landing publish Java（2026-07-03）** ✅ `PublicLandingPageController` + publish/unpublish · `LandingPublishProperties` · `PublicApiCorsConfig` · Turnstile siteverify · `test_landing_publish_e2e.py` ✅
- **EPIC-6 M2 Astro landing（2026-07-03）** ✅ `/p/[projectId]/[slug]` hybrid SSR · 八模块 · Turnstile LeadForm · `404.astro` · `pnpm build` ✅ · `test_landing_astro_e2e.py` · 404 友好页 ✅
- **EPIC-6 M2 Admin 落地页发布（2026-07-03）** ✅ 列表/ drawer 发布·下线·公网预览 · `publishLandingPage`/`unpublishLandingPage` · `pnpm build:prod` ✅
- **EPIC-7 M1 FR-601 leads Java（2026-07-01）** ✅ `POST /api/v1/public/leads` + `GET .../leads` 列表/详情 · Turnstile M1 stub · IP+landingPageId 限流 · `tenant.excludes` 加 `lead` · `test_public_leads_api.py` ✅
- **EPIC-7 M1 FR-601 Admin 线索页（2026-07-01）** ✅ `/leads` 侧栏 + 列表/筛选/脱敏 + 详情 drawer · `maskPii` · `pnpm build:prod` ✅
- **EPIC-8 M1 FR-701/702 Admin 报告中心（2026-07-02）** ✅ `/reports` 侧栏 + 列表/筛选 + 周报 dialog + 预览 drawer + DOCX/PDF 下载 · `pnpm build:prod` ✅
- **EPIC-9 M1 FR-804 billing Java（2026-07-02）** ✅ `GET /api/v1/settings/billing` · `QuotaService.checkAndConsume` · 6 拦截点（项目/诊断/关键词/内容/落地页/周报）· HTTP 402 `code=40201` · `tenant.excludes` 加 `subscription` · `test_billing_quota.py` ✅
- **EPIC-9 M1 FR-804 Admin 计费页（2026-07-02）** ✅ `/settings/billing` 只读用量 · 6×`el-progress` · 超额/预警 alert · Axios 402 全局提示 · `pnpm build:prod` ✅
- **EPIC-10 Phase 2 embed MVP（2026-06-29）** ✅ **已提交** `f40cf8d` — `ai.embed` worker · asset#1 READY
- **M2 代码（2026-06-29）** ✅ **已提交** `f40cf8d` / `e22cd43` / `f96ba7e`（已 push）
- **EPIC-2 M1 代码** ✅ **已提交** `54d8ca5` / `6ba5e1e` / `48926d2`（已 push）
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
  - `test_content_api` ✅ task create → generate → detail → delete
- **C8 后全量 smoke 9/9（2026-07-01 10:28 · commit `23a46f6`）** — 前提：Docker 4 容器 healthy + Java `:8080` + `inbound-ai` `:8090`
  - **结果：6/9**（EPIC-4 相关 **2/2 ✅**；EPIC-2 diagnostic 链 **0/3 ❌** 环境阻塞，非 C8 回归）

  | # | 脚本 | 结果 | 备注 |
  |---|------|:----:|------|
  | 1 | `test_projects_api` | ✅ | project id=3 新建 |
  | 2 | `test_diagnostic_e2e` | ❌ | runId=**2** · 240s 轮询仍 `RUNNING` · diagnose MQ worker 未消费（ai-api 日志无 `/ai/diagnose`） |
  | 3 | `test_embed_e2e` | ✅ | direct embed READY · RAG 1 hit chunkId=2 |
  | 4 | `test_diagnostic_report_export` | ❌ | runId=2 非 SUCCESS · 返回 JSON 83B（无可用报告） |
  | 5 | `test_diagnostic_trends` | ❌ | `runs=0`（无 SUCCESS 历史 run） |
  | 6 | `test_ai_health` | ✅ | `/health` + `/ai/health` litellm=ready |
  | 7 | `test_knowledge_rag_search` | ✅ | asset#1 · 1 hit chunkId=2 |
  | 8 | `test_keywords_api` | ✅ | insertedCount=3 · inspiration total 4→7 |
  | 9 | `test_content_api` | ✅ | taskId=6 · generate · detail · delete |

  - **EPIC-4 必测**：`uv run pytest tests/test_content_generate.py -q` **6 passed**（同会话）
  - **根因（diagnostic 3 项）**：本机 PG 卷重置后无 SUCCESS run；`DIAGNOSE_MOCK_LLM` 未开 / diagnose worker 未跑通 → run 卡在 RUNNING
  - **修复建议**：`deploy/.env` 设 `DIAGNOSE_MOCK_LLM=true` → `docker compose up -d ai-api`；或取消 runId=1/2 后重跑 e2e
- **E2E（本机 Docker ADR-09）** ⚠️ 历史 runId=2/3 SUCCESS 已随 PG 卷丢失；需 mock 或真 Gemini 重跑
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
5. Smoke（C8 后 9/9）：见上表 · 当前 **6/9** · EPIC-4 **2/2 ✅**

### 开发联调步骤（SSH 隧道 — **备用**，ADR-05）

见 `docs/INFRA_ACCESS.local.md` §6；Redis **6380** + 密码；PG 可能 **15432**。

---

## UI 设计

- **已完成**：… billing-settings (EPIC-9 M1) ✅ · **probe-nodes (EPIC-11 M1)** ✅
- **HANDOFF**：[探针节点 → 开发](HANDOFFS/2026-07-05-ui-to-developer-probe-nodes.md)
- **待办**：线索 CRM M2 · 报告白标/月报 M2 · 探针今日任务量 M2

---

## 阻塞项

| ID | 描述 | 负责人 | 状态 |
|----|------|--------|------|
| B-01 | ~~本机不装 Docker，隧道联调~~ | 用户 | **已关闭** → ADR-09 本机 Docker |
| B-02 | ~~本机若依表未 import~~ | 运维 | **已关闭** 2026-06-27 |
| B-03a | 本地 GEO E2E **mock LLM**（`DIAGNOSE_MOCK_LLM=true`） | 开发 | ✅ 2026-06-29 runId=2/3 · geo_score=85 |
| B-03b | 本地 GEO E2E **真 Gemini**（grounded-api） | 开发 | ⏳ 配额用尽；`DIAGNOSE_MOCK_LLM=false` 时 FAILED |
| B-04 | ~~工作区未 commit（C1–C6）~~ | 开发 | **已关闭** 2026-07-01 · `2ffa3d7` Admin 关键词页 |
| B-05 | ~~EPIC-3 M1 仓库签核~~ | 技术总监 | **已关闭** 2026-07-01 · `75e96cb` |
| B-06 | ~~EPIC-4 M1 C8 未 commit~~ | 开发 | **已关闭** · `23a46f6` |
| B-07 | ~~EPIC-6 M1 C9+C10 未 commit/push~~ | 开发 | ✅ **已关闭** C10 `91b3ea4` + C9 docs |
| B-08 | ~~EPIC-7 M1 C11 未 commit/push~~ | 开发 | ✅ **已关闭** C11 `76da501` |
| B-09 | ~~EPIC-8 M1 Java report API 未实现~~ | 开发 Java | ✅ **已关闭** smoke docx 2885B |
| B-10 | ~~EPIC-8 M1 C12 未 commit/push~~ | 开发 | ✅ **已关闭** C12 `e127485` |
| B-11 | ~~EPIC-6 M2 C13 未 commit/push~~ | 开发 | ✅ **已关闭** `71c374d` |

---

## 下一步（跨角色 · 2026-07-05）

| 优先级 | 窗口 | 动作 |
|:------:|------|------|
| **P0** | **开发 Java** | [Probe API](HANDOFFS/2026-07-05-tech-director-to-dev-java-epic11-probe.md) | poll + browser-extension 分支 |
| **P1** | **开发 Extension** | [Plasmo scaffold](HANDOFFS/2026-07-05-tech-director-to-dev-extension-epic11-plasmo.md) | 依赖 Java #2 |
| **P1** | **开发 Admin** | [探针节点页](HANDOFFS/2026-07-05-tech-director-to-dev-admin-epic11-probe-nodes.md) | 依赖 Java API · 线框 ✅ |
| **P2** | **开发** | EPIC-3 M2 FR-203 关键词评分 | [可选 Sprint](HANDOFFS/2026-06-29-tech-director-epic3-m2-keyword-score-sprint.md) 并行 |
| **P3** | **开发** | diagnostic smoke 9/9 恢复 | `DIAGNOSE_MOCK_LLM=true` |

---

## 更新日志（最近 5 条）

| 日期 | 角色 | 摘要 |
|------|------|------|
| 2026-07-05 | 开发 | C14 `f23e539` EPIC-9 M1 计费 commit+push · `test_billing_quota` ✅ |
| 2026-07-05 | UI 设计 | EPIC-11 M1 probe-nodes 线框 FR-113 · 在线/离线 · 空态安装引导 · HANDOFF |
| 2026-07-05 | 技术总监 | 定案 **EPIC-11 M1** 浏览器探针 · ADR-18 · 6 条 HANDOFF（含 C14 前置） |
| 2026-07-02 | 开发 Admin | EPIC-9 M1 `/settings/billing` + Axios 402 全局提示 · `build:prod` ✅ |
| 2026-07-02 | 开发 Java | EPIC-9 M1 QuotaService + GET billing + 6×402 拦截 · `test_billing_quota` ✅ |
| 2026-07-04 | UI 设计 | EPIC-9 M1 billing-settings 线框 FR-804 · 6 quota 进度条 · 超额 alert · HANDOFF |
| 2026-07-04 | 技术总监 | 定案 **EPIC-9 M1** 计费 · ADR-17 · 3 条 HANDOFF |
| 2026-07-03 | 技术总监 | **EPIC-6 M2 正式关闭** C13 `71c374d` |
| 2026-07-03 | 开发 | C13 `71c374d` EPIC-6 M2 全栈 push · smoke publish + astro ✅ |
| 2026-07-03 | 开发 Admin | EPIC-6 M2 落地页发布/下线/公网预览 · drawer + 列表 · build:prod ✅ |
| 2026-07-03 | 开发 Landing | EPIC-6 M2 Astro `/p/[projectId]/[slug]` 八模块 + Turnstile LeadForm · `pnpm build` · 404 smoke ✅ |
| 2026-07-03 | 开发 Java | EPIC-6 M2 public landing + publish/unpublish + CORS + Turnstile siteverify · smoke ✅ |
| 2026-07-03 | 运维 | EPIC-6 M2 `inbound-landing` Dockerfile + compose :4321 + env 文档 · curl 200 healthy |
| 2026-07-02 | 开发 Java | EPIC-8 M1 Report API list/weekly/export · `test_reports_api.py` ✅ |
| 2026-07-03 | UI 设计 | EPIC-6 M2 landing-page-publish 线框 · Admin 发布/下线 + Astro 八模块 Turnstile · HANDOFF |
| 2026-07-03 | 技术总监 | 定案 **EPIC-6 M2** · ADR-16 · 5 条 HANDOFF（UI/Java/运维/Landing/Admin） |
| 2026-07-02 | 技术总监 | **EPIC-8 M1 正式关闭** C12 `e127485` |
| 2026-07-02 | 开发 | C12 commit+push EPIC-8 全栈 report center |
| 2026-07-02 | 开发 Java | EPIC-8 Report API + weekly 聚合 + export · `test_reports_api` ✅ |
| 2026-07-02 | UI 设计 | EPIC-8 M1 reports-list 线框 FR-701/702 · 周报 dialog · DOCX/PDF · HANDOFF 开发 |
| 2026-07-02 | 技术总监 | **EPIC-7 M1 正式关闭** C11 `76da501` · 排 **EPIC-8 M1** HANDOFF + ADR-15 |
| 2026-07-01 | 技术总监 | EPIC-7 M1 功能签核 ✅ · C11 commit |
| 2026-07-01 | 开发 Admin | EPIC-7 M1 线索列表 + 详情 drawer + 侧栏 `/leads` · build ✅ |
| 2026-07-01 | 开发 | EPIC-7 M1 FR-601 public leads + Admin list/detail · `test_public_leads_api.py` ✅ |
| 2026-07-01 | 技术总监 | **EPIC-6 M1 功能签核 ✅** · 排 **EPIC-7 M1** HANDOFF + ADR-14 · B-07 C9/C10 |
| 2026-07-01 | 开发 | push C9 `96dcd4e` + C10 `91b3ea4` · EPIC-6 smoke 全过 |
| 2026-07-01 | 开发 | C10 `91b3ea4` EPIC-6 landing 全栈 · pytest 6/6 + `test_landing_api` ✅ |
| 2026-07-01 | 开发 | EPIC-6 M1 Admin 落地页列表/创建/预览 drawer · 关键词转落地页 · build ✅ |
| 2026-07-01 | 开发 | EPIC-6 M1 landing Java CRUD + generate · `test_landing_api.py` ✅ |
| 2026-07-01 | 开发 | EPIC-6 M1 `POST /ai/landing/generate` · 八模块 content_json · pytest 6 passed |
| 2026-07-01 | UI 设计 | EPIC-7 M1 leads-list 线框 FR-601 · 详情 drawer · PII 脱敏 · HANDOFF 开发 |
| 2026-07-01 | UI 设计 | EPIC-6 M1 landing-page-list 线框 FR-501~505 · 创建/预览 · HANDOFF 开发 |
| 2026-07-01 | 运维 | Git `origin` SSH 配置 + `git push -u origin main`（25 commits）；`deploy/.env` 未入库 |
| 2026-07-01 | 开发 | C8 后全量 smoke **6/9**（EPIC-4 2/2 ✅ · diagnostic 3 项 MQ/mock 阻塞） |
| 2026-07-01 | 开发 | commit `4d8e1e2` C7 EPIC-4 docs/wireframe |
| 2026-07-01 | 开发 | EPIC-4 M1 content Java CRUD + generate · `test_content_api.py` ✅ |
| 2026-07-01 | 开发 | EPIC-4 M1 `POST /ai/content/generate` · mock/RAG/template · pytest 6 passed |
| 2026-07-01 | 开发 | commit `75e96cb` EPIC-3 M1 Admin 关键词页 · B-04 全关 |
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
