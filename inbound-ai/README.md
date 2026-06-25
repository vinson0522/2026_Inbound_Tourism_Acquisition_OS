# inbound-ai

Python FastAPI AI 微服务。

## 职责

LiteLLM 网关、LangGraph Agent、RAG（LlamaIndex + pgvector）、GEO 诊断采样与 citations 解析、评分、Docling 向量化、视频拆帧 — **不含权限与计费**。

## 目标结构

```
inbound-ai/
├── app/
│   ├── routers/       # /ai/diagnose, /ai/embed, ...
│   ├── agents/        # LangGraph 状态图
│   ├── services/      # llm_gateway, scorer, citation_parser
│   └── workers/       # RabbitMQ 消费者
├── tests/
├── pyproject.toml
└── Dockerfile
```

## 对应 EPIC

EPIC-10（核心）、EPIC-2/3/4/5/6/7 的 AI 能力

## 参考

- `docs/ARCHITECTURE.md` §7
- `PRD_商业化版_V2.0.md` §7.6、§9、§10
- `AGENT.md` §6、§7

## 状态

⏳ **待 scaffold**
