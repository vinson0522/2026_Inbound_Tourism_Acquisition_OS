# HANDOFF | 技术总监 → 开发（inbound-ai）

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-06-26 |
| **优先级** | Critical（EPIC-2 阻塞项） |
| **关联** | EPIC-2 M1 · FR-104/105 · ADR-20260626-07 · [Sprint 总览](2026-06-26-tech-director-epic2-geo-sprint.md) |

## 上下文

**当前状态**：EPIC-10 Phase 1 ✅（`/ai/llm/complete` + grounded 铁律）。服务器 `ai-api` healthy；OpenAI/Gemini Key 已配置；**PERPLEXITY_API_KEY 待补**（M1 主力模型）。

**本 HANDOFF**：实现 GEO **grounded-api 探针** AI 能力 —— diagnose / parse-citations / score + RabbitMQ worker。

**相关文件**：
- `docs/ARCHITECTURE.md` §7.3–§7.4、§GEO 评分
- `AGENTS.md` §6（GEO 专项）、§10 MQ 契约 `diag.grounded-api`
- `database/ddl/001_schema.sql` — `diagnostic_result` 字段
- `inbound-ai/app/services/llm_gateway.py` — 需扩展多供应商 key
- `PRD_商业化版_V2.0.md` §7.6、§10 GEO 公式

**约束**（铁律）：
- `probe_mode=grounded-api` → **必须** `grounding_enabled=true`，否则拒绝
- **禁止**裸 Chat Completions 结果充当 GEO 探针
- **不缓存** GEO 诊断 LLM 响应
- Python 不写权限/计费；回调 Java 写库（worker 通过 HTTP callback 或 Java 轮询 —— M1 采用 **Java 同步等 worker 写回** 见 Java HANDOFF）

## 交付请求

**需要什么**：inbound-ai 提供 GEO 诊断 AI 端点 + MQ 消费者，输出统一 `diagnostic_result` 结构供 Java 落库。

## 验收标准

### 1. 修复 `llm_gateway` 多供应商 Key（阻塞 GEO）

- [ ] 按 `model` 前缀路由：`openai/*` → `OPENAI_API_KEY`；`gemini/*` → `GEMINI_API_KEY`；`perplexity/*` → `PERPLEXITY_API_KEY`
- [ ] 保留 `openai_api_base` 仅作用于 OpenAI 兼容端点
- [ ] 单测：三种 prefix mock 各走对应 env key

### 2. 新增路由

#### `POST /ai/diagnose`

**Request**（Pydantic，字段对齐 Java MQ payload）：

```json
{
  "trace_id": "uuid",
  "run_id": 1,
  "question_id": 1,
  "tenant_id": 1,
  "project_id": 1,
  "platform": "perplexity",
  "probe_mode": "grounded-api",
  "region": "us-east",
  "locale": "en-US",
  "question": "Can you recommend reliable China travel agencies?",
  "sample_index": 0,
  "model": "perplexity/sonar-pro",
  "grounding_enabled": true
}
```

**Response** `ApiResponse`：

```json
{
  "code": 0,
  "data": {
    "answer_text": "...",
    "model": "perplexity/sonar-pro",
    "platform": "perplexity",
    "probe_mode": "grounded-api",
    "mentioned_brands": ["China Highlights"],
    "competitors": ["Trip.com"],
    "citations": [
      {"url": "...", "title": "...", "domain": "...", "rank": 1, "is_customer": false, "is_competitor": true}
    ],
    "rank": 2,
    "capture_method": "grounded-api",
    "raw_response_json": {},
    "sampled_at": "2026-06-26T12:00:00Z"
  },
  "trace_id": "..."
}
```

- [ ] `grounding_enabled=false` + `grounded-api` → 400（已有逻辑复用）
- [ ] 无 Perplexity key 时明确 503
- [ ] Langfuse trace（有 key 时）

#### `POST /ai/parse-citations`

- [ ] 输入：platform + raw_response_json；输出：统一 citations 列表（Perplexity `[1]` 映射 MVP 即可）
- [ ] 单测 fixture：Perplexity citations JSON

#### `POST /ai/score`

- [ ] 输入：run 级聚合请求（results 摘要或 run_id + Java 传 metrics 数组）
- [ ] 权重从请求体 `metric_weights_json` 读取，**禁止硬编码**（默认与 PRD §10 一致）
- [ ] 输出：`geo_score` + 分项 metrics JSON

### 3. RabbitMQ Worker

- [ ] 依赖：`aio-pika` 加入 `pyproject.toml`
- [ ] 队列：`diag.grounded-api`（与 `AGENTS.md` §10 一致）
- [ ] 消费 payload → 调 `diagnose` 逻辑 → **HTTP POST** Java callback（M1 URL 由 Java HANDOFF 定义，如 `/api/v1/internal/diagnostics/probe-callback`）
- [ ] 手动 ACK；失败 retry ≤3 → DLQ
- [ ] 环境变量：`RABBITMQ_URL`、`CORE_CALLBACK_BASE_URL`

### 4. 测试

- [ ] `pytest` 新增：`test_diagnose_grounded.py`、`test_citation_parser_perplexity.py`、`test_scorer_weights.py`
- [ ] grounded 拒绝 + mock LiteLLM 成功路径
- [ ] 全量 `uv run pytest tests/ -q` 通过

### 5. 文档

- [ ] `inbound-ai/README.md` 追加 diagnose/score/worker 启动说明
- [ ] 更新 HANDOFF Done + `MEMORY.md`

## 本地验通（curl 示例）

```powershell
$env:AI_SERVICE_INTERNAL_TOKEN="..."
$env:PERPLEXITY_API_KEY="pplx-..."
# uvicorn 或连服务器 ai-api
curl -X POST http://localhost:8090/ai/diagnose `
  -H "Authorization: Bearer $env:AI_SERVICE_INTERNAL_TOKEN" `
  -H "Content-Type: application/json" `
  -d '{"run_id":1,"question_id":1,"tenant_id":1,"project_id":1,"platform":"perplexity","probe_mode":"grounded-api","grounding_enabled":true,"question":"test","model":"perplexity/sonar-pro","region":"us-east","locale":"en-US","sample_index":0}'
```

## 质量 / 证据

- pytest 输出
- 一条真实 Perplexity diagnose 响应摘要（脱敏）
- scorer 权重来自 JSON 的单元测试说明

**交给下一棒**：[Java diagnostic HANDOFF](2026-06-26-tech-director-to-dev-java-epic2-diagnostic.md)

---

## Done（由 To 角色填写）

- **完成时间**：2026-06-26
- **结果摘要**：
  - `llm_provider.py`：openai/gemini/perplexity 三前缀 key 路由 + `openai_api_base` 仅 OpenAI
  - 新路由：`POST /ai/diagnose`、`/ai/parse-citations`、`/ai/score`（内网 token）
  - RabbitMQ worker：`diag.grounded-api` + DLQ + callback → Java probe-callback
  - `uv run pytest tests/ -q` → **32 passed**
- **遗留**：
  - 真实 Perplexity 联调需 `PERPLEXITY_API_KEY`（503 无 key）
  - Java callback 端点待 [Java HANDOFF](2026-06-26-tech-director-to-dev-java-epic2-diagnostic.md) 实现
