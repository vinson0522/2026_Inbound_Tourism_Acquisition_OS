"""Tests for POST /ai/landing/generate (FR-502~505)."""

import json

import pytest
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app
from app.models.landing import LANDING_MODULE_KEYS
from app.models.landing import LandingGenerateRequest
from app.services import landing_service

TOKEN = "test_internal_token"


def _request_body(**overrides):
    body = {
        "tenantId": 1,
        "projectId": 1,
        "keywordId": 1,
        "keywordText": "Chongqing cyberpunk city tour",
        "templateType": "destination",
        "language": "en",
        "targetMarket": "US",
        "useRag": False,
        "traceId": "landing-test-1",
    }
    body.update(overrides)
    return body


def _llm_modules_payload():
    modules = []
    for key in LANDING_MODULE_KEYS:
        if key == "hero":
            content = {
                "headline": "Chongqing After Dark",
                "subtitle": "Private neon-city tour for US travelers",
                "cta_text": "Get itinerary",
                "image_hint": "Neon skyline",
            }
        elif key == "itinerary":
            content = {
                "headline": "3-day plan",
                "days": [{"day": 1, "title": "Arrival", "highlights": ["Pickup"]}],
            }
        elif key == "faq":
            content = {
                "headline": "FAQ",
                "items": [{"question": "Visa?", "answer": "Check embassy."}],
            }
        else:
            content = {"headline": f"Module {key}"}
        modules.append({"key": key, "content": content})
    return modules


@pytest.mark.asyncio
async def test_generate_mock_has_required_module_keys():
    settings = Settings(ai_service_internal_token=TOKEN, landing_mock_llm=True)
    req = LandingGenerateRequest(**_request_body())
    data = await landing_service.generate_landing(req, settings=settings)
    assert data.needs_human_review is True
    assert data.capture_method == "landing-mock"
    assert data.title
    assert data.seo_meta_json.title
    assert data.seo_meta_json.description
    assert data.form_config_json.fields
    module_keys = [m.key for m in data.content_json.modules]
    assert module_keys == list(LANDING_MODULE_KEYS)
    for module in data.content_json.modules:
        assert module.content


@pytest.mark.asyncio
async def test_generate_mock_no_key_fallback():
    settings = Settings(ai_service_internal_token=TOKEN, landing_mock_llm=False)
    req = LandingGenerateRequest(**_request_body(templateType="route"))
    data = await landing_service.generate_landing(req, settings=settings)
    assert data.capture_method == "landing-mock"
    assert len(data.content_json.modules) == len(LANDING_MODULE_KEYS)


@pytest.mark.asyncio
async def test_generate_llm_parsed(monkeypatch):
    llm_json = {
        "title": "Chongqing Cyberpunk Private Tour",
        "content_json": {"modules": _llm_modules_payload()},
        "seo_meta_json": {
            "title": "Chongqing Cyberpunk Tour | Private China Travel",
            "description": "Book a private Chongqing neon-city tour for US travelers.",
            "h1": "Chongqing Cyberpunk City Tour",
            "faq_schema": [],
        },
        "form_config_json": {
            "fields": ["name", "email", "phone", "travel_date", "party_size", "budget", "message"],
            "submit_label": "Request quote",
            "whatsapp_link": "https://wa.me/8613800138000",
        },
        "chunk_ids": [7],
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
        return "landing template placeholder"

    monkeypatch.setattr(landing_service.llm_gateway, "acompletion", fake_acompletion)
    monkeypatch.setattr(landing_service.template_service, "load_landing_generate_prompt", fake_prompt)

    settings = Settings(
        ai_service_internal_token=TOKEN,
        openai_api_key="sk-test",
        landing_mock_llm=False,
    )
    req = LandingGenerateRequest(**_request_body())
    data = await landing_service.generate_landing(req, settings=settings)
    assert data.capture_method == "llm"
    assert data.chunk_ids == [7]
    assert [m.key for m in data.content_json.modules] == list(LANDING_MODULE_KEYS)
    hero = next(m for m in data.content_json.modules if m.key == "hero")
    assert hero.content["headline"] == "Chongqing After Dark"


def test_endpoint_requires_token():
    app = create_app()
    client = TestClient(app)
    resp = client.post("/ai/landing/generate", json=_request_body())
    assert resp.status_code == 401


def test_endpoint_mock_success():
    app = create_app()
    client = TestClient(app)
    resp = client.post(
        "/ai/landing/generate",
        json=_request_body(templateType="theme"),
        headers={"Authorization": f"Bearer {TOKEN}"},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 0
    data = body["data"]
    assert data["needs_human_review"] is True
    assert data["title"]
    assert data["seo_meta_json"]["title"]
    assert data["form_config_json"]["fields"]
    module_keys = [m["key"] for m in data["content_json"]["modules"]]
    assert module_keys == list(LANDING_MODULE_KEYS)


def test_normalize_template_type_aliases():
    assert landing_service.normalize_template_type("Destination") == "destination"
    assert landing_service.normalize_template_type("visa_policy") == "visa"
