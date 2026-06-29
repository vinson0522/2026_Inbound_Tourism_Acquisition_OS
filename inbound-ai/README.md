# inbound-ai



Python FastAPI AI 微服务（EPIC-10 / EPIC-2）。



## 职责



LiteLLM 网关、LangGraph Agent、RAG（LlamaIndex + pgvector）、GEO 诊断采样与 citations 解析、评分、Docling 向量化、视频拆帧 — **不含权限与计费**。



## 状态（2026-06-26）



✅ Phase 1：FastAPI scaffold + 内网鉴权 + LiteLLM gateway + `/health` + Dockerfile  

✅ EPIC-2 M1：多供应商 key 路由 + `/ai/diagnose` + `/ai/parse-citations` + `/ai/score` + RabbitMQ worker



## 目录结构



```

inbound-ai/

├── app/

│   ├── main.py

│   ├── config.py

│   ├── deps.py

│   ├── routers/       # health, llm, diagnose

│   ├── services/      # llm_gateway, llm_provider, citation_parser, scorer, diagnose_service

│   ├── workers/       # diagnose_worker (diag.grounded-api)

│   └── models/

├── tests/

├── pyproject.toml

├── Dockerfile

└── .env.example

```



## 本地启动



```powershell

cd inbound-ai

copy .env.example .env

# 编辑 .env：AI_SERVICE_INTERNAL_TOKEN 与 Java 侧一致

# EPIC-2：PERPLEXITY_API_KEY 必填（grounded-api 主力）



uv sync --extra dev

uv run pytest tests/ -q



$env:AI_SERVICE_INTERNAL_TOKEN="dev_internal_token_change_me"

uv run uvicorn app.main:app --reload --port 8090

```



## RabbitMQ Worker（可选）



与 uvicorn 同进程启动（FastAPI lifespan），需设置：



```powershell

$env:RABBITMQ_URL="amqp://inbound:inbound_dev_pass@127.0.0.1:5672/"

$env:CORE_CALLBACK_BASE_URL="http://localhost:8080"

$env:DIAGNOSE_WORKER_ENABLED="true"

uv run uvicorn app.main:app --port 8090

```



- 队列：`diag.grounded-api`（失败 ≥3 次 → `diag.grounded-api.dlq`）

- 消费 MQ → 调 diagnose 逻辑 → POST Java `/api/v1/internal/diagnostics/probe-callback`



## API



| 方法 | 路径 | 鉴权 | 说明 |

|------|------|------|------|

| GET | `/health` | 无 | 公开健康检查 |

| GET | `/ai/health` | Bearer / `X-Internal-Token` | 内网健康 + LiteLLM 状态 |

| POST | `/ai/llm/complete` | 同上 | LiteLLM 最小 completion |

| POST | `/ai/diagnose` | 同上 | GEO grounded-api 探针（Perplexity 主力） |

| POST | `/ai/parse-citations` | 同上 | 统一 citations 解析（M1：Perplexity） |

| POST | `/ai/score` | 同上 | GEO 分数聚合（权重来自请求 JSON） |



**铁律**：`probe_mode=grounded-api` 且 `grounding_enabled=false` → HTTP 400。



**多供应商 Key 路由**：`openai/*` → `OPENAI_API_KEY`；`gemini/*` → `GEMINI_API_KEY`；`perplexity/*` → `PERPLEXITY_API_KEY`。



## curl 示例（diagnose）



```powershell

$env:AI_SERVICE_INTERNAL_TOKEN="dev_internal_token_change_me"

$env:PERPLEXITY_API_KEY="pplx-..."

curl -X POST http://localhost:8090/ai/diagnose `

  -H "Authorization: Bearer $env:AI_SERVICE_INTERNAL_TOKEN" `

  -H "Content-Type: application/json" `

  -d '{"run_id":1,"question_id":1,"tenant_id":1,"project_id":1,"platform":"perplexity","probe_mode":"grounded-api","grounding_enabled":true,"question":"Can you recommend China travel agencies?","model":"perplexity/sonar-pro","region":"us-east","locale":"en-US","sample_index":0}'

```



## Docker



```powershell

docker build -t inbound-ai:local inbound-ai

docker run --rm -p 8090:8090 -e AI_SERVICE_INTERNAL_TOKEN=dev_internal_token_change_me inbound-ai:local

```



## 参考



- `docs/ARCHITECTURE.md` §7

- `docs/TECH_STACK_COMPONENTS.md` §3–§4.2

- `docs/agent-team/HANDOFFS/2026-06-26-tech-director-to-dev-ai-epic2-diagnose.md`

- `docs/agent-team/DECISIONS.md` ADR-20260626-07

