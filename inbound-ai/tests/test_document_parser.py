from pathlib import Path

import pytest

from app.services.document_parser import DocumentParseError, extract_text, parse_document_bytes

FIXTURES = Path(__file__).resolve().parent / "fixtures"
SAMPLE_PDF = FIXTURES / "sample_brand_overview.pdf"
MOCK_INLINE = "Dragon Journey Travel mock inline content for seed demo."
PDF_MARKER = "Zhangjiajie"
PDF_PHRASE = "glass bridge private tour"


@pytest.fixture
def sample_pdf_path() -> Path:
    assert SAMPLE_PDF.is_file(), f"missing fixture: {SAMPLE_PDF}"
    return SAMPLE_PDF


def test_parse_pdf_bytes_returns_docling_text(sample_pdf_path: Path):
    data = sample_pdf_path.read_bytes()
    text = parse_document_bytes(data, ".pdf")
    assert PDF_MARKER in text
    assert PDF_PHRASE in text
    assert MOCK_INLINE not in text


@pytest.mark.asyncio
async def test_extract_text_prefers_file_url_over_inline_content(sample_pdf_path: Path):
    file_url = sample_pdf_path.as_uri()
    text = await extract_text(content=MOCK_INLINE, file_url=file_url)
    assert PDF_MARKER in text
    assert MOCK_INLINE not in text


@pytest.mark.asyncio
async def test_extract_text_inline_when_no_file_url():
    text = await extract_text(content=MOCK_INLINE, file_url=None)
    assert text == MOCK_INLINE


@pytest.mark.asyncio
async def test_extract_text_missing_source_raises():
    with pytest.raises(DocumentParseError):
        await extract_text(content=None, file_url=None)
