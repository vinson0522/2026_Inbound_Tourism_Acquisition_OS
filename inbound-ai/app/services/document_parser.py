"""Document → plain text (Docling when available, else inline content / plain text)."""

from __future__ import annotations

import logging
from pathlib import Path

import httpx

logger = logging.getLogger(__name__)


async def extract_text(*, content: str | None, file_url: str | None) -> str:
    if content and content.strip():
        return content.strip()
    if not file_url:
        raise ValueError("knowledge asset has no content or file_url")
    return await _extract_from_file_url(file_url)


async def _extract_from_file_url(file_url: str) -> str:
    async with httpx.AsyncClient(timeout=60.0, follow_redirects=True) as client:
        resp = await client.get(file_url)
        resp.raise_for_status()
        data = resp.content
        content_type = resp.headers.get("content-type", "")

    suffix = _guess_suffix(file_url, content_type)
    if suffix in {".txt", ".md", ".markdown"}:
        return data.decode("utf-8", errors="replace").strip()

    try:
        return _parse_with_docling(data, suffix)
    except ImportError:
        logger.warning("docling not installed — falling back to utf-8 decode for %s", file_url)
        return data.decode("utf-8", errors="replace").strip()


def _parse_with_docling(data: bytes, suffix: str) -> str:
    from docling.document_converter import DocumentConverter  # noqa: PLC0415

    tmp = Path("/tmp") / f"inbound-embed{suffix or '.bin'}"
    tmp.write_bytes(data)
    try:
        result = DocumentConverter().convert(str(tmp))
        return result.document.export_to_markdown().strip()
    finally:
        tmp.unlink(missing_ok=True)


def _guess_suffix(file_url: str, content_type: str) -> str:
    lower = file_url.lower()
    for ext in (".pdf", ".docx", ".txt", ".md", ".markdown", ".html"):
        if lower.endswith(ext):
            return ext
    if "pdf" in content_type:
        return ".pdf"
    if "word" in content_type or "docx" in content_type:
        return ".docx"
    return ".txt"
