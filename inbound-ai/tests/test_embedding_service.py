import os

import pytest

from app.config import Settings
from app.services.embedding_service import EMBED_DIM, EmbeddingError, embed_query, embed_texts


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


@pytest.mark.asyncio
async def test_real_embedding_requires_openai_key():
    cfg = Settings(embed_mock=False, openai_api_key=None)
    with pytest.raises(EmbeddingError):
        await embed_texts(["hello"], settings=cfg)


@pytest.mark.asyncio
@pytest.mark.skipif(not os.environ.get("OPENAI_API_KEY"), reason="OPENAI_API_KEY required")
async def test_openai_embedding_live():
    cfg = Settings(
        embed_mock=False,
        openai_api_key=os.environ["OPENAI_API_KEY"],
        openai_api_base=os.environ.get("OPENAI_API_BASE"),
        embedding_model="openai/text-embedding-3-small",
    )
    vectors = await embed_texts(["China inbound tour"], settings=cfg)
    assert len(vectors) == 1
    assert len(vectors[0]) == EMBED_DIM
