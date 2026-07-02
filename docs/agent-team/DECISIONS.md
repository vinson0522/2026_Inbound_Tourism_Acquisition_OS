# 架构与产品决策日志（ADR 轻量版）

> 技术总监主笔；开发/UI/运维可 **提议**，仅技术总监或用户可 **定案**。  
> 所有 Agent 实施前扫一眼最近 5 条，避免推翻已定决策。

---

## 记录格式

```markdown
### ADR-YYYYMMDD-NN | 标题
- **状态**：已采纳 / 已废弃 / 待讨论
- **决策者**：技术总监 / 用户
- **背景**：…
- **决策**：…
- **影响**：涉及目录/EPIC/角色 …
```

---

## 已采纳

### ADR-20260623-01 | 业务底座选用 RuoYi-Vue-Plus
- **状态**：已采纳
- **决策者**：用户 + 文档基线
- **背景**：需要多租户 IAM、代码生成、快速 CRUD
- **决策**：拉取 RuoYi-Vue-Plus 5.X + plus-ui；**不**采用若依内置业务表
- **影响**：`inbound-core`、`inbound-admin`；业务表以 `001_schema.sql` 为准

### ADR-20260623-02 | MVP 数据库统一 PostgreSQL
- **状态**：已采纳
- **决策者**：ARCHITECTURE / PRD
- **背景**：业务 + pgvector 向量合一
- **决策**：不用 MySQL；若依需改数据源
- **影响**：运维 compose、开发 Flyway、若依配置

### ADR-20260625-03 | 多 Agent 共享记忆走文件而非会话
- **状态**：已采纳
- **决策者**：用户
- **背景**：多 Cursor 窗口互不共享上下文
- **决策**：`docs/agent-team/MEMORY.md` + `DECISIONS.md` + `HANDOFFS/`
- **影响**：所有角色；见 `AGENTS.md` §21

### ADR-20260625-04 | 若依与业务表共用单库 PostgreSQL
- **状态**：已采纳
- **决策者**：技术总监
- **背景**：ADR-02 已定 MVP 统一 PG；若依默认 MySQL；`001_schema.sql` 已通过 compose init 写入 `inbound_growth`
- **决策**：**单库** `inbound_growth`（`public` schema）；若依系统表走 RuoYi 自带 Flyway/SQL 脚本；业务表保持 `001_schema.sql` 命名；**不做** MySQL 双库过渡
- **影响**：运维 compose 验证；开发改 `application-dev.yml` + PG driver；Langfuse 同库共存

### ADR-20260625-05 | 本机无 Docker 时用 SSH 隧道联调共享服务器
- **状态**：已采纳（**本机开发机已被 ADR-09 取代**；隧道仍适用于未装 Docker 的备用场景）
- **决策者**：技术总监
- **背景**：Windows 开发机未装 Docker Desktop；共享服务器 `18.139.209.10` 基础设施已全绿
- **决策**：EPIC-1 Sprint-1 **不阻塞**于本机 compose；开发通过 SSH 隧道映射 PG(5432)、Redis(6380)、RabbitMQ(5672)，`application-dev.yml` 用 `INBOUND_*` 环境变量注入密码；本机 PG 占 5432 时隧道改 `15432` + `INBOUND_PG_PORT=15432`；本地 Docker 列为 P2 便利项
- **影响**：开发联调步骤见 `MEMORY.md`「开发 → 开发联调步骤」；`docs/INFRA_ACCESS.local.md` §6；**首选**见 ADR-09 `deploy/LOCAL_DOCKER.md`

### ADR-20260627-09 | 本机开发改用 D 盘 Docker Compose（取代隧道）
- **状态**：已采纳
- **决策者**：用户 + 技术总监
- **背景**：SSH 隧道联调远程 PG/MQ 延迟高；用户要求本地化且 **不污染 C 盘**
- **决策**：
  - **本机主路径**：Docker Desktop (WSL2) + `docker-compose.yml` + `docker-compose.local-d.yml`
  - **数据落盘**：镜像 VHDX → `D:\Dev\SDKs\Docker\wsl-data`；业务卷 bind mount → `D:\Dev\SDKs\Docker\inbound-growth\`
  - **最小服务集**：postgres / redis / rabbitmq / ai-api（Java/Admin 仍本机 IDE）
  - **ai-api worker**：`CORE_CALLBACK_BASE_URL=http://host.docker.internal:8080`
  - **共享服务器** `18.139.209.10` 保留 staging；**禁止**隧道与本地 Docker 混用
  - Windows PostgreSQL-16 保持停止，5432 给容器
- **影响**：`deploy/LOCAL_DOCKER.md`、bootstrap/cleanup/import 脚本；运维 HANDOFF [local-docker-finish](HANDOFFS/2026-06-27-tech-director-to-devops-local-docker-finish.md)；`MEMORY.md` 联调步骤更新

### ADR-20260625-06 | EPIC-10 分两 Phase 交付
- **状态**：已采纳
- **决策者**：技术总监
- **背景**：EPIC-10 全量（RAG/embed/worker/LangGraph）过大，阻塞 EPIC-2 启动
- **决策**：
  - **Phase 1（当前 Sprint）**：FastAPI scaffold + internal token + LiteLLM gateway + `/health` + Dockerfile
  - **Phase 2**：embed worker、RAG、RabbitMQ 消费者、LangGraph Agent
  - Phase 1 **不引入** sentence-transformers / Docling / LlamaIndex 依赖
- **影响**：`inbound-ai/pyproject.toml` 最小依赖；运维 compose `ai-api` 在 Dockerfile 就绪后启用

### ADR-20260626-07 | EPIC-2 M1 仅 grounded-api + demo 题库
- **状态**：已采纳
- **决策者**：技术总监
- **背景**：FR-101~110 全做周期过长；UI/AI/Java 底座已就绪
- **决策**：
  - **M1**：`grounded-api` 单模式；demo `question_bank` 3 题；`sample_count=1`
  - 调度：RabbitMQ `diag.grounded-api` + Python worker + Java callback
  - **M1 不做**：browser-extension、FR-101 自动生成 100 题、FR-106 报告导出、多平台并行
- **影响**：[EPIC-2 Sprint HANDOFF](HANDOFFS/2026-06-26-tech-director-epic2-geo-sprint.md)

### ADR-20260626-08 | M1 无 Perplexity 时用 Gemini Grounding
- **状态**：已采纳
- **决策者**：技术总监 + 用户（成本约束）
- **背景**：Perplexity API 成本高，用户暂不提供 Key；OpenAI/Gemini Key 已有
- **决策**：
  - **M1 默认探针**：`platform=gemini`，`model=gemini/gemini-2.0-flash`（`grounding_enabled=true`）
  - **OpenAI** 作备选；**Perplexity** 推迟至 M2 或用户后续提供 Key
  - Java/Admin 默认模型改 Gemini；Python 补 `parse_gemini`
- **影响**：不阻塞 Java diagnostic HANDOFF

### ADR-20260629-10 | FR-108 趋势复用 diagnostic_run，不建新表
- **状态**：已采纳
- **决策者**：技术总监
- **背景**：FR-108 需多次诊断历史对比；`diagnostic_run` 已有 `geo_score`、`finished_at`；`diagnostic_result.score_json` 可支撑分项
- **决策**：
  - **M2.2 不新增**趋势/快照表；Java 聚合查询 `diagnostic_run`（+ 可选 join `diagnostic_result`）
  - 参与趋势：`status IN (SUCCESS, PARTIAL_FAILED)` 且 `geo_score IS NOT NULL`
  - Admin：最少 2 条 run 才渲染图表（与 UI 线框一致）
  - **M2.2 不做**：FR-109 定时任务、竞品趋势、多平台拆分
- **影响**：[EPIC-2 M2.2 Sprint](HANDOFFS/2026-06-29-tech-director-epic2-m22-fr108-sprint.md)；`ruoyi-diagnostic` trends API；Admin `trends.vue`

### ADR-20260629-11 | EPIC-3 M1 仅 FR-201/202 MVP，评分推迟 M2
- **状态**：已采纳
- **决策者**：技术总监
- **背景**：EPIC-3 全量（FR-201~207）过大；DDL `keyword_opportunity` 已就绪；RAG/LLM 底座已通
- **决策**：
  - **M1 做**：Python `/ai/keywords/generate`；Java CRUD + generate 落库；Admin 列表 + 八阶段筛选 + 生成按钮
  - **M1 词量**：每阶段 **≥5 词**（非 PRD 全文 ≥10）；`words_per_stage` 可配置
  - **M1 不做**：FR-203 完整机会评分公式、词库运营后台、编辑/合并/导出、转内容任务（FR-204+）
  - `score`/`score_detail_json` 可占位；`source_json` 记 AI + chunk_ids
  - 生成调用：**M1 同步** Feign 调 AI；高耗时再改 MQ `ai.keywords`
- **影响**：[EPIC-3 M1 Sprint](HANDOFFS/2026-06-29-tech-director-epic3-m1-keywords-sprint.md)；`ruoyi-keyword` 新模块；`inbound-ai` keywords router

### ADR-20260629-12 | EPIC-4 M1 仅 FR-301/302 脚本 MVP
- **状态**：已采纳
- **决策者**：技术总监
- **背景**：EPIC-4 全量（FR-301~308）含排期/多语言/图文；DDL `content_task` + `generated_content` 已就绪；EPIC-3 关键词可关联 `keyword_id`
- **决策**：
  - **M1 做**：从 keyword 创建 `content_task`；Python `/ai/content/generate` 输出 hook/script/voiceover/storyboard_json/cta；Java CRUD + 同步 generate；Admin 列表 + 预览 drawer
  - **M1 时长**：15/30/60s 三档；platform 枚举 MVP（如 `youtube_shorts` / `tiktok`）
  - **M1 不做**：FR-303 分镜导出、FR-304~308、LangGraph 多节点、TipTap 编辑、MQ `ai.content`
  - `needs_human_review` 默认 true；Prompt 读 `template` 表
- **影响**：[EPIC-4 M1 Sprint](HANDOFFS/2026-06-29-tech-director-epic4-m1-content-sprint.md)；`ruoyi-project` 或 `ruoyi-content`；`inbound-ai` content router

### ADR-20260701-13 | EPIC-6 M1 仅 FR-501~505 草稿 MVP
- **状态**：已采纳
- **决策者**：技术总监
- **背景**：EPIC-6 全量含 Astro 发布、A/B、托管；DDL `landing_page` 已就绪；EPIC-4 内容可链 `landing_page_suggestion`
- **决策**：
  - **M1 做**：Python `/ai/landing/generate`；Java `landing_page` CRUD + generate；Admin 列表 + JSON/SEO 预览；status 保持 `DRAFT`
  - **M1 模板**：5 类 template_type（destination/route/theme/visa/event）；`content_json` 按 PRD §20.3 最小模块集
  - **M1 不做**：FR-506 HTML 导出、FR-507 Astro 公网发布、Turnstile 公开表单（→ EPIC-7）、GrapesJS 编辑
  - 同步 Feign 调 AI；`needs_human_review` 默认 true
- **影响**：[EPIC-6 M1 Sprint](HANDOFFS/2026-07-01-tech-director-epic6-m1-landing-sprint.md)；`inbound-landing` Astro **M2**

### ADR-20260701-14 | EPIC-7 M1 仅 FR-601 公开表单 + Admin 列表
- **状态**：已采纳
- **决策者**：技术总监
- **背景**：EPIC-7 全量含 CRM/归因/广告；EPIC-6 已生成 `form_config_json`；闭环需 lead 落库
- **决策**：
  - **M1 做**：Java `POST /api/v1/public/leads` + Admin `GET .../leads` 列表/详情；status 默认 NEW
  - **M1 Turnstile**：配置占位；本地 dev 可 skip
  - **M1 不做**：FR-602~607、Python 跟进 AI、`inbound-landing` 公网页（与 EPIC-6 M2 合并排）
- **影响**：[EPIC-7 M1 Sprint](HANDOFFS/2026-07-01-tech-director-epic7-m1-leads-sprint.md)

### ADR-20260702-15 | EPIC-8 M1 仅 FR-701/702 报告列表 + 手动周报
- **状态**：已采纳
- **决策者**：技术总监
- **背景**：FR-106 已实现诊断导出并写 `report` 表，但无报告中心 UI；PRD FR-702 周报是 MVP 交付物
- **决策**：
  - **M1 做**：`GET .../reports` 列表/详情；`POST .../reports/weekly` Java 聚合（诊断/关键词/内容/落地页/线索）；DOCX/PDF 导出；Admin `/reports`
  - **M1 复用**：`DiagnosticReportExportServiceImpl` 渲染/Gotenberg；DIAGNOSTIC 行按 summary.runId 再导出
  - **M1 不做**：FR-703 月报、FR-704 白标模板、FR-705 推送、XXL-Job 定时、MinIO `file_url`、LLM 周报摘要
  - 周报建议：3 条静态模板（按 KPI 阈值选文案）
- **影响**：[EPIC-8 M1 Sprint](HANDOFFS/2026-07-02-tech-director-epic8-m1-reports-sprint.md)；`ruoyi-diagnostic` ReportController

### ADR-20260703-16 | EPIC-6 M2 Astro 预览 + Turnstile 表单闭环
- **状态**：已采纳
- **决策者**：技术总监 + 用户（2026-07-03）
- **背景**：M1 仅 DRAFT + Admin JSON 预览；EPIC-7 已有 `POST /api/v1/public/leads`；闭环缺公网页与发布
- **决策**：
  - **M2 做**：`inbound-landing` Astro 4 · 路径 `/p/{projectId}/{slug}` · Java public GET + publish/unpublish · Admin 发布按钮 · Turnstile widget → public leads
  - **公网 API**：`GET /api/v1/public/landing-pages/{slug}?projectId=` 仅 `PUBLISHED`；`published_url` = `{landing.public-base-url}/p/{projectId}/{slug}`
  - **Turnstile**：有 `secret-key` 时 Java 真 siteverify；无 Key 本地 skip（与 M1 兼容）
  - **M2 不做**：GrapesJS/Puck 编辑、HTML 文件导出 FR-506、自定义域名、A/B FR-508、PostHog（→ M3）
  - **CORS**：Java 允许 landing origin 访问 `/api/v1/public/**`
- **影响**：[EPIC-6 M2 Sprint](HANDOFFS/2026-07-03-tech-director-epic6-m2-landing-publish-sprint.md)

### ADR-20260704-17 | EPIC-9 M1 仅 FR-804 额度查询 + 超额拦截
- **状态**：已采纳
- **决策者**：技术总监
- **背景**：V1.0 商业化需套餐额度；DDL `subscription` + demo seed 已就绪；各 EPIC 生成 API 无扣费
- **决策**：
  - **M1 做**：`GET` 当前订阅 · `QuotaService` 在 6 个入口 `checkAndConsume` · Admin 只读用量页 · HTTP 402
  - **M1 存储**：PG `used_json` 同事务更新（不用 Redis）
  - **M1 不做**：支付、套餐 CRUD、FR-802 模型配置、FR-806 审计、周期自动重置 Job
  - 无 ACTIVE 订阅：dev 默认放行；staging 可 seed TRIAL
- **影响**：[EPIC-9 M1 Sprint](HANDOFFS/2026-07-04-tech-director-epic9-m1-billing-sprint.md)

### ADR-20260705-18 | EPIC-11 M1 仅 FR-112~114 扩展探针 poll 闭环
- **状态**：已采纳
- **决策者**：技术总监
- **背景**：grounded-api 已跑通；Admin 创建诊断可选 `browser-extension` 但无调度/API；`probe_node`/`platform_adapter` DDL 已就绪；扩展目录待 scaffold
- **决策**：
  - **M1 做**：Java `ProbeController`（register/poll/result/adapters）+ `createRun` 按 probe_mode 分叉 · Plasmo 扩展 1 平台（`perplexity`）· Admin `/settings/probe-nodes` 只读列表
  - **M1 调度**：HTTP poll 分配 PENDING 任务（不用 MQ `diag.probe-extension`）
  - **M1 鉴权**：`X-Probe-Node-Key` + `@SaIgnore`（dev 白名单 node_key）
  - **M1 不做**：FR-115 校准 · FR-116 adapter 后台 CRUD · FR-117 截图 · FR-118 Headless · 国内平台 adapter
  - 扩展结果 Java 侧写 `diagnostic_result`；M1 可简化 citation 解析，后续接 `/ai/parse-citations`
- **影响**：[EPIC-11 M1 Sprint](HANDOFFS/2026-07-05-tech-director-epic11-m1-probe-sprint.md)；`inbound-probe-extension` Plasmo

### ADR-20260706-19 | EPIC-3 M2 仅 FR-203 关键词机会评分
- **状态**：已采纳
- **决策者**：技术总监
- **背景**：M1 generate+列表已完成；`score`/`score_detail_json` 占位；Admin 列表已预留机会分列；FR-203 为 P0
- **决策**：
  - **M2 做**：Python `/ai/keywords/score` · Java 单条+批量 refresh · Admin 真实分数+排序+刷新按钮
  - **M2 五维权重**（`keyword_score_v1`，存 template 表）：相关性 30% · 长尾价值 20% · 可生产性 20% · 落地页价值 15% · 竞品强度 15%
  - **竞品强度输入**：可选最近 SUCCESS `diagnostic_run.geo_score` 反算（无诊断时用 LLM 估计）
  - **M2 不做**：generate 后自动评分 · 权重运营 UI · 词库编辑/导出 · 额度单独计费（score 调用暂不占 quota）
  - mock：`KEYWORD_SCORE_MOCK_LLM=true` deterministic
- **影响**：[EPIC-3 M2 Sprint](HANDOFFS/2026-07-06-tech-director-epic3-m2-keyword-score-sprint.md)

### ADR-20260707-20 | EPIC-7 M2 仅 FR-605 轻量 CRM
- **状态**：已采纳
- **决策者**：技术总监（完整版交付排期）
- **背景**：M1 公开表单+只读列表已完成；落地页→询盘闭环缺销售侧操作；DDL `lead_followup` + `lead_status` 已就绪
- **决策**：
  - **M2 做**：状态机五态流转 · `lead_followup` 增删查 · assignee_id · Admin drawer 可操作
  - **M2 状态规则**：NEW→FOLLOWING→QUOTED→WON；任意非终态→LOST；WON/LOST 锁定
  - **M2 不做**：FR-602 WhatsApp 追踪 · FR-603 AI 跟进 · FR-606 归因报表 · FR-607 广告 · CSV 导出
  - 负责人 M2：指派当前登录用户或手动 assigneeId（无完整成员管理 UI）
- **影响**：[EPIC-7 M2 Sprint](HANDOFFS/2026-07-07-tech-director-epic7-m2-crm-sprint.md)

---

## 待讨论

- （暂无）
