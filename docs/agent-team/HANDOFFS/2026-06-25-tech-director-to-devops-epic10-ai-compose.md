# HANDOFF | 技术总监 → 运维

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 运维 |
| **日期** | 2026-06-25 |
| **优先级** | Medium |
| **关联** | EPIC-10 · ADR-20260625-06 |

## 上下文

**当前状态**：开发 **Phase 1 Done** ✅（[dev HANDOFF Done](2026-06-25-tech-director-to-dev-epic10-scaffold.md)）。`inbound-ai/Dockerfile` 已就绪。**门禁已开** — 可取消注释 compose 并部署 `ai-api`。

**本任务时机**：**门禁** — 开发交付可 `docker build` 的 `inbound-ai/Dockerfile` 后再取消注释 compose 并部署。

**相关文件**：
- `deploy/docker-compose.yml` / `docker-compose.prod.yml` — `ai-api` 块
- `docs/TECH_STACK_COMPONENTS.md` §4.2 — AI 环境变量
- `docs/INFRA_ACCESS.md` / `INFRA_ACCESS.local.md` — 凭证与隧道
- `inbound-ai/.env.example` — 开发交付后同步

**约束**：
- `ai-api` 仅内网暴露；**不对公网**开放 8090
- `AI_SERVICE_INTERNAL_TOKEN` 写入服务器 `deploy/.env`（chmod 600），与 Java 侧一致
- LLM API Key 不入 Git；服务器用 `deploy/.env` 或密钥管理
- Phase 1 不要求 RabbitMQ worker 消费，但 compose 可预置 `RABBITMQ_URL`

## 交付请求

**需要什么**：为 `inbound-ai` 准备 compose 集成与凭证文档，开发 Dockerfile 就绪后在服务器验通 `ai-api` 容器。

## 验收标准

### A. 文档与模板（可与开发并行）

- [ ] `deploy/.env.example` 追加 AI 相关变量（若文件不存在则新建示例段）：
  - `AI_SERVICE_INTERNAL_TOKEN`
  - `OPENAI_API_KEY` / `PERPLEXITY_API_KEY` / `GEMINI_API_KEY`（占位）
  - `LANGFUSE_PUBLIC_KEY` / `LANGFUSE_SECRET_KEY`
- [ ] `deploy/README.md`「当前 Compose 服务」表：`ai-api` 状态改为「待 Dockerfile 后启用」→ 完成后改为 ✅
- [ ] `docs/INFRA_ACCESS.md` §5 追加 `inbound-ai` 连接说明：
  - 容器内：`http://ai-api:8090`
  - 本机调试：`http://localhost:8090`（uvicorn 直跑）或隧道后访问服务器映射

### B. Compose 集成（**门禁：开发 Dockerfile ✅ 后**）

- [ ] 取消 `docker-compose.yml` 中 `ai-api` 注释；`build: ../inbound-ai`；端口 `8090:8090`
- [ ] 环境变量对齐 `TECH_STACK_COMPONENTS.md` §4.2（DATABASE_URL、RABBITMQ_URL、LANGFUSE_HOST、INTERNAL_TOKEN、LLM keys）
- [ ] `depends_on`: postgres + rabbitmq healthy
- [ ] 同步 `docker-compose.prod.yml`（绑定 `127.0.0.1:8090`）
- [ ] 服务器：`docker compose -f docker-compose.prod.yml up -d --build ai-api`
- [ ] `curl http://127.0.0.1:8090/health` → 200
- [ ] 更新 `server_infra_verify.py` 或新增 `test_ai_health.py` 远程调用段（可选）

### C. Java 联调准备

- [ ] 确认 `inbound-core` 可配置 `AI_SERVICE_BASE_URL=http://127.0.0.1:8090`（隧道或同机）
- [ ] 在 `INFRA_ACCESS.local.md` §5 追加 AI 内网 token 与 base URL（不写真实 key 到 Git）

## 质量 / 证据

**必须提供**：
- `docker compose ps` 含 `inbound-ai` running
- `curl /health` 输出
- `.env.example` diff 摘要

**交给下一棒**：开发 Java Feign 调 `/ai/health`；技术总监排 EPIC-2

---

## Done（由 To 角色填写）

- **完成时间**：2026-06-26 10:12 CST
- **结果摘要**：
  - `docker-compose.prod.yml` / `docker-compose.yml` 已启用 `ai-api`（`build: ../inbound-ai`，绑定 `127.0.0.1:8090`）
  - 新增 `deploy/scripts/server_ai_deploy.py` — 上传源码 + `docker compose up -d --build ai-api`
  - 服务器 `18.139.209.10`：`inbound-ai` **Up (healthy)**；`curl http://127.0.0.1:8090/health` → **200** `{"status":"ok","service":"inbound-ai","version":"0.1.0"}`
  - 服务器 `.env` 已追加 `AI_SERVICE_INTERNAL_TOKEN`（随机生成，chmod 600；同步本机：`export_infra_credentials.py`）
  - `deploy/.env.example`、`deploy/README.md`、`server_infra_verify.py` 已更新
- **遗留**：
  - 本机无 Docker，本地 compose `ai-api` 未验
  - LLM API Key 未配置（Phase 1 `/health` 不依赖；`/ai/llm/complete` 需后续写入服务器 `.env`）
  - Java `AI_SERVICE_*` 联调待开发 HANDOFF
