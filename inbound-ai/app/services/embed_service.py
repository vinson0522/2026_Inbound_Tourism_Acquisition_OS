"""Embed pipeline: parse → chunk → embed → knowledge_chunk."""

from __future__ import annotations

import logging

from app.config import Settings, get_settings
from app.models.embed import EmbedRequest, EmbedResultData
from app.repositories import knowledge_repository
from app.services import chunk_service, document_parser, embedding_service
from app.services.embedding_service import EmbeddingError

logger = logging.getLogger(__name__)


class EmbedPipelineError(Exception):
    pass


async def run_embed(req: EmbedRequest, settings: Settings | None = None) -> EmbedResultData:
    cfg = settings or get_settings()
    asset = await knowledge_repository.fetch_asset(req.asset_id, req.tenant_id, req.project_id)
    if asset is None:
        raise EmbedPipelineError(f"knowledge asset not found: assetId={req.asset_id}")

    await knowledge_repository.set_asset_status(req.asset_id, "INDEXING")
    try:
        text = await document_parser.extract_text(content=asset.content, file_url=req.file_url or asset.file_url)
        pieces = chunk_service.chunk_text(
            text,
            chunk_size=cfg.chunk_size,
            chunk_overlap=cfg.chunk_overlap,
        )
        if not pieces:
            raise EmbedPipelineError("no text extracted for embedding")

        vectors = await embedding_service.embed_texts(pieces, cfg)
        rows: list[tuple[int, str, int, list[float]]] = []
        for idx, (piece, vec) in enumerate(zip(pieces, vectors, strict=True)):
            rows.append((idx, piece, chunk_service.count_tokens(piece), vec))

        count = await knowledge_repository.replace_chunks(
            tenant_id=req.tenant_id,
            project_id=req.project_id,
            asset_id=req.asset_id,
            chunks=rows,
        )
        await knowledge_repository.set_asset_status(req.asset_id, "READY")
        logger.info(
            "embed complete asset_id=%s tenant_id=%s project_id=%s chunks=%s",
            req.asset_id,
            req.tenant_id,
            req.project_id,
            count,
        )
        return EmbedResultData(asset_id=req.asset_id, chunk_count=count, vector_status="READY")
    except Exception:
        await knowledge_repository.set_asset_status(req.asset_id, "FAILED")
        raise
