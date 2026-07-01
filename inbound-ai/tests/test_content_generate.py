"""Tests for POST /ai/content/generate (FR-301/302)."""

import json

import pytest
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app
from app.models.content import ContentGenerateRequest
from app.services import content_service

TOKEN = "test_internal_token"


def _request_body(**overrides):
    body = {
        "tenantId": 1,
        "projectId": 1,
        "keywordId": 1,
        "keywordText": "Chongqing cyberpunk city tour",
        "platform": "tiktok",
        "durationSec": 30,
        "tone": "friendly",
        "language": "en",
        "targetMarket": "US",
        "useRag": False,
        "traceId": "content-test-1",
    }
    body.update(overrides)
    return body


@pytest.mark.asyncio
async def test_generate_mock_has_required_fields():
    settings = Settings(ai_service_internal_token=TOKEN, content_mock_llm=True)
    req = ContentGenerateRequest(**_request_body())
    data = await content_service.generate_content(req, settings=settings)
    assert data.needs_human_review is True
    assert data.capture_method == "content-mock"
    assert data.hook
    assert data.script
    assert data.voiceover
    assert data.on_screen_text
    assert data.cta
    assert len(data.storyboard_json) >= 3
    for scene in data.storyboard_json:
        assert scene.scene >= 1
        assert scene.duration >= 1
        assert scene.visual


@pytest.mark.asyncio
async def test_generate_mock_no_key_fallback():
    settings = Settings(ai_service_internal_token=TOKEN, content_mock_llm=False)
    req = ContentGenerateRequest(**_request_body(platform="youtube_shorts", durationSec=15))
    data = await content_service.generate_content(req, settings=settings)
    assert data.capture_method == "content-mock"
    assert len(data.storyboard_json) == 3


@pytest.mark.asyncio
async def test_generate_llm_parsed(monkeypatch):
    llm_json = {
        "title": "Chongqing After Dark",
        "hook": "This city looks like sci-fi at night.",
        "target_audience": "US first-time China travelers",
        "script": "Scene 1: skyline hook. Scene 2: private guide intro. Scene 3: CTA.",
        "voiceover": "Skip the crowds. Explore Chongqing with a private English-speaking guide.",
        "on_screen_text": "Chongqing cyberpunk tour\nPrivate · English guide",
        "cta": "Comment TOUR for a free itinerary PDF.",
        "hashtags": "#Chongqing #ChinaTravel",
        "landing_page_suggestion": "Landing page hero with 3-day Chongqing module.",
        "storyboard": [
            {"scene": 1, "duration": 3, "visual": "Neon skyline drone shot", "note": "Hook"},
            {"scene": 2, "duration": 8, "visual": "Guide greeting at hotel lobby", "note": "Trust"},
            {"scene": 3, "duration": 4, "visual": "CTA end card", "note": "Convert"},
        ],
        "chunk_ids": [5],
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
        return "content template placeholder"

    monkeypatch.setattr(content_service.llm_gateway, "acompletion", fake_acompletion)
    monkeypatch.setattr(content_service.template_service, "load_content_script_prompt", fake_prompt)

    settings = Settings(
        ai_service_internal_token=TOKEN,
        openai_api_key="sk-test",
        content_mock_llm=False,
    )
    req = ContentGenerateRequest(**_request_body())
    data = await content_service.generate_content(req, settings=settings)
    assert data.capture_method == "llm"
    assert data.hook == "This city looks like sci-fi at night."
    assert data.chunk_ids == [5]
    assert len(data.storyboard_json) == 3
    assert data.storyboard_json[0].visual == "Neon skyline drone shot"


def test_endpoint_requires_token():
    app = create_app()
    client = TestClient(app)
    resp = client.post("/ai/content/generate", json=_request_body())
    assert resp.status_code == 401


def test_endpoint_mock_success():
    app = create_app()
    client = TestClient(app)
    resp = client.post(
        "/ai/content/generate",
        json=_request_body(durationSec=60),
        headers={"Authorization": f"Bearer {TOKEN}"},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 0
    data = body["data"]
    assert data["needs_human_review"] is True
    assert data["hook"]
    assert data["script"]
    assert data["voiceover"]
    assert data["on_screen_text"]
    assert data["cta"]
    assert len(data["storyboard_json"]) >= 4


def test_normalize_platform_aliases():
    assert content_service.normalize_platform("TikTok") == "tiktok"
    assert content_service.normalize_platform("YouTube Shorts") == "youtube_shorts"
