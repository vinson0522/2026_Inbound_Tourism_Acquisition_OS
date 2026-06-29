import pytest

from app.config import Settings
from app.services import reranker_service


def test_mock_rerank_prefers_overlapping_doc():
    cfg = Settings(reranker_mock=True)
    docs = [
        "Dragon Journey Travel offers private China tours.",
        "Unrelated content about cooking recipes.",
        "English-speaking travelers enjoy Beijing and Shanghai.",
    ]
    ranked = reranker_service._mock_rerank("China tour English", docs, 2)
    assert len(ranked) == 2
    assert ranked[0][0] in (0, 2)


@pytest.mark.asyncio
async def test_rerank_async_mock():
    cfg = Settings(reranker_mock=True)
    docs = ["alpha beta", "gamma delta"]
    ranked = await reranker_service.rerank("alpha", docs, top_k=1, settings=cfg)
    assert len(ranked) == 1
    assert ranked[0][0] == 0


@pytest.mark.asyncio
async def test_rerank_empty_documents():
    cfg = Settings(reranker_mock=True)
    assert await reranker_service.rerank("query", [], settings=cfg) == []
