# HANDOFF | 技术总监 → 开发

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-06-25 |
| **优先级** | High |
| **关联** | EPIC-10 · ADR-20260625-06 · `CLAUDE.md` EPIC 顺序 #2 |

## 上下文

**当前状态**：EPIC-1 最小闭环 ✅（登录 → 建项目 → 列表）。`inbound-ai/` 仅有 README，**无代码**。共享服务器基础设施全绿；本机联调仍走 SSH 隧道（ADR-20260625-05）。

**本 Sprint 范围（EPIC-10 Phase 1 — Scaffold）**：
- FastAPI 应用骨架 + 内网鉴权 + LiteLLM 网关最小封装 + 健康检查 + Dockerfile
- **不含**：RabbitMQ worker、Docling embed、RAG 检索、GEO 诊断全链路（留 Phase 2 / EPIC-2）

**相关文件**：
- `inbound-ai/README.md` — 目标结构
- `docs/ARCHITECTURE.md` §7.2–§7.3 — 目录与 LiteLLM 规则
- `docs/TECH_STACK_COMPONENTS.md` §3–§4.2 — 依赖 BOM 与环境变量
- `deploy/docker-compose.yml` — `ai-api` 占位（8090）
- `inbound-core` — 未来通过 `AI_SERVICE_BASE_URL` + `AI_SERVICE_INTERNAL_TOKEN` 调用

**约束**（分层铁律）：
- Python **不做** Casbin、套餐扣费、线索状态机
- 所有 `/ai/**` 路由须 `Depends(verify_internal_token)`（Bearer 或 `X-Internal-Token`）
- `probe_mode=grounded-api` 时 `llm_gateway` **必须**校验 `grounding_enabled=true`，否则 `400`（见 ARCHITECTURE §7.3）
- 依赖以 `TECH_STACK_COMPONENTS.md` 为准；Phase 1 **不要**引入 sentence-transformers / scenedetect（减小镜像）
- API Key 走环境变量，**禁止**硬编码；提供 `inbound-ai/.env.example`

## 交付请求

**需要什么**：Scaffold `inbound-ai` 为可本地/隧道运行的 FastAPI 服务，暴露健康检查与 LiteLLM 最小调用，供 Java 后续 Feign 联调。

## 验收标准

### 1. 项目结构

```
inbound-ai/
├── app/
│   ├── main.py              # FastAPI + router 挂载
│   ├── config.py            # pydantic-settings Settings
│   ├── deps.py              # verify_internal_token
│   ├── routers/
│   │   ├── health.py        # GET /health, GET /ai/health
│   │   └── llm.py           # POST /ai/llm/complete（Phase 1 探针）
│   ├── services/
│   │   └── llm_gateway.py   # LiteLLM + grounding 校验
│   └── models/
│       └── llm.py           # Pydantic request/response
├── tests/
│   ├── test_health.py
│   └── test_llm_gateway.py  # grounding 拒绝用例
├── pyproject.toml
├── Dockerfile
├── .env.example
└── README.md（更新启动命令）
```

- [ ] `requires-python >= 3.11`；Phase 1 依赖：`fastapi`, `uvicorn`, `litellm`, `httpx`, `pydantic-settings`, `python-multipart`（**不含** langgraph/llama-index/docling 待 Phase 2）

### 2. 配置（`app/config.py`）

| 变量 | 必填 Phase 1 | 说明 |
|------|:------------:|------|
| `AI_SERVICE_INTERNAL_TOKEN` | ✅ | 与 Java `AI_SERVICE_INTERNAL_TOKEN` 一致 |
| `DATABASE_URL` | 可选 | Phase 1 可不连库；`/health` 可只报 `db: skipped` |
| `OPENAI_API_KEY` | 可选* | 无 key 时 `/ai/llm/complete` 返回明确 503 |
| `LANGFUSE_*` | 可选 | 有则 success_callback，无则跳过 |
| `PERPLEXITY_API_KEY` / `GEMINI_API_KEY` | 可选 | Phase 2 GEO 用 |

\* 有任一 LLM key 时须能完成 smoke completion。

### 3. API 端点

- [ ] `GET /health` — 公开，返回 `{ "status": "ok", "service": "inbound-ai", "version": "0.1.0" }`
- [ ] `GET /ai/health` — 需 internal token；返回 `{ "status": "ok", "litellm": "ready"|"no_key" }`
- [ ] `POST /ai/llm/complete` — 需 internal token

**Request 示例**：

```json
{
  "model": "openai/gpt-4o-mini",
  "messages": [{"role": "user", "content": "ping"}],
  "probe_mode": "chat",
  "grounding_enabled": false,
  "max_tokens": 16
}
```

**Grounded 校验**：
- `probe_mode=grounded-api` 且 `grounding_enabled=false` → HTTP 400，`ProbeConfigError` 文案
- `probe_mode=grounded-api` 且 `grounding_enabled=true` → 允许调用（Perplexity/Gemini 路由 Phase 1 可只 log model）

**Response**：统一 `{ "code": 0, "message": "ok", "data": { ... litellm result ... }, "trace_id": "<uuid>" }`

### 4. 测试

- [ ] `uv run pytest tests/ -q` 全部通过
- [ ] `test_llm_gateway` 覆盖 grounded-api + grounding=false 拒绝（mock LiteLLM，不耗 API）

### 5. 运行与打包

- [ ] 本地：`cd inbound-ai && uv sync && uv run uvicorn app.main:app --reload --port 8090`
- [ ] `Dockerfile` 多阶段或 slim Python 3.11；`EXPOSE 8090`；`CMD uvicorn app.main:app --host 0.0.0.0 --port 8090`
- [ ] `docker build -t inbound-ai:local inbound-ai` 成功

### 6. 联调脚本

- [ ] 新增 `deploy/scripts/test_ai_health.py`：`GET /health` + 带 token 的 `GET /ai/health`（`INBOUND_AI_BASE=http://localhost:8090`）

### 7. 文档

- [ ] 更新 `inbound-ai/README.md` 启动步骤
- [ ] 更新 `MEMORY.md` 开发章节 + 本文件 **Done** 段

## 隧道联调（本机无 Docker）

与 Java 相同：SSH 隧道后，8090 为本机 uvicorn 监听，**不需要**隧道映射 8090（除非服务跑在服务器）。

```powershell
# 终端 1：隧道（PG/Redis，AI 服务本机起）
# 终端 2：
cd inbound-ai
$env:AI_SERVICE_INTERNAL_TOKEN="dev_internal_token_change_me"   # 与 Java 对齐
$env:OPENAI_API_KEY="sk-..."                                   # 可选
uv run uvicorn app.main:app --reload --port 8090

# 终端 3：
python deploy/scripts/test_ai_health.py
```

## 质量 / 证据

**必须提供**：
- `pytest` 输出
- `curl http://localhost:8090/health` 响应
- 带 token 的 `/ai/health` 响应
- grounded 拒绝用例 curl 或 pytest 说明
- `docker build` 成功日志（一行即可）

**交给下一棒**：
- **运维**：[EPIC-10 compose](2026-06-25-tech-director-to-devops-epic10-ai-compose.md) 取消注释 `ai-api`（Dockerfile 就绪后）
- **开发 Phase 2**：`/ai/embed`、`embed_worker`、RabbitMQ（FR-004/005）
- **技术总监**：EPIC-2 GEO 诊断 HANDOFF（依赖 Phase 1 llm_gateway）

## Phase 2 预告（本 HANDOFF 不做）

| 能力 | 路由/Worker | 依赖 EPIC |
|------|-------------|-----------|
| 文档 embed | `POST /ai/embed`, `embed_worker` | FR-004/005 |
| RAG 检索 | `POST /ai/rag/search` | EPIC-10 |
| GEO 诊断 | `POST /ai/diagnose`, `diagnose_worker` | EPIC-2 |

---

> 完成后：开发在本文件末尾追加 **Done** 段，并更新 `MEMORY.md`。

## Done（由 To 角色填写）

- **完成时间**：2026-06-26
- **结果摘要**：
  - **结构**：`inbound-ai/app/` — `main`, `config`, `deps`, `routers/health|llm`, `services/llm_gateway`, `models/`
  - **API**：`GET /health`（公开）；`GET /ai/health`、`POST /ai/llm/complete`（Bearer / `X-Internal-Token`）
  - **铁律**：`probe_mode=grounded-api` + `grounding_enabled=false` → HTTP 400 `ProbeConfigError`
  - **测试**：`uv run pytest tests/ -q` → **11 passed**
  - **运行**：`uv run uvicorn app.main:app --port 8090`；`deploy/scripts/test_ai_health.py` ✅
  - **打包**：`Dockerfile` 就绪（本机无 Docker，未执行 `docker build`）
- **遗留**：
  - Phase 2：embed/RAG/worker/RabbitMQ
  - 运维 compose 启用 `ai-api`（见 ops HANDOFF）
  - 有 LLM key 时 `/ai/llm/complete` 实网 smoke（当前 litellm=no_key 可接受）
