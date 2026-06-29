import pytest

from app.models.embed import RagSearchRequest
from app.services import rag_service
from app.config import Settings


@pytest.mark.asyncio
async def test_rag_search_requires_tenant_and_project(monkeypatch):
    async def fake_search(**kwargs):
        assert kwargs["tenant_id"] == 1
        assert kwargs["project_id"] == 2
        return []

    monkeypatch.setattr(
        "app.services.rag_service.knowledge_repository.search_chunks",
        fake_search,
    )
    async def fake_embed_query(q, s):
        return [0.1, 0.2, 0.3]

    monkeypatch.setattr(
        "app.services.rag_service.embedding_service.embed_query",
        fake_embed_query,
    )
    data = await rag_service.search(
        RagSearchRequest(tenantId=1, projectId=2, query="China tour"),
        settings=Settings(embed_mock=True),
    )
    assert data.hits == []
