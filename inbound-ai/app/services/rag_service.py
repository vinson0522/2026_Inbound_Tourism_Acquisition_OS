"""Vector search scoped by tenant_id + project_id."""

from __future__ import annotations

from app.config import Settings, get_settings
from app.models.embed import RagChunkHit, RagSearchData, RagSearchRequest
from app.repositories import knowledge_repository
from app.services import embedding_service


async def search(req: RagSearchRequest, settings: Settings | None = None) -> RagSearchData:
    cfg = settings or get_settings()
    query_vec = await embedding_service.embed_query(req.query, cfg)
    rows = await knowledge_repository.search_chunks(
        tenant_id=req.tenant_id,
        project_id=req.project_id,
        query_vector=query_vec,
        top_k=req.top_k,
    )
    hits = [
        RagChunkHit(
            chunk_id=row["id"],
            asset_id=row["asset_id"],
            chunk_index=row["chunk_index"],
            chunk_text=row["chunk_text"],
            score=float(row["score"]),
        )
        for row in rows
    ]
    return RagSearchData(hits=hits)
