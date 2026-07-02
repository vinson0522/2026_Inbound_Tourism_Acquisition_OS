# HANDOFF | 技术总监 → 开发（Python / AI）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 Python | 2026-07-10 | 维护轨 · B-21 |

## 上下文

C23 关闭后 smoke 回归：`test_embed_e2e.py` direct embed **HTTP 500**；`test_knowledge_rag_search.py` **RAG 500**（Java 代理 AI `/ai/rag/search`）。

**相关文件**：
- `inbound-ai/app/routers/` embed + rag 路由
- `deploy/scripts/test_embed_e2e.py` · `test_knowledge_rag_search.py`
- `deploy/docker-compose.yml` ai-api 服务

## 交付请求

- [x] 本机 `:8090` direct embed 成功（或文档化 `EMBED_MOCK=true` 为 smoke 默认并改脚本）
- [x] RAG search 返回 200 + ≥1 hit（demo asset#1）
- [x] `uv run pytest tests/` 相关用例仍绿

## 验收标准

```bash
python deploy/scripts/test_embed_e2e.py
python deploy/scripts/test_knowledge_rag_search.py
```

## Prompt

```
角色：开发 Python。必读 inbound-ai embed/RAG 路由、MEMORY B-21、维护 Sprint 索引 2026-07-10-tech-director-maintenance-smoke-regression-sprint.md。
任务：修复 embed/RAG smoke 500。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-02
- **结果摘要**：
  - 根因：本机 uvicorn 无 `DATABASE_URL` + `EMBED_MOCK=false` 无 OpenAI Key → 500
  - `config.py` 默认 `database_url`（本地 PG 5432）· `embed_mock=true` · `reranker_mock=true`
  - `embed.py` 捕获 `EmbeddingError`/DB 缺失 → 503（非裸 500）
  - `.env.example` · `docker-compose.local-d.yml` 对齐 smoke 默认
  - smoke ✅ chunkId=3 · pytest embed/rag 4 passed
- **遗留**：生产 compose 仍显式 `EMBED_MOCK=false` + Key；需重启 ai-api 加载新默认
