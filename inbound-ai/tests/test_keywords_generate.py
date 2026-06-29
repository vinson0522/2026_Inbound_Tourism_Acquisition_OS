"""Tests for POST /ai/keywords/generate (FR-201)."""

import json

import pytest
from fastapi import HTTPException
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app
from app.models.keywords import KeywordGenerateRequest, LIFECYCLE_STAGES
from app.services import keywords_service

TOKEN = "test_internal_token"


def _request_body(**overrides):
    body = {
        "tenantId": 1,
        "projectId": 1,
        "market": "US",
        "locale": "en",
        "wordsPerStage": 3,
        "useRag": False,
        "traceId": "kw-test-1",
    }
    body.update(overrides)
    return body


@pytest.mark.asyncio
async def test_generate_mock_returns_all_default_stages():
    settings = Settings(ai_service_internal_token=TOKEN, keywords_mock_llm=True)
    req = KeywordGenerateRequest(**_request_body())
    data = await keywords_service.generate_keywords(req, settings=settings)
    assert data.needs_human_review is True
    assert data.capture_method == "keywords-mock"
    assert len(data.stages) == len(LIFECYCLE_STAGES)
    stage_keys = {s.stage for s in data.stages}
    assert stage_keys == set(LIFECYCLE_STAGES)
    for stage in data.stages:
        assert len(stage.keywords) == 3
        for kw in stage.keywords:
            assert kw.text
            assert kw.needs_human_review is True
            assert kw.suggested_score is None


@pytest.mark.asyncio
async def test_generate_mock_no_key_fallback():
    settings = Settings(ai_service_internal_token=TOKEN, keywords_mock_llm=False)
    req = KeywordGenerateRequest(**_request_body(stages=["INSPIRATION", "planning"]))
    data = await keywords_service.generate_keywords(req, settings=settings)
    assert data.capture_method == "keywords-mock"
    assert {s.stage for s in data.stages} == {"inspiration", "planning"}


@pytest.mark.asyncio
async def test_generate_llm_parsed(monkeypatch):
    llm_json = {
        "stages": [
            {
                "stage": "inspiration",
                "keywords": [
                    {"text": "best China destinations first trip", "rationale": "Top of funnel"},
                    {"text": "hidden gems China travel", "rationale": "Discovery intent"},
                ],
            },
            {
                "stage": "planning",
                "keywords": [
                    {"text": "10 day China itinerary", "rationale": "Planning phase"},
                ],
            },
        ]
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
        return "short template placeholder"

    monkeypatch.setattr(keywords_service.llm_gateway, "acompletion", fake_acompletion)
    monkeypatch.setattr(keywords_service.template_service, "load_keyword_generate_prompt", fake_prompt)

    settings = Settings(
        ai_service_internal_token=TOKEN,
        openai_api_key="sk-test",
        keywords_mock_llm=False,
    )
    req = KeywordGenerateRequest(**_request_body(stages=["inspiration", "planning"], wordsPerStage=2))
    data = await keywords_service.generate_keywords(req, settings=settings)
    assert data.capture_method == "llm"
    assert len(data.stages) == 2
    assert data.stages[0].keywords[0].text == "best China destinations first trip"


@pytest.mark.asyncio
async def test_unknown_stage_400():
    settings = Settings(ai_service_internal_token=TOKEN, keywords_mock_llm=True)
    req = KeywordGenerateRequest(**_request_body(stages=["invalid_stage"]))
    with pytest.raises(HTTPException) as exc:
        await keywords_service.generate_keywords(req, settings=settings)
    assert exc.value.status_code == 400


def test_endpoint_requires_token():
    app = create_app()
    client = TestClient(app)
    resp = client.post("/ai/keywords/generate", json=_request_body())
    assert resp.status_code == 401


def test_endpoint_mock_success():
    app = create_app()
    client = TestClient(app)
    resp = client.post(
        "/ai/keywords/generate",
        json=_request_body(stages=["inspiration"], wordsPerStage=2),
        headers={"Authorization": f"Bearer {TOKEN}"},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 0
    data = body["data"]
    assert data["needs_human_review"] is True
    assert len(data["stages"]) == 1
    assert data["stages"][0]["stage"] == "inspiration"
    assert len(data["stages"][0]["keywords"]) == 2


def test_normalize_stage_aliases():
    assert keywords_service.normalize_stage("INSPIRATION") == "inspiration"
    assert keywords_service.normalize_stage("灵感") == "inspiration"
    assert keywords_service.normalize_stage("repurchase") == "repurchase"
