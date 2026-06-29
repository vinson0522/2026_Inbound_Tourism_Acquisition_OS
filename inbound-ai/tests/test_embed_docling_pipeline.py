import os
from pathlib import Path

import pytest

from app.config import Settings
from app.db import close_pool, get_pool, normalize_database_url
from app.models.embed import EmbedRequest
from app.services import embed_service

FIXTURES = Path(__file__).resolve().parent / "fixtures"
SAMPLE_PDF = FIXTURES / "sample_brand_overview.pdf"
MOCK_INLINE = "Dragon Journey Travel mock inline content — must not appear in chunks."
PDF_MARKER = "Zhangjiajie"

pytestmark = pytest.mark.skipif(
    not os.environ.get("DATABASE_URL"),
    reason="DATABASE_URL required for knowledge repository integration test",
)


@pytest.fixture
async def db_pool():
    pool = await get_pool(
        Settings(database_url=os.environ["DATABASE_URL"], embed_mock=True)
    )
    yield pool
    await close_pool()


@pytest.mark.asyncio
async def test_embed_pdf_asset_uses_docling_not_inline_content(db_pool):
    """Upload path: file_url set → chunks from Docling PDF, not inline mock text."""
    cfg = Settings(
        database_url=os.environ["DATABASE_URL"],
        embed_mock=True,
        chunk_size=512,
        chunk_overlap=64,
    )
    pdf_uri = SAMPLE_PDF.as_uri()
    asset_id: int | None = None

    try:
        row = await db_pool.fetchrow(
            """
            INSERT INTO knowledge_asset (
                tenant_id, project_id, type, title, content, file_url, vector_status, created_by
            ) VALUES (
                1, 1, 'DOCUMENT', 'Docling PDF Test', $1, $2, 'PENDING', 1
            ) RETURNING id
            """,
            MOCK_INLINE,
            pdf_uri,
        )
        asset_id = int(row["id"])
        req = EmbedRequest(assetId=asset_id, tenantId=1, projectId=1, fileUrl=pdf_uri)
        result = await embed_service.run_embed(req, settings=cfg)
        assert result.vector_status == "READY"
        assert result.chunk_count >= 1

        chunks = await db_pool.fetch(
            """
            SELECT chunk_text FROM knowledge_chunk
            WHERE asset_id = $1 AND deleted_at IS NULL
            ORDER BY chunk_index
            """,
            asset_id,
        )
        combined = " ".join(r["chunk_text"] for r in chunks)
        assert PDF_MARKER in combined
        assert MOCK_INLINE not in combined
    finally:
        if asset_id is not None:
            await db_pool.execute("DELETE FROM knowledge_chunk WHERE asset_id = $1", asset_id)
            await db_pool.execute("DELETE FROM knowledge_asset WHERE id = $1", asset_id)
