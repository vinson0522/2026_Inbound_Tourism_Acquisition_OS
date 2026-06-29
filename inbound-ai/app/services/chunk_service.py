"""512-token sliding window chunking (overlap 64) — FR-004 / AGENTS.md §7."""

from __future__ import annotations

import logging

logger = logging.getLogger(__name__)

DEFAULT_CHUNK_SIZE = 512
DEFAULT_CHUNK_OVERLAP = 64


def _encoding():
    try:
        import tiktoken

        return tiktoken.get_encoding("cl100k_base")
    except Exception:
        return None


def count_tokens(text: str) -> int:
    enc = _encoding()
    if enc is None:
        return max(1, len(text.split()))
    return len(enc.encode(text))


def chunk_text(
    text: str,
    *,
    chunk_size: int = DEFAULT_CHUNK_SIZE,
    chunk_overlap: int = DEFAULT_CHUNK_OVERLAP,
) -> list[str]:
    """Split text into token-bounded chunks with overlap."""
    cleaned = (text or "").strip()
    if not cleaned:
        return []

    enc = _encoding()
    if enc is None:
        return _chunk_by_words(cleaned, chunk_size, chunk_overlap)

    tokens = enc.encode(cleaned)
    if len(tokens) <= chunk_size:
        return [cleaned]

    chunks: list[str] = []
    start = 0
    while start < len(tokens):
        end = min(start + chunk_size, len(tokens))
        piece = enc.decode(tokens[start:end]).strip()
        if piece:
            chunks.append(piece)
        if end >= len(tokens):
            break
        start = max(0, end - chunk_overlap)
    return chunks


def _chunk_by_words(text: str, chunk_size: int, chunk_overlap: int) -> list[str]:
    words = text.split()
    if len(words) <= chunk_size:
        return [text]
    chunks: list[str] = []
    start = 0
    while start < len(words):
        end = min(start + chunk_size, len(words))
        chunks.append(" ".join(words[start:end]))
        if end >= len(words):
            break
        start = max(0, end - chunk_overlap)
    return chunks
