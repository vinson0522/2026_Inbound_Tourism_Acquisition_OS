import pytest

from app.config import Settings
from app.services.embedding_service import EMBED_DIM, embed_texts, embed_query


@pytest.mark.asyncio
async def test_mock_embedding_dimension():
    cfg = Settings(embed_mock=True, openai_api_key=None)
    vectors = await embed_texts(["alpha", "beta"], settings=cfg)
    assert len(vectors) == 2
    assert len(vectors[0]) == EMBED_DIM
    assert vectors[0] != vectors[1]


@pytest.mark.asyncio
async def test_mock_query_embedding_stable():
    cfg = Settings(embed_mock=True)
    v1 = await embed_query("visa policy", settings=cfg)
    v2 = await embed_query("visa policy", settings=cfg)
    assert v1 == v2
