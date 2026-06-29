# Agent 团队共享记忆（Single Source of Truth）

> **所有 Cursor Agent 窗口在开始任务前必读，结束任务后必更新。**  
> 各窗口会话互不共享上下文；本文件 + `DECISIONS.md` + `HANDOFFS/` 即「共享记忆」。

| 字段 | 值 |
|------|-----|
| **最后更新** | 2026-06-29 |
| **更新角色** | 开发 |
| **当前 EPIC 焦点** | **EPIC-2 M2** FR-106 报告导出 ✅ → 趋势 FR-108 / 多平台 |

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
**M2 FR-106（2026-06-29）** ✅ DOCX 导出 + 合规元数据；PDF 需 Gotenberg（文档化）  
**E2E**：隧道 runId=10 ✅；**本地 runId=2 ✅** mock LLM geo_score=85

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
- **6/29 增量二（2026-06-29）** ✅ **已提交** `a87b780` Story3 · `91c215b` Phase2.1 · docs HANDOFF/wireframes 同批（未 push）
- **EPIC-2 M2 FR-106（2026-06-29）** ✅ DOCX 报告导出 runId=2；`test_diagnostic_report_export.py` 通过
- **EPIC-10 Phase 2 embed MVP（2026-06-29）** ✅ **已提交** `f40cf8d` — `ai.embed` worker · asset#1 READY
- **M2 代码（2026-06-29）** ✅ **已提交** `f40cf8d` ai embed · `e22cd43` core FR-106 report · `f96ba7e` docs（未 push）
- **EPIC-2 M1 代码** ✅（Python + Java + Admin 详情页）— **已提交** `54d8ca5` / `6ba5e1e` / `48926d2`（2026-06-29）
- **E2E（隧道）** ✅ runId=10 mock LLM
- **E2E（本机 Docker ADR-09，2026-06-29）** ✅ runId=2 · `SUCCESS` · `geo_score=85.00` · 3 results · `DIAGNOSE_MOCK_LLM=true`
- **本机 Docker 联调** ✅ Java :8080 + 4 容器 healthy；`test_projects_api` + E2E 通过
- **工作台 FR-006 MVP（2026-06-26）** ✅ `getDashboard` 聚合诊断列表；`/dashboard` 编译与运行正常
- **待办**：FR-005 检索预览 UI（Admin）；真 embedding 生产 Key 轮换；Docling 文件解析

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
5. Smoke：`test_projects_api.py` / `test_diagnostic_e2e.py` / `test_embed_e2e.py` / **`test_diagnostic_report_export.py`**

### 开发联调步骤（SSH 隧道 — **备用**，ADR-05）

见 `docs/INFRA_ACCESS.local.md` §6；Redis **6380** + 密码；PG 可能 **15432**。

---

## UI 设计

- **已完成**：dashboard / diagnostics-list / diagnostic-detail / projects-list / **project-detail（Story 3）**
- **HANDOFF**：[项目详情 → 开发](HANDOFFS/2026-06-29-ui-to-developer-project-detail.md)
- **待办**：诊断趋势 FR-108；关键词/内容模块线框

---

## 阻塞项

| ID | 描述 | 负责人 | 状态 |
|----|------|--------|------|
| B-01 | ~~本机不装 Docker，隧道联调~~ | 用户 | **已关闭** → ADR-09 本机 Docker |
| B-02 | ~~本机若依表未 import~~ | 运维 | **已关闭** 2026-06-27 |
| B-03 | 本地 E2E 真 Gemini | 开发 | ⏳ 配额用尽；**mock 管道已验通**（runId=2） |

---

## 下一步（跨角色）

| 优先级 | 窗口 | 动作 | HANDOFF |
|:------:|------|------|---------|
| **P0** | ~~**运维**~~ | ~~import 若依 + smoke + MEMORY Done~~ | ✅ 2026-06-27 |
| **P1** | ~~**开发**~~ | ~~本地 E2E mock LLM~~ | ✅ 2026-06-29 runId=2 |
| **P2** | ~~**开发 Admin**~~ | ~~补 `getDashboard` stub~~ | ✅ 2026-06-26 |
| **P3** | ~~**开发**~~ | ~~EPIC-2 M2 FR-106 报告导出~~ | ✅ 2026-06-29 |
| **P4** | **技术总监** | M2 签核 → FR-108 趋势 / 多平台 | — |
| **P5** | ~~**开发**~~ | ~~Story 3 项目详情页~~ | ✅ 2026-06-29 |

---

## 更新日志（最近 5 条）

| 日期 | 角色 | 摘要 |
|------|------|------|
| 2026-06-29 | 开发 | 6/29 增量二 commit：`a87b780` Story3 · `91c215b` Phase2.1 · docs HANDOFF（未 push） |
| 2026-06-29 | 开发 | EPIC-10 Phase 2.1：OpenAI embedding + bge-reranker top-3；pytest + test_embed_e2e ✅ |
| 2026-06-29 | 开发 | EPIC-2 M2 FR-106：DOCX 报告导出 + Admin 按钮；runId=2 smoke 通过 |
| 2026-06-29 | 开发 | EPIC-10 Phase 2 embed MVP：`ai.embed` + chunk/pgvector + `/ai/rag/search`；E2E asset#1 READY |
| 2026-06-29 | 开发 | M1 代码分批 commit：`54d8ca5` ai · `6ba5e1e` core · `48926d2` admin+deploy（未 push） |
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
