"""Vector search (top-20) + bge-reranker (top-3), scoped by tenant_id + project_id."""

from __future__ import annotations

from app.config import Settings, get_settings
from app.models.embed import RagChunkHit, RagSearchData, RagSearchRequest
from app.repositories import knowledge_repository
from app.services import embedding_service, reranker_service


async def search(req: RagSearchRequest, settings: Settings | None = None) -> RagSearchData:
    cfg = settings or get_settings()
    query_vec = await embedding_service.embed_query(req.query, cfg)
    vector_top_k = cfg.rag_vector_top_k
    rows = await knowledge_repository.search_chunks(
        tenant_id=req.tenant_id,
        project_id=req.project_id,
        query_vector=query_vec,
        top_k=vector_top_k,
    )
    if not rows:
        return RagSearchData(hits=[])

    documents = [row["chunk_text"] for row in rows]
    final_k = min(req.top_k, cfg.rag_rerank_top_k, len(documents))
    ranked = await reranker_service.rerank(req.query, documents, top_k=final_k, settings=cfg)

    hits = [
        RagChunkHit(
            chunk_id=rows[idx]["id"],
            asset_id=rows[idx]["asset_id"],
            chunk_index=rows[idx]["chunk_index"],
            chunk_text=rows[idx]["chunk_text"],
            score=score,
        )
        for idx, score in ranked
    ]
    return RagSearchData(hits=hits)
