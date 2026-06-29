# 技术栈组件清单（细粒度版）

>  companion 文档：`ARCHITECTURE.md`  
> 用途：AI Coding 时精确到「装什么、什么版本、怎么配、替换成什么」

---

## 1. 组件总表（MVP 基线）

| # | 层 | 组件 | 版本 | 部署方式 | 端口 | License |
|---|-----|------|------|----------|------|---------|
| 1 | L1 | PostgreSQL + pgvector | 16 + 0.7 | Docker | 5432 | PostgreSQL |
| 2 | L1 | Redis | 7.2 | Docker | 6379 | BSD |
| 3 | L1 | MinIO | RELEASE.2024+ | Docker | 9000/9001 | AGPL |
| 4 | L1 | RabbitMQ | 3.13 | Docker | 5672/15672 | MPL |
| 5 | L0 | XXL-Job Admin | 2.4.1 | Docker | 8088 | GPL |
| 6 | L0 | Langfuse | 2.x | Docker | 3000 | MIT |
| 7 | L2 | Nginx | 1.25 | Docker | 80/443 | BSD |
| 8 | L3 | OpenJDK | 21 LTS | 内嵌 | — | GPL+CE |
| 9 | L3 | Spring Boot | 3.3.x | 内嵌 | 8080 | Apache-2.0 |
| 10 | L3 | MyBatis-Plus | 3.5.x | Maven | — | Apache-2.0 |
| 11 | L3 | Spring Security | 6.x | Maven | — | Apache-2.0 |
| 12 | L3 | Casbin | jCasbin 1.x | Maven | — | Apache-2.0 |
| 13 | L3 | Flyway | 10.x | Maven | — | Apache-2.0 |
| 14 | L3 | SpringDoc OpenAPI | 2.x | Maven | — | Apache-2.0 |
| 15 | L4 | Python | 3.11+ | 内嵌 | — | PSF |
| 16 | L4 | FastAPI | 0.110+ | pip | 8090 | MIT |
| 17 | L4 | Uvicorn | 0.27+ | pip | — | BSD |
| 18 | L4 | LiteLLM | 1.x | pip | — | MIT |
| 19 | L4 | LangGraph | 0.2+ | pip | — | MIT |
| 20 | L4 | LlamaIndex | 0.10+ | pip | — | MIT |
| 21 | L4 | Docling | latest | pip | — | MIT |
| 22 | L4 | sentence-transformers | 2.x | pip | — | Apache-2.0 |
| 23 | L4 | asyncpg | 0.29+ | pip | — | Apache-2.0 |
| 24 | L5 | Vue | 3.4+ | npm | 5173 | MIT |
| 25 | L5 | Element Plus | 2.x | npm | — | MIT |
| 26 | L5 | Vite | 5.x | npm | — | MIT |
| 27 | L5 | Pinia | 2.x | npm | — | MIT |
| 28 | L5 | TipTap | 2.x | npm | — | MIT |
| 29 | L5 | Astro | 4.x | npm | 4321 | MIT |
| 30 | L5 | GrapesJS | 0.21+ | npm | — | BSD |
| 31 | L6 | Plasmo | 0.88+ | npm | — | MIT |
| 32 | L6 | Playwright | 1.42+ | pip | — | Apache-2.0 |
| 33 | L7 | Prometheus | 2.x | Docker | 9090 | Apache-2.0 |
| 34 | L7 | Grafana | 10.x | Docker | 3001 | AGPL |
| 35 | L7 | Loki | 2.x | Docker | 3100 | AGPL |
| 36 | L7 | PostHog | 1.x | Docker | 8089 | MIT |
| 37 | 报告 | XDocReport | 2.x | Maven | — | MIT |
| 38 | 报告 | Gotenberg | 8.x | Docker | 3002 | MIT |
| 39 | 抓取 | Firecrawl | self-host | Docker | 3010 | AGPL |
| 40 | 视频 | PySceneDetect | 0.6+ | pip | — | BSD |
| 41 | 视频 | ffmpeg | 6.x | apt/brew | — | LGPL |
| 42 | 防刷 | Cloudflare Turnstile | SaaS | — | — | 免费层 |
| 43 | 重排 | bge-reranker-v2-m3 | HF | 本地/GPU | — | Apache-2.0 |

---

## 2. L3 Java 依赖 BOM（`inbound-core/pom.xml` 摘录）

```xml
<properties>
  <java.version>21</java.version>
  <spring-boot.version>3.3.5</spring-boot.version>
  <mybatis-plus.version>3.5.7</mybatis-plus.version>
  <casbin.version>1.55.0</casbin.version>
  <flyway.version>10.10.0</flyway.version>
  <minio.version>8.5.11</minio.version>
  <mapstruct.version>1.5.5.Final</mapstruct.version>
</properties>

<dependencies>
  <!-- Web + Validation -->
  <dependency>spring-boot-starter-web</dependency>
  <dependency>spring-boot-starter-validation</dependency>
  <!-- Security -->
  <dependency>spring-boot-starter-security</dependency>
  <dependency>org.casbin:jcasbin</dependency>
  <dependency>io.jsonwebtoken:jjwt-api:0.12.5</dependency>
  <!-- DB -->
  <dependency>org.postgresql:postgresql</dependency>
  <dependency>com.baomidou:mybatis-plus-boot-starter</dependency>
  <dependency>org.flywaydb:flyway-core</dependency>
  <!-- Cache + MQ -->
  <dependency>spring-boot-starter-data-redis</dependency>
  <dependency>spring-boot-starter-amqp</dependency>
  <!-- OSS -->
  <dependency>io.minio:minio</dependency>
  <!-- OpenAPI -->
  <dependency>org.springdoc:springdoc-openapi-starter-webmvc-ui</dependency>
  <!-- Report -->
  <dependency>fr.opensagres.xdocreport:... </dependency>
</dependencies>
```

---

## 3. L4 Python 依赖（`inbound-ai/pyproject.toml` 摘录）

```toml
[project]
name = "inbound-ai"
requires-python = ">=3.11"
dependencies = [
  "fastapi>=0.110",
  "uvicorn[standard]>=0.27",
  "litellm>=1.40",
  "langgraph>=0.2",
  "llama-index>=0.10",
  "llama-index-vector-stores-postgres>=0.2",
  "docling>=2.0",
  "asyncpg>=0.29",
  "aio-pika>=9.4",          # RabbitMQ
  "httpx>=0.27",
  "langfuse>=2.0",
  "minio>=7.2",
  "pydantic-settings>=2.2",
  "sentence-transformers>=2.7",
  "scenedetect[opencv]>=0.6",
]
```

---

## 4. 环境变量清单

### 4.1 `inbound-core`（Java）

| 变量 | 示例 | 说明 |
|------|------|------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/inbound_growth` | 主库 |
| `SPRING_DATASOURCE_USERNAME` | `inbound` | — |
| `SPRING_DATASOURCE_PASSWORD` | `***` | — |
| `SPRING_REDIS_HOST` | `redis` | — |
| `SPRING_RABBITMQ_HOST` | `rabbitmq` | — |
| `MINIO_ENDPOINT` | `http://minio:9000` | — |
| `MINIO_ACCESS_KEY` | `minioadmin` | — |
| `MINIO_SECRET_KEY` | `***` | — |
| `JWT_PRIVATE_KEY_PATH` | `/secrets/jwt.pem` | RS256 私钥 |
| `JWT_PUBLIC_KEY_PATH` | `/secrets/jwt.pub` | RS256 公钥 |
| `AI_SERVICE_BASE_URL` | `http://ai-api:8090` | 内网 AI 地址 |
| `AI_SERVICE_INTERNAL_TOKEN` | `***` | 服务间鉴权 |
| `XXL_JOB_ADMIN_ADDRESSES` | `http://xxl-job:8088/xxl-job-admin` | — |

### 4.2 `inbound-ai`（Python）

| 变量 | 示例 | 说明 |
|------|------|------|
| `DATABASE_URL` | `postgresql+asyncpg://inbound:***@postgres:5432/inbound_growth` | — |
| `REDIS_URL` | `redis://redis:6379/1` | — |
| `RABBITMQ_URL` | `amqp://guest:guest@rabbitmq:5672/` | — |
| `MINIO_ENDPOINT` | `http://minio:9000` | — |
| `OPENAI_API_KEY` | `sk-***` | LiteLLM 读取 |
| `GEMINI_API_KEY` | `***` | — |
| `PERPLEXITY_API_KEY` | `pplx-***` | GEO 诊断主力 |
| `LANGFUSE_PUBLIC_KEY` | `pk-***` | — |
| `LANGFUSE_SECRET_KEY` | `sk-***` | — |
| `LANGFUSE_HOST` | `http://langfuse:3000` | — |
| `EMBEDDING_MODEL` | `openai/text-embedding-3-small` | 1536 维 |
| `RERANKER_MODEL` | `BAAI/bge-reranker-v2-m3` | 本地推理 |
| `CORE_CALLBACK_BASE_URL` | `http://core-api:8080` | Worker 回调 Java；**生产同 compose 网**用服务名。混合联调本机 worker 用 `http://localhost:8080` |
| `DIAGNOSE_WORKER_ENABLED` | `true` / `false` | 是否消费 `diag.grounded-api`；混合阶段本机 `true`、服务器 ai-api 默认 `false` |

### 4.3 `inbound-admin`（Vue）

| 变量 | 示例 | 说明 |
|------|------|------|
| `VITE_API_BASE_URL` | `/api/v1` | 经 Nginx 反代 |
| `VITE_TURNSTILE_SITE_KEY` | `0x***` | 公开表单用 |

---

## 5. 网络与端口矩阵

| 服务 | 容器名 | 内部端口 | 宿主机端口 | 暴露范围 |
|------|--------|----------|------------|----------|
| postgres | inbound-postgres | 5432 | 5432 | 内网 |
| redis | inbound-redis | 6379 | 6379 | 内网 |
| minio | inbound-minio | 9000/9001 | 9000/9001 | 内网 |
| rabbitmq | inbound-rabbitmq | 5672/15672 | 5672/15672 | 内网 |
| core-api | inbound-core | 8080 | 8080 | 内网 |
| ai-api | inbound-ai | 8090 | 8090 | 内网 |
| admin-web | inbound-admin | 5173 | 5173 | 内网 |
| nginx | inbound-nginx | 80/443 | 80/443 | **公网** |
| langfuse | inbound-langfuse | 3000 | 3000 | 内网 |
| xxl-job | inbound-xxl-job | 8088 | 8088 | 内网 |
| gotenberg | inbound-gotenberg | 3000 | 3002 | 内网 |

**安全规则**：仅 Nginx 443 对公网；MinIO/RabbitMQ/Postgres 不对公网暴露。

---

## 6. 分层职责 — 逐层「谁做什么、不做什么」

### L5 客户端

| 组件 | 做什么 | 不做什么 |
|------|--------|----------|
| Admin Web | 表单/列表/任务触发/报告预览 | 不调 LLM；不存向量 |
| Astro 落地页 | SSG 渲染 + 表单 POST | 不含 Admin 鉴权逻辑 |
| Plasmo 扩展 | 拉探针任务、DOM 采集 | 不存业务数据；最小权限 |

### L2 网关

| 做什么 | 不做什么 |
|--------|----------|
| JWT 校验、租户头注入、限流、路由 | 业务逻辑、LLM 调用 |

### L3 Java

| 做什么 | 不做什么 |
|--------|----------|
| CRUD、状态机、计费扣额、MQ 投递、报告模板渲染 | Embedding、Prompt 拼装、GEO 解析 |

### L4 Python

| 做什么 | 不做什么 |
|--------|----------|
| LLM/RAG/评分/拆帧/解析 | 用户权限、套餐计费、线索状态流转 |

---

## 7. 底座选型决策树

```
需要 GEO 探针 + 多租户 SaaS + Agent 编排？
├─ 是 → 排除 Supabase/Dify 全栈底座
│       ├─ 团队熟悉 Java？
│       │   ├─ 是 → Spring Boot 3 模块化单体 + FastAPI AI 微服务 ✅
│       │   └─ 否 → 可考虑 NestJS 替代 Java（需重写 PRD 基线，不推荐）
│       ├─ MVP 要快？
│       │   ├─ 是 → Docker Compose + PG 单库 + pgvector ✅
│       │   └─ 否 → 直接 K8s（过度，不推荐 MVP）
│       └─ CRUD 要加速？
│           ├─ 是 → 借 RuoYi 仅 IAM/代码生成，业务表自建 ✅
│           └─ 否 → 从零 Spring Boot 脚手架
└─ 否 → 与本项目无关
```

---

## 8. 组件替换矩阵（规模化时）

| 当前（MVP） | 触发条件 | 替换为 |
|-------------|----------|--------|
| pgvector | 向量 > 500 万 / QPS > 200 | Qdrant 或 Milvus |
| RabbitMQ | 吞吐 > 1 万 msg/s | Kafka |
| 模块化单体 | 团队 > 8 人 / 发布冲突 | 拆 9 微服务 |
| XXL-Job | 需 Saga/补偿/长事务 | Temporal |
| MinIO 单机 | 存储 > 10TB | 阿里云 OSS / AWS S3 |
| bge-reranker 本地 | GPU 不足 | Jina Reranker API |
| LiteLLM | 并发 > 5000 RPS | Bifrost (Go) |

---

## 9. Maven / npm 子项目版本锁定策略

| 生态 | 锁定方式 |
|------|----------|
| Java | `spring-boot-starter-parent` BOM + `<dependencyManagement>` |
| Python | `uv lock` 或 `poetry.lock` |
| Node | `pnpm-lock.yaml` + `engines.node: ">=20"` |

---

## 10. 与 ARCHITECTURE.md 的分工

| 文档 | 侧重 |
|------|------|
| [README.md](../README.md) | 项目入口、结构总览、快速开始 |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 分层、服务职责、数据流、部署拓扑、EPIC 映射 |
| **本文档** | 组件版本、端口、环境变量、依赖 BOM、替换矩阵 |
| [../database/README.md](../database/README.md) | DDL 用法、表域分组、迁移策略 |
| [../deploy/README.md](../deploy/README.md) | Compose 启动、服务端口、联调说明 |
| `database/ddl/*.sql` | 表结构 |
| `deploy/docker-compose.yml` | 可运行基础设施 |

---

*变更组件版本时请同步更新 `deploy/docker-compose.yml` 与本表。*
