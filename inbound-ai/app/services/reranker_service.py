"""bge-reranker-v2-m3 cross-encoder reranking (vector top-20 → rerank top-3)."""

from __future__ import annotations

import asyncio
import logging
import re
from functools import partial

from app.config import Settings, get_settings

logger = logging.getLogger(__name__)

_reranker = None
_reranker_model_name: str | None = None


class RerankerError(Exception):
    pass


def _tokenize(text: str) -> set[str]:
    return {t for t in re.split(r"\W+", text.lower()) if t}


def _mock_rerank(query: str, documents: list[str], top_k: int) -> list[tuple[int, float]]:
    q_tokens = _tokenize(query)
    scored: list[tuple[int, float]] = []
    for idx, doc in enumerate(documents):
        d_tokens = _tokenize(doc)
        overlap = len(q_tokens & d_tokens)
        score = overlap / max(len(q_tokens), 1)
        scored.append((idx, float(score)))
    scored.sort(key=lambda item: item[1], reverse=True)
    return scored[:top_k]


def _get_reranker(model_name: str):
    global _reranker, _reranker_model_name
    if _reranker is not None and _reranker_model_name == model_name:
        return _reranker
    try:
        from sentence_transformers import CrossEncoder  # noqa: PLC0415
    except ImportError as exc:
        raise RerankerError(
            "sentence-transformers required for reranker (pip install sentence-transformers)"
        ) from exc
    logger.info("loading reranker model=%s", model_name)
    _reranker = CrossEncoder(model_name)
    _reranker_model_name = model_name
    return _reranker


def _rerank_sync(query: str, documents: list[str], top_k: int, cfg: Settings) -> list[tuple[int, float]]:
    if not documents:
        return []
    model = _get_reranker(cfg.reranker_model)
    pairs = [[query, doc] for doc in documents]
    scores = model.predict(pairs)
    ranked = sorted(enumerate(scores), key=lambda item: float(item[1]), reverse=True)
    return [(idx, float(score)) for idx, score in ranked[:top_k]]


async def rerank(
    query: str,
    documents: list[str],
    top_k: int = 3,
    settings: Settings | None = None,
) -> list[tuple[int, float]]:
    cfg = settings or get_settings()
    if not documents:
        return []
    limit = min(top_k, len(documents))
    if cfg.reranker_mock:
        return _mock_rerank(query, documents, limit)
    loop = asyncio.get_running_loop()
    return await loop.run_in_executor(
        None,
        partial(_rerank_sync, query, documents, limit, cfg),
    )
