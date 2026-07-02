"""Tests for POST /ai/keywords/score (FR-203 / ADR-19)."""

import json

import pytest
from fastapi import HTTPException
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app
from app.models.keywords import KeywordScoreRequest, SCORE_DIMENSIONS
from app.services import keywords_service, template_service

TOKEN = "test_internal_token"


def _score_body(**overrides):
    body = {
        "tenantId": 1,
        "projectId": 1,
        "keywordId": 42,
        "keyword": "10 day China itinerary first timers",
        "keywordEn": "10 day China itinerary first timers",
        "stage": "planning",
        "market": "US",
        "useRag": False,
        "traceId": "kw-score-test-1",
    }
    body.update(overrides)
    return body


@pytest.mark.asyncio
async def test_score_mock_deterministic():
    settings = Settings(ai_service_internal_token=TOKEN, keyword_score_mock_llm=True)
    req = KeywordScoreRequest(**_score_body())
    data = await keywords_service.score_keyword(req, settings=settings)
    assert data.needs_human_review is False
    assert data.capture_method == "keyword-score-mock"
    assert 0 <= data.score <= 100
    detail = data.score_detail
    assert detail.weights_version == "keyword_score_v1"
    for key in SCORE_DIMENSIONS:
        val = getattr(detail, key)
        assert 0 <= val <= 100

    repeat = await keywords_service.score_keyword(req, settings=settings)
    assert repeat.score == data.score
    assert repeat.score_detail.relevance == detail.relevance


@pytest.mark.asyncio
async def test_score_mock_with_geo_score_input():
    settings = Settings(ai_service_internal_token=TOKEN, keyword_score_mock_llm=True)
    req = KeywordScoreRequest(**_score_body(geoScore=85))
    data = await keywords_service.score_keyword(req, settings=settings)
    assert data.score_detail.geo_score_input == 85
    assert data.score_detail.competitive_pressure == pytest.approx(86.2, abs=0.1)


def test_compute_weighted_score_boundaries():
    weights = {
        "relevance": 0.30,
        "long_tail_value": 0.20,
        "producibility": 0.20,
        "landing_value": 0.15,
        "competitive_pressure": 0.15,
    }
    all_hundred = {k: 100.0 for k in SCORE_DIMENSIONS}
    assert keywords_service.compute_weighted_score(all_hundred, weights) == 100.0

    all_zero = {k: 0.0 for k in SCORE_DIMENSIONS}
    assert keywords_service.compute_weighted_score(all_zero, weights) == 0.0

    mixed = {
        "relevance": 80.0,
        "long_tail_value": 60.0,
        "producibility": 70.0,
        "landing_value": 50.0,
        "competitive_pressure": 40.0,
    }
    assert keywords_service.compute_weighted_score(mixed, weights) == 63.5


@pytest.mark.asyncio
async def test_score_unknown_stage_400():
    settings = Settings(ai_service_internal_token=TOKEN, keyword_score_mock_llm=True)
    req = KeywordScoreRequest(**_score_body(stage="not_a_stage"))
    with pytest.raises(HTTPException) as exc:
        await keywords_service.score_keyword(req, settings=settings)
    assert exc.value.status_code == 400


def test_score_missing_keyword_422():
    app = create_app()
    client = TestClient(app)
    body = _score_body()
    body["keyword"] = ""
    resp = client.post(
        "/ai/keywords/score",
        json=body,
        headers={"Authorization": f"Bearer {TOKEN}"},
    )
    assert resp.status_code == 422


@pytest.mark.asyncio
async def test_score_llm_parsed(monkeypatch):
    llm_json = {
        "relevance": 82,
        "long_tail_value": 75,
        "producibility": 88,
        "landing_value": 70,
        "competitive_pressure": 65,
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

    async def fake_template(_tenant_id, _settings):
        return template_service.KeywordScoreTemplateConfig(
            prompt="score template",
            weights={
                "relevance": 0.30,
                "long_tail_value": 0.20,
                "producibility": 0.20,
                "landing_value": 0.15,
                "competitive_pressure": 0.15,
            },
            weights_version="keyword_score_v1",
        )

    monkeypatch.setattr(keywords_service.llm_gateway, "acompletion", fake_acompletion)
    monkeypatch.setattr(keywords_service.template_service, "load_keyword_score_template", fake_template)

    settings = Settings(
        ai_service_internal_token=TOKEN,
        openai_api_key="sk-test",
        keyword_score_mock_llm=False,
    )
    req = KeywordScoreRequest(**_score_body(geoScore=85))
    data = await keywords_service.score_keyword(req, settings=settings)
    assert data.capture_method == "llm"
    assert data.score == pytest.approx(77.5, abs=0.1)
    assert data.score_detail.relevance == 82.0
    assert data.score_detail.geo_score_input == 85


def test_endpoint_requires_token():
    app = create_app()
    client = TestClient(app)
    resp = client.post("/ai/keywords/score", json=_score_body())
    assert resp.status_code == 401


def test_endpoint_mock_success():
    app = create_app()
    client = TestClient(app)
    resp = client.post(
        "/ai/keywords/score",
        json=_score_body(),
        headers={"Authorization": f"Bearer {TOKEN}"},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 0
    data = body["data"]
    assert data["needs_human_review"] is False
    assert "score" in data
    assert data["score_detail"]["weights_version"] == "keyword_score_v1"
