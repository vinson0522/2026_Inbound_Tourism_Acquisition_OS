# HANDOFF | 技术总监 → 开发（inbound-core + inbound-admin）

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-06-26 |
| **优先级** | High |
| **关联** | EPIC-2 M1 · FR-103~105 · ADR-20260626-07 · [Sprint 总览](2026-06-26-tech-director-epic2-geo-sprint.md) |

## 上下文

**当前状态**：
- 项目 CRUD ✅（`ruoyi-project` 模式可复用 PG TypeHandler、`BusinessTenantHelper`）
- Admin GEO 列表/详情 **mock**（`VITE_TOURGEO_MOCK` 默认 true）
- AI 客户端 ✅（`ruoyi-ai-client` RestTemplate + internal token）
- Python `/ai/diagnose` **待** [AI HANDOFF](2026-06-26-tech-director-to-dev-ai-epic2-diagnose.md) 完成

**本 HANDOFF**：Java 诊断域 CRUD + 状态机 + MQ 调度 + Admin 接真实 API。

**相关文件**：
- `database/ddl/001_schema.sql` — `diagnostic_run`、`diagnostic_result`、`probe_task`、`question_bank`、`scoring_rule`
- `database/ddl/002_seed_demo.sql` — demo project_id=1，3 条 question_bank
- `inbound-admin/src/api/tourgeo/diagnostic.ts` — 替换 mock
- `docs/design/wireframes/diagnostic-detail.md` — API 对照表
- `ruoyi-modules/ruoyi-project/` — 包结构参考

**约束**：
- Java 管事务、状态机、MQ 投递；**禁止** Java 内直接调 OpenAI/Perplexity SDK
- 创建诊断默认 `probe_modes_json=["grounded-api"]`；Admin 已有合规校验须保留
- 租户：`tenant_id` 过滤（复用 `BusinessTenantHelper`）
- API 路径以 PRD 为准；响应可暂用 RuoYi `R`/`TableDataInfo`（与 Story 2 一致）

## 交付请求

**需要什么**：实现诊断任务 M1 全链路 —— **创建 run → 拆 probe_task → MQ → AI 回调 → 聚合 score → Admin 可查**。

## 验收标准

### 1. 模块结构（Java）

新增 `ruoyi-modules/ruoyi-diagnostic`（或 `ruoyi-tourgeo-diagnostic`）：

```
controller → DiagnosticController
service    → DiagnosticRunService（状态机）
mapper     → DiagnosticRunMapper, DiagnosticResultMapper, ProbeTaskMapper, QuestionBankMapper
domain     → 对齐 DDL 字段 + PG JSONB/ENUM TypeHandler（复用 project 模块模式）
```

- [ ] 注册到 `ruoyi-admin` pom 依赖

### 2. HTTP API（M1）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/projects/{projectId}/diagnostics` | 创建 run（FR-103）；202 或 200 + runId |
| GET | `/api/v1/projects/{projectId}/diagnostics` | 分页列表 |
| GET | `/api/v1/diagnostics/{runId}` | 详情 + 进度 + geo_score |
| GET | `/api/v1/diagnostics/{runId}/results` | 问题明细分页 |
| GET | `/api/v1/diagnostics/{runId}/probe-tasks` | 子任务进度 |
| POST | `/api/v1/internal/diagnostics/probe-callback` | **内网** AI worker 回调写 `diagnostic_result` |

**创建请求字段**（对齐 Admin `CreateDiagnosticForm` + DDL）：
- `name`, `market`, `locale`, `region`
- `probeModes`: 仅允许 `["grounded-api"]`（M1）
- `models`: 默认 **`["gemini/gemini-2.0-flash"]`**（ADR-20260626-08；无 Perplexity Key）
- `platform` in MQ payload：`gemini`（非 perplexity）
- `sampleCount`: 默认 1
- `questionScope`: `{ "questionIds": [...] }` 或 `{ "stage": "planning" }` 从 `question_bank` 解析

**创建流程**：
1. INSERT `diagnostic_run` status=`PENDING`
2. 解析 question 列表（M1：demo 项目 3 题全选或 scope 指定）
3. 每题 × 每 platform × sample_count → INSERT `probe_task` status=`PENDING`
4. UPDATE run → `RUNNING`；投递 RabbitMQ `diag.grounded-api`（每条 probe_task 一条消息）
5. 消息体含 `trace_id`, `runId`, `probeTaskId`, `questionId`, `platform`, `region`, `locale`, `sampleIndex`

### 3. 状态机（`AGENTS.md` §6.4）

```
diagnostic_run: PENDING → RUNNING → SUCCESS | PARTIAL_FAILED | FAILED | CANCELLED
probe_task:     PENDING → DISPATCHED → RUNNING → SUCCESS | FAILED | RETRY
```

- [ ] 全部 probe_task 终态 → 调 Python `POST /ai/score` → UPDATE `diagnostic_run.geo_score` + status
- [ ] 部分失败 → `PARTIAL_FAILED`，仍可查结果

### 4. AI 集成

- [ ] 扩展 `ruoyi-ai-client`：`diagnose(DiagnoseRequest)`、`score(ScoreRequest)` → RestTemplate
- [ ] Callback 端点校验 internal token 或 IP 白名单（内网）
- [ ] `application.yml` 已有 `ai.service.base-url` / `internal-token`

### 5. RabbitMQ

- [ ] Spring AMQP 配置（`application-dev.yml` 隧道 RabbitMQ 5672）
- [ ] Producer：`diag.grounded-api`
- [ ] M1 不要求 Java 消费；消费在 inbound-ai worker

### 6. Admin（inbound-admin）

- [ ] `diagnostic.ts`：实现真实 API；`VITE_TOURGEO_MOCK=false` 可跑通
- [ ] 列表页：创建抽屉 POST 成功后刷新
- [ ] **新增** `views/tourgeo/diagnostics/detail.vue` — 按 [UI HANDOFF](2026-06-26-ui-to-developer-diagnostic-detail.md)
- [ ] 路由 `/diagnostics/runs/:runId`（hidden，activeMenu 指向列表）
- [ ] RUNNING 轮询 GET run + probe-tasks

### 7. 联调脚本

- [ ] `deploy/scripts/test_diagnostic_e2e.py`：login → create diagnostic (project 1) → poll until SUCCESS → GET results

### 8. 文档

- [ ] 更新 HANDOFF Done + `MEMORY.md` 开发章节

## M1 验收场景（手工 / 脚本）

1. Admin 选 demo 项目 → GEO 诊断 → 新建（grounded-api，US，1 次采样）
2. 列表出现 `RUNNING` → 终态 `SUCCESS`（或 `PARTIAL_FAILED` 若个别题失败）
3. 详情页：geo_score 有值；问题明细 ≥1 条；合规 alert 展示 probe_mode/region
4. 日志含 Langfuse trace_id（若配置）

## 质量 / 证据

- `test_diagnostic_e2e.py` 输出
- Admin 截图：列表 SUCCESS + 详情 KPI
- PG 查询：`SELECT status, geo_score FROM diagnostic_run WHERE id=?`

**交给下一棒**：M2 报告导出 FR-106；FR-101 题库生成；多平台

---

## Done（由 To 角色填写）

- **完成时间**：2026-06-26
- **结果摘要**：
  - 新增 `ruoyi-diagnostic`：CRUD + 状态机 + RabbitMQ producer + probe-callback
  - 默认探针 ADR-08：`platform=gemini`，`model=gemini/gemini-2.0-flash`
  - `ruoyi-ai-client.score()`；Python `parse_gemini` + grounded tools
  - Admin `diagnostic.ts` 接真实 API；E2E 脚本 `deploy/scripts/test_diagnostic_e2e.py`
  - `mvn compile` 通过
- **遗留**：
  - E2E 需本机 Java + inbound-ai worker + RabbitMQ 隧道 + `GEMINI_API_KEY`
  - Admin 详情页 `detail.vue` 待 UI HANDOFF 下一棒
