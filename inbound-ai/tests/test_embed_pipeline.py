import os

import pytest

from app.config import Settings
from app.models.embed import EmbedRequest
from app.services import embed_service

pytestmark = pytest.mark.skipif(
    not os.environ.get("DATABASE_URL"),
    reason="DATABASE_URL required for knowledge repository integration test",
)


@pytest.mark.asyncio
async def test_embed_demo_asset_reaches_ready():
    cfg = Settings(
        database_url=os.environ["DATABASE_URL"],
        embed_mock=True,
    )
    req = EmbedRequest(
        assetId=int(os.environ.get("EMBED_TEST_ASSET_ID", "1")),
        tenantId=int(os.environ.get("EMBED_TEST_TENANT_ID", "1")),
        projectId=int(os.environ.get("EMBED_TEST_PROJECT_ID", "1")),
    )
    result = await embed_service.run_embed(req, settings=cfg)
    assert result.vector_status == "READY"
    assert result.chunk_count >= 1
