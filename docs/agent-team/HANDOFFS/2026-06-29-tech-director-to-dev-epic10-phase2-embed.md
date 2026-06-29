# HANDOFF | 技术总监 → 开发

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-10 Phase 2 / FR-004 / FR-005 |

## 上下文

**当前状态**：EPIC-10 Phase 1（LiteLLM gateway + health）与 EPIC-2 M1 本地 E2E 已验通。知识库 embed/RAG 为 Phase 2 范围。

**相关文件**：
- `inbound-ai/app/workers/embed_worker.py` — MQ 消费者
- `inbound-core/ruoyi-modules/ruoyi-project/` — Java 发 `ai.embed`
- `database/ddl/001_schema.sql` — `knowledge_asset` / `knowledge_chunk`

**约束**：Python 管 embed；Java 不做 embedding；检索必须 `tenant_id` + `project_id`；本地 ADR-09 Docker。

## 交付请求

**需要什么**：知识库 embed 管道 MVP — Docling/文本解析 → 512 token 切片 → embedding → `knowledge_chunk`；MQ `ai.embed`。

**验收标准**：
- [x] Java 创建/重索引资产后发 `ai.embed`
- [x] Python worker 消费并写入 `knowledge_chunk`，`vector_status=READY`
- [x] `/ai/rag/search` 带 tenant/project 过滤
- [x] pytest 覆盖切片与 mock embedding
- [x] `deploy/scripts/test_embed_e2e.py` 本地验通

## Done（由 To 角色填写）

- **完成时间**：2026-06-29
- **结果摘要**：
  - `inbound-ai`：`embed_worker`、`chunk_service`（512/64）、`embedding_service`（LiteLLM + `EMBED_MOCK`）、`knowledge_repository`、`/ai/embed`、`/ai/rag/search`
  - `ruoyi-project`：`KnowledgeAsset` CRUD + `AiEmbedPublisher` → `ai.embed`
  - Demo seed `knowledge_asset` id=1；`test_embed_e2e.py` direct 模式验通 `READY`
  - pytest：`test_chunk_service` / `test_embedding_service` / `test_rag_service` / `test_embed_pipeline`（需 `DATABASE_URL`）
- **遗留**：
  - ~~生产需 `EMBED_MOCK=false` + 有效 embedding Key~~ → Phase 2.1 ✅ OpenAI embedding + reranker
  - Docling 可选依赖（无 file_url 时用 `content` 字段）
  - ~~bge-reranker top-3 留 Phase 2.1~~ → ✅ `reranker_service` pgvector top-20 → rerank top-3
