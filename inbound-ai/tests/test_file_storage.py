from pathlib import Path

import pytest

from app.services.file_storage import FileFetchError, fetch_file_bytes

FIXTURES = Path(__file__).resolve().parent / "fixtures"
SAMPLE_PDF = FIXTURES / "sample_brand_overview.pdf"


@pytest.mark.asyncio
async def test_fetch_local_file_uri():
    uri = SAMPLE_PDF.as_uri()
    data, suffix = await fetch_file_bytes(uri)
    assert suffix == ".pdf"
    assert data[:4] == b"%PDF"


@pytest.mark.asyncio
async def test_fetch_missing_local_raises():
    with pytest.raises(FileFetchError):
        await fetch_file_bytes("file:///nonexistent/inbound-test.pdf")
