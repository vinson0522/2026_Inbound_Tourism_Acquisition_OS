"""Tests for POST /ai/followup/generate (FR-603 / EPIC-7 M3)."""

from __future__ import annotations

import json

import pytest
from fastapi.testclient import TestClient
from httpx import ASGITransport, AsyncClient

from app.config import Settings
from app.main import app, create_app
from app.models.followup import FollowupGenerateRequest
from app.services import followup_service

TOKEN = "test_internal_token"


def _request_body(**overrides):
    body = {
        "tenantId": 1,
        "projectId": 1,
        "leadId": 1,
        "name": "Alex",
        "message": "Private tour inquiry",
        "keywordText": "Great Wall private tour",
        "traceId": "test-followup-en",
    }
    body.update(overrides)
    return body


@pytest.fixture
def auth_headers() -> dict[str, str]:
    return {"Authorization": f"Bearer {TOKEN}"}


@pytest.mark.asyncio
async def test_generate_mock_has_bilingual_fields():
    settings = Settings(ai_service_internal_token=TOKEN, followup_mock_llm=True)
    req = FollowupGenerateRequest(**_request_body())
    data = await followup_service.generate_followup(req, settings=settings)
    assert data.needs_human_review is True
    assert data.capture_method == "followup-mock"
    assert data.suggestion_en
    assert data.suggestion_zh
    assert "Alex" in data.suggestion_en
    assert "Alex" in data.suggestion_zh
    assert "Private tour inquiry" in data.suggestion_en


@pytest.mark.asyncio
async def test_generate_mock_no_key_fallback():
    settings = Settings(ai_service_internal_token=TOKEN, followup_mock_llm=False)
    req = FollowupGenerateRequest(**_request_body(source="whatsapp"))
    data = await followup_service.generate_followup(req, settings=settings)
    assert data.capture_method == "followup-mock"
    assert "whatsapp" in data.suggestion_en


@pytest.mark.asyncio
async def test_generate_llm_parsed(monkeypatch: pytest.MonkeyPatch):
    llm_json = {
        "suggestion_en": (
            "Hi Alex, thanks for your interest in the Great Wall private tour. "
            "Could you share your travel dates and group size? "
            "Final pricing depends on itinerary — our team will confirm after review."
        ),
        "suggestion_zh": (
            "您好 Alex，感谢您对长城私人游的关注。"
            "请告知出行日期与人数；最终报价需人工确认。"
        ),
        "needs_human_review": True,
    }

    class FakeMessage:
        content = json.dumps(llm_json)

    class FakeChoice:
        message = FakeMessage()

    class FakeResponse:
        model = "openai/gpt-4o-mini"
        choices = [FakeChoice()]

    async def fake_acompletion(*_args, **_kwargs):
        return FakeResponse()

    async def fake_prompt(_tenant_id, _settings):
        return "followup template placeholder"

    monkeypatch.setattr(followup_service.llm_gateway, "acompletion", fake_acompletion)
    monkeypatch.setattr(followup_service.template_service, "load_followup_generate_prompt", fake_prompt)

    settings = Settings(
        ai_service_internal_token=TOKEN,
        openai_api_key="sk-test",
        followup_mock_llm=False,
    )
    req = FollowupGenerateRequest(**_request_body())
    data = await followup_service.generate_followup(req, settings=settings)
    assert data.capture_method == "llm"
    assert "Great Wall" in data.suggestion_en
    assert data.needs_human_review is True


@pytest.mark.asyncio
async def test_followup_generate_mock_en(auth_headers: dict[str, str], monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("FOLLOWUP_MOCK_LLM", "true")
    from app.config import get_settings

    get_settings.cache_clear()

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post(
            "/ai/followup/generate",
            json=_request_body(),
            headers=auth_headers,
        )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 0
    data = body["data"]
    assert data["suggestionEn"]
    assert data["suggestionZh"]
    assert "Alex" in data["suggestionEn"]
    assert data["needsHumanReview"] is True


@pytest.mark.asyncio
async def test_followup_generate_mock_zh(auth_headers: dict[str, str], monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("FOLLOWUP_MOCK_LLM", "true")
    from app.config import get_settings

    get_settings.cache_clear()

    payload = _request_body(
        leadId=2,
        name="李明",
        budget="USD 4000",
        travelDate="2026-09-15",
        source="whatsapp",
        message="想咨询九月私人团",
        keywordText="",
        traceId="test-followup-zh",
    )
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post("/ai/followup/generate", json=payload, headers=auth_headers)
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 0
    data = body["data"]
    assert data["suggestionZh"]
    assert "李明" in data["suggestionZh"]
    assert "2026-09-15" in data["suggestionZh"]
    assert data["needsHumanReview"] is True


def test_endpoint_requires_token():
    client = TestClient(create_app())
    resp = client.post("/ai/followup/generate", json=_request_body())
    assert resp.status_code == 401


def test_mock_avoids_price_visa_guarantees():
    settings = Settings(ai_service_internal_token=TOKEN, followup_mock_llm=True)
    req = FollowupGenerateRequest(**_request_body(budget="USD 5000"))
    import asyncio

    data = asyncio.run(followup_service.generate_followup(req, settings=settings))
    combined = f"{data.suggestion_en} {data.suggestion_zh}".lower()
    assert "guaranteed" not in combined
    assert "保证" not in data.suggestion_zh or "需人工确认" in data.suggestion_zh
