# CLAUDE.md — 入境游海外获客增长 Agent

> 本文件为 **Claude / Cursor / 其他 AI 编程助手** 的项目上下文基线。  
> 开始编码前请先读本文，再按需深入 `docs/` 与 `PRD_商业化版_V2.0.md`。

---

## 1. 项目是什么

**产品名**：Inbound AI Growth Agent / 旅获 AI / TourGEO Agent

**一句话**：面向中国入境游企业的 AI 海外获客增长系统 —— GEO 可见率诊断、海外关键词洞察、社媒内容 Agent、英文落地页生成、询盘转化与周报监控。

**核心闭环**：

```
客户资料 → GEO 诊断 → 关键词机会 → 内容脚本 → 落地页 → 询盘 → 周报 → 优化
```

**产品不是什么**：

- 不是通用 AI 聊天机器人
- 不是 Prompt 工具箱
- 不承诺「保证 AI 推荐排名」
- 不是素材搬运/侵权工具

**真正壁垒（必须自建）**：行业问题库、八阶段词库、GEO 评分规则、Prompt 模板体系、交付 SOP。

**基础设施（必须用开源，禁止手搓轮子）**：LiteLLM、LlamaIndex、Docling、LangGraph、pgvector、Plasmo、XDocReport、Gotenberg 等 —— 见 `docs/TECH_STACK_COMPONENTS.md`。

---

## 2. 文档地图（按优先级）

| 优先级 | 文档 | 用途 |
|:------:|------|------|
| P0 | `PRD_商业化版_V2.0.md` | 功能需求 FR、业务流程、数据模型、API、GEO 探针、验收标准 |
| P0 | `docs/ARCHITECTURE.md` | 七层架构、服务职责、数据流、Monorepo 结构、EPIC 映射 |
| P0 | `docs/TECH_STACK_COMPONENTS.md` | 组件版本、端口、环境变量、依赖 BOM、替换矩阵 |
| P1 | `database/ddl/001_schema.sql` | PostgreSQL + pgvector 全量 DDL（28 表） |
| P1 | `database/ddl/002_seed_demo.sql` | 演示种子数据 |
| P1 | `deploy/docker-compose.yml` | MVP 基础设施一键启动 |
| P2 | `AGENT.md` | AI Agent 编码工作流、分层边界、反模式 |
| P2 | `.cursor/rules/*.mdc` | Cursor 专项规则（含 QingTian MCP 保活，勿与业务逻辑冲突） |

**变更同步规则**：改表结构 → 同步 `001_schema.sql` + PRD §11 + ARCHITECTURE；改组件版本 → 同步 `TECH_STACK_COMPONENTS.md` + `docker-compose.yml`。

---

## 3. 仓库结构（Monorepo 目标态）

```
2026_Inbound_Tourism_Acquisition_OS/
├── CLAUDE.md                    # 本文件
├── AGENT.md                     # Agent 编码指南
├── PRD_商业化版_V2.0.md
├── docs/
│   ├── ARCHITECTURE.md
│   └── TECH_STACK_COMPONENTS.md
├── database/ddl/
├── deploy/
├── inbound-core/                # Java Spring Boot 3 — 业务后端
├── inbound-ai/                  # Python FastAPI — AI 微服务
├── inbound-admin/               # Vue3 + Element Plus — 管理后台
├── inbound-landing/             # Astro — 落地页 SSG
└── inbound-probe-extension/     # Plasmo — Chrome MV3 GEO 探针
```

> 部分子项目目录可能尚未 scaffold，按 EPIC 顺序逐步创建，勿一次性空壳全建。

---

## 4. 技术栈摘要

| 层 | 技术 | 端口 |
|----|------|------|
| L3 业务 | Java 21 + Spring Boot 3.3 + MyBatis-Plus + Casbin | 8080 |
| L4 AI | Python 3.11+ + FastAPI + LiteLLM + LangGraph + LlamaIndex | 8090 |
| L5 Admin | Vue 3.4 + TS + Vite + Element Plus + Pinia | 5173 |
| L5 落地页 | Astro 4 + Turnstile | 4321 |
| L6 探针 | Plasmo (MV3) + Playwright 兜底 | — |
| L1 数据 | PostgreSQL 16 + pgvector | 5432 |
| 缓存/队列/存储 | Redis 7 / RabbitMQ 3 / MinIO | 6379 / 5672 / 9000 |

**数据库**：MVP 统一 PostgreSQL（业务 + 向量），**不用 MySQL**。DDL 以 `001_schema.sql` 为准。

---

## 5. 架构铁律（违反即 Bug）

### 5.1 分层职责

| 层 | 做什么 | 不做什么 |
|----|--------|----------|
| **Java (inbound-core)** | CRUD、状态机、计费扣额、MQ 投递、权限、报告模板渲染 | Embedding、Prompt 拼装、GEO 解析、LLM 调用 |
| **Python (inbound-ai)** | LLM/RAG/评分/拆帧/citations 解析、LangGraph Agent | 用户权限、套餐计费、线索状态流转 |
| **Admin (inbound-admin)** | 表单/列表/任务触发/报告预览 | 直连 LLM；不存向量 |
| **探针扩展** | 拉任务、网页提问、hook 抓取、上报 | 不存业务数据；最小权限 |

### 5.2 多租户

- 所有业务表含 `tenant_id`
- Java：MyBatis-Plus `TenantLineHandler` 强制过滤
- Python：RAG 检索必须带 `tenant_id` + `project_id`
- MinIO 路径：`{tenant_id}/{project_id}/...`
- 禁止写跨租户查询或无 tenant 条件的 DELETE

### 5.3 GEO 诊断必须 Grounded

- **禁止**用裸 Chat Completions（不联网）结果充当 GEO 分数
- `probe_mode=grounded-api` 时，`grounding_enabled` 必须为 `true`，否则拒绝执行
- GEO 诊断 **不缓存** LLM 响应
- 三种探针模式：`grounded-api`（默认主力）| `browser-extension`（国内+校准）| `headless-automation`（兜底）
- 详见 PRD §7.2、§7.6

### 5.4 AI 生成内容

- 生成结果默认 `needs_human_review=true`
- 必须 RAG 引用客户知识库，标注 `chunk_id` 来源
- 价格/签证/政策类字段加「需人工确认」标记
- 对外导出前显示版权/合规提醒

---

## 6. 统一约定

### 6.1 API

- 前缀：`/api/v1`（Admin → Java）；`/ai/**`（Java → Python，内网 Token）
- 响应：`{ "code": 0, "message": "ok", "data": {}, "trace_id": "..." }`
- 长任务：创建 → 异步 MQ → 轮询/WebSocket/callback
- 公开端点：`/api/v1/public/**`（Turnstile + 限流）

### 6.2 数据库

- 主键：`BIGSERIAL`
- 时间：`created_at` / `updated_at` / 软删 `deleted_at`
- JSON 字段：`JSONB`
- 向量：`vector(1536)` via pgvector
- 迁移：Flyway（Java 侧统一管理）

### 6.3 代码风格

| 生态 | 约定 |
|------|------|
| Java | 包结构 `inbound-api/application/domain/infrastructure/bootstrap`；MapStruct 做 DTO 转换；Lombok 适度使用 |
| Python | `app/routers/services/agents/workers/models`；Pydantic v2；类型注解完整 |
| Vue | `<script setup lang="ts">`；API 封装在 `src/api/`；Pinia 管全局状态 |
| SQL | snake_case 表名/列名；枚举用 PG ENUM 或 CHECK |

### 6.4 Git

- **仅在用户明确要求时 commit**
- 不 force push main/master
- 不提交 `.env`、API Key、JWT 私钥
- commit message 聚焦「为什么」

---

## 7. 本地开发

### 7.1 启动基础设施

```bash
cd deploy
docker compose up -d
```

验证：Postgres `5432`、Redis `6379`、MinIO `9000`、RabbitMQ `15672`。

### 7.2 环境变量

见 `docs/TECH_STACK_COMPONENTS.md` §4：

- Java：`SPRING_DATASOURCE_URL`、`AI_SERVICE_BASE_URL`、`JWT_*`、`MINIO_*`
- Python：`DATABASE_URL`、`PERPLEXITY_API_KEY`、`GEMINI_API_KEY`、`OPENAI_API_KEY`、`LANGFUSE_*`
- Admin：`VITE_API_BASE_URL`

### 7.3 常用命令（目标态，子项目 scaffold 后）

```bash
# Java
cd inbound-core && ./mvnw spring-boot:run

# Python
cd inbound-ai && uv run uvicorn app.main:app --reload --port 8090

# Admin
cd inbound-admin && pnpm dev

# DDL 手动执行
psql -U inbound -d inbound_growth -f database/ddl/001_schema.sql
```

---

## 8. EPIC 实施顺序

按依赖关系实施，**不要跳 EPIC**：

| 顺序 | EPIC | 说明 |
|:----:|------|------|
| 1 | EPIC-1 基础平台 | 租户/权限/项目/知识库上传 |
| 2 | EPIC-10 AI 编排 | LiteLLM 网关 + embed + RAG 基础 |
| 3 | EPIC-2 GEO 诊断 | grounded-api 探针 + 评分 + 报告 |
| 4 | EPIC-3 关键词 | 机会词 + 八阶段词库 |
| 5 | EPIC-4 内容 Agent | 脚本/分镜生成 |
| 6 | EPIC-6 落地页 | 草稿 + 表单 |
| 7 | EPIC-7 线索 | 公开表单 + 归因 |
| 8 | EPIC-8 报告 | DOCX/PDF 导出 |
| 9 | EPIC-9 计费 | 套餐额度 |
| 10 | EPIC-11 浏览器探针 | Plasmo 扩展 + Adapter |
| 11 | EPIC-5 爆款素材 | 拆帧/七维拆解（可后置） |

---

## 9. 关键业务域速查

### 9.1 一级模块（9 个）

工作台 · 客户项目 · GEO 诊断 · 关键词洞察 · 内容 Agent · 落地页 Agent · 线索与转化 · 报告中心 · 系统设置

### 9.2 GEO 评分（PRD §10）

```
GEO_Score = 100 × (0.25×品牌出现率 + 0.20×Top3率 + 0.15×(1-竞品压制) + 0.15×引用覆盖 + 0.15×长尾覆盖 + 0.10×资产完整度)
```

权重从 `scoring_rule.metric_weights_json` 读取，**禁止硬编码**。

### 9.3 用户生命周期八阶段

灵感 → 种草 → 比较 → 签证 → 规划 → 信任 → 决策 → 复购

---

## 10. 禁止事项（AI 编码时）

- ❌ 自研 LLM 网关 / 向量检索 / 文档解析 / 报告 PDF 引擎
- ❌ 在 Java 里直接调 OpenAI/Perplexity SDK
- ❌ 在 Python 里做 Casbin 权限或套餐扣费
- ❌ 用 MySQL 替代 PostgreSQL（MVP 阶段）
- ❌ 裸模型 API 做 GEO 诊断
- ❌ 硬编码 Prompt / 评分权重 / 平台 Adapter
- ❌ 扩展采集用户非任务对话内容
- ❌ 承诺「保证 AI 推荐」的文案或逻辑
- ❌ 无 scope 的大重构（用户没要求时不要动无关模块）
- ❌ 主动创建 commit（除非用户明确要求）

---

## 11. 合规要点

- 报告必须标注：`probe_mode`、`sampled_at`、`region`、参与平台
- 扩展：安装前明示授权；仅处理下发任务；截图脱敏
- 素材/拆解/导出：版权风险提示
- 线索：最小化采集；脱敏导出

---

## 12. 遇到不确定时

1. 先查 PRD 对应 FR 编号
2. 再查 ARCHITECTURE 对应服务模块
3. 数据字段以 `001_schema.sql` 为准
4. 组件选型以 `TECH_STACK_COMPONENTS.md` 为准
5. 仍不确定 → 问用户，**不要猜**

---

*Last updated: 2026-06-23 | 基线: PRD V2.0 + ARCHITECTURE V1.0*
