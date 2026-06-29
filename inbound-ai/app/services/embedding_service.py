"""LiteLLM embedding (OpenAI text-embedding-3-small) with optional mock for tests."""

from __future__ import annotations

import hashlib
import logging
import math

from app.config import Settings, get_settings

logger = logging.getLogger(__name__)

EMBED_DIM = 1536


class EmbeddingError(Exception):
    pass


async def embed_texts(texts: list[str], settings: Settings | None = None) -> list[list[float]]:
    cfg = settings or get_settings()
    if not texts:
        return []
    if cfg.embed_mock:
        return [_mock_vector(t) for t in texts]
    if not cfg.openai_api_key:
        raise EmbeddingError("OPENAI_API_KEY required when EMBED_MOCK=false")
    return await _litellm_embed(texts, cfg)


async def embed_query(query: str, settings: Settings | None = None) -> list[float]:
    vectors = await embed_texts([query], settings)
    return vectors[0]


async def _litellm_embed(texts: list[str], cfg: Settings) -> list[list[float]]:
    from litellm import aembedding  # noqa: PLC0415

    kwargs: dict = {
        "model": cfg.embedding_model,
        "input": texts,
        "api_key": cfg.openai_api_key,
    }
    if cfg.openai_api_base:
        kwargs["api_base"] = cfg.openai_api_base
    resp = await aembedding(**kwargs)
    return [item["embedding"] for item in resp.data]


def _mock_vector(text: str) -> list[float]:
    digest = hashlib.sha256(text.encode("utf-8")).digest()
    values: list[float] = []
    while len(values) < EMBED_DIM:
        for i in range(0, len(digest), 4):
            chunk = digest[i : i + 4]
            if len(chunk) < 4:
                break
            n = int.from_bytes(chunk, "big") / 2**32
            values.append(n * 2 - 1)
            if len(values) >= EMBED_DIM:
                break
        digest = hashlib.sha256(digest).digest()
    norm = math.sqrt(sum(v * v for v in values)) or 1.0
    return [v / norm for v in values]


def vector_to_pg(v: list[float]) -> str:
    return "[" + ",".join(f"{x:.8f}" for x in v) + "]"
