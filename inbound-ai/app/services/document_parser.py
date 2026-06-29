"""Document → plain text via Docling (PDF/DOCX) or UTF-8 for plain text."""

from __future__ import annotations

import asyncio
import logging
import tempfile
from pathlib import Path

from app.config import Settings, get_settings
from app.services.file_storage import FileFetchError, fetch_file_bytes

logger = logging.getLogger(__name__)

_TEXT_SUFFIXES = {".txt", ".md", ".markdown", ".html"}
_DOC_SUFFIXES = {".pdf", ".docx"}


class DocumentParseError(Exception):
    pass


async def extract_text(
    *,
    content: str | None,
    file_url: str | None,
    settings: Settings | None = None,
) -> str:
    """Prefer ``file_url`` (Docling) over inline ``content`` when both are present."""
    cfg = settings or get_settings()
    if file_url and file_url.strip():
        return await _extract_from_file_url(file_url.strip(), cfg)
    if content and content.strip():
        return content.strip()
    raise DocumentParseError("knowledge asset has no content or file_url")


async def _extract_from_file_url(file_url: str, settings: Settings) -> str:
    try:
        data, suffix = await fetch_file_bytes(file_url, settings)
    except FileFetchError as exc:
        raise DocumentParseError(str(exc)) from exc

    if suffix in _TEXT_SUFFIXES:
        return data.decode("utf-8", errors="replace").strip()

    if suffix in _DOC_SUFFIXES:
        return await asyncio.to_thread(parse_document_bytes, data, suffix)

    raise DocumentParseError(f"unsupported document type: {suffix}")


def parse_document_bytes(data: bytes, suffix: str) -> str:
    """Parse PDF/DOCX with Docling (sync — run via ``asyncio.to_thread``)."""
    if suffix not in _DOC_SUFFIXES:
        raise DocumentParseError(f"docling does not support suffix {suffix}")
    try:
        from docling.datamodel.base_models import InputFormat  # noqa: PLC0415
        from docling.datamodel.pipeline_options import PdfPipelineOptions  # noqa: PLC0415
        from docling.document_converter import DocumentConverter, PdfFormatOption  # noqa: PLC0415
    except ImportError as exc:
        raise DocumentParseError(
            "docling is required for PDF/DOCX parsing — install docling>=2.0"
        ) from exc

    ext = suffix if suffix.startswith(".") else f".{suffix}"
    with tempfile.NamedTemporaryFile(suffix=ext, delete=False) as tmp:
        tmp.write(data)
        tmp_path = tmp.name
    try:
        if suffix == ".pdf":
            pdf_opts = PdfPipelineOptions()
            pdf_opts.do_ocr = False
            pdf_opts.do_table_structure = False
            pdf_opts.force_backend_text = True
            converter = DocumentConverter(
                format_options={InputFormat.PDF: PdfFormatOption(pipeline_options=pdf_opts)}
            )
        else:
            converter = DocumentConverter()
        result = converter.convert(tmp_path)
        text = result.document.export_to_markdown().strip()
    except Exception as exc:
        raise DocumentParseError(f"docling failed for {ext}: {exc}") from exc
    finally:
        Path(tmp_path).unlink(missing_ok=True)

    if not text:
        raise DocumentParseError(f"docling extracted no text from {ext}")
    return text
