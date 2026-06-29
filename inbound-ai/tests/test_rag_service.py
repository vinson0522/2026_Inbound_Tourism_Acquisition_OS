import pytest

from app.models.embed import RagSearchRequest
from app.services import rag_service
from app.config import Settings


@pytest.mark.asyncio
async def test_rag_search_vector_then_rerank(monkeypatch):
    captured: dict = {}

    async def fake_search(**kwargs):
        captured.update(kwargs)
        return [
            {
                "id": 10,
                "asset_id": 1,
                "chunk_index": 0,
                "chunk_text": "Dragon Journey Travel private China tours.",
                "score": 0.9,
            },
            {
                "id": 11,
                "asset_id": 1,
                "chunk_index": 1,
                "chunk_text": "Unrelated cooking recipes.",
                "score": 0.5,
            },
        ]

    async def fake_embed_query(q, s):
        return [0.1] * 1536

    async def fake_rerank(query, documents, top_k=3, settings=None):
        assert query == "China tour"
        assert len(documents) == 2
        assert top_k == 2
        return [(0, 0.95)]

    monkeypatch.setattr("app.services.rag_service.knowledge_repository.search_chunks", fake_search)
    monkeypatch.setattr("app.services.rag_service.embedding_service.embed_query", fake_embed_query)
    monkeypatch.setattr("app.services.rag_service.reranker_service.rerank", fake_rerank)

    data = await rag_service.search(
        RagSearchRequest(tenantId=1, projectId=2, query="China tour"),
        settings=Settings(embed_mock=True, reranker_mock=True, rag_vector_top_k=20),
    )
    assert captured["tenant_id"] == 1
    assert captured["project_id"] == 2
    assert captured["top_k"] == 20
    assert len(data.hits) == 1
    assert data.hits[0].chunk_id == 10
    assert data.hits[0].score == 0.95
