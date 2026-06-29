"""Tests for POST /ai/diagnose grounded-api flow."""

import pytest
from fastapi import HTTPException
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app
from app.models.diagnostic import DiagnoseRequest
from app.services.diagnose_service import run_diagnose
from app.services.llm_gateway import ProbeConfigError

TOKEN = "test_internal_token"


def _diagnose_body(**overrides):
    body = {
        "run_id": 1,
        "question_id": 1,
        "tenant_id": 1,
        "project_id": 1,
        "platform": "perplexity",
        "probe_mode": "grounded-api",
        "grounding_enabled": True,
        "question": "Can you recommend reliable China travel agencies?",
        "model": "perplexity/sonar-pro",
        "region": "us-east",
        "locale": "en-US",
        "sample_index": 0,
    }
    body.update(overrides)
    return body


@pytest.mark.asyncio
async def test_run_diagnose_no_api_key_503():
    settings = Settings(ai_service_internal_token=TOKEN, openai_api_key="sk-test")
    req = DiagnoseRequest(**_diagnose_body())
    with pytest.raises(HTTPException) as exc:
        await run_diagnose(req, settings=settings)
    assert exc.value.status_code == 503
    assert "perplexity" in str(exc.value.detail).lower()


@pytest.mark.asyncio
async def test_run_diagnose_grounded_rejected():
    settings = Settings(ai_service_internal_token=TOKEN, perplexity_api_key="pplx-test")
    req = DiagnoseRequest(**_diagnose_body(grounding_enabled=False))
    with pytest.raises(ProbeConfigError):
        await run_diagnose(req, settings=settings)


@pytest.mark.asyncio
async def test_run_diagnose_success_mocked(monkeypatch):
    class FakeMessage:
        content = "China Highlights is recommended."

    class FakeChoice:
        message = FakeMessage()

    class FakeResponse:
        model = "perplexity/sonar-pro"
        choices = [FakeChoice()]

        def model_dump(self):
            return {
                "model": self.model,
                "choices": [{"message": {"content": self.choices[0].message.content}}],
                "citations": ["https://www.chinahighlights.com/"],
            }

    async def fake_acompletion(*_args, **_kwargs):
        return FakeResponse()

    import app.services.llm_gateway as gw

    monkeypatch.setattr(gw, "acompletion", fake_acompletion)

    settings = Settings(ai_service_internal_token=TOKEN, perplexity_api_key="pplx-test")
    req = DiagnoseRequest(**_diagnose_body(customer_brand="China Highlights"))
    result = await run_diagnose(req, settings=settings)
    assert result.probe_mode == "grounded-api"
    assert result.platform == "perplexity"
    assert "China Highlights" in result.answer_text
    assert len(result.citations) >= 1


def test_diagnose_endpoint_grounded_rejected():
    client = TestClient(create_app())
    resp = client.post(
        "/ai/diagnose",
        headers={"Authorization": f"Bearer {TOKEN}"},
        json=_diagnose_body(grounding_enabled=False),
    )
    assert resp.status_code == 400
    assert resp.json()["code"] == 40001


def test_diagnose_endpoint_success_mocked(monkeypatch):
    class FakeMessage:
        content = "Answer with citations."

    class FakeChoice:
        message = FakeMessage()

    class FakeResponse:
        model = "perplexity/sonar-pro"
        choices = [FakeChoice()]

        def model_dump(self):
            return {"choices": [{"message": {"content": "Answer"}}], "citations": []}

    async def fake_acompletion(*_args, **_kwargs):
        return FakeResponse()

    import app.services.llm_gateway as gw

    monkeypatch.setattr(gw, "acompletion", fake_acompletion)

    from app.config import get_settings

    get_settings.cache_clear()
    monkeypatch.setenv("PERPLEXITY_API_KEY", "pplx-test")

    client = TestClient(create_app())
    resp = client.post(
        "/ai/diagnose",
        headers={"Authorization": f"Bearer {TOKEN}"},
        json=_diagnose_body(),
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 0
    assert body["data"]["probe_mode"] == "grounded-api"


def test_parse_citations_endpoint():
    client = TestClient(create_app())
    resp = client.post(
        "/ai/parse-citations",
        headers={"Authorization": f"Bearer {TOKEN}"},
        json={
            "platform": "perplexity",
            "raw_response_json": {
                "choices": [{"message": {"content": "China Highlights"}}],
                "citations": ["https://www.chinahighlights.com/"],
            },
            "customer_brand": "China Highlights",
        },
    )
    assert resp.status_code == 200
    assert len(resp.json()["data"]["citations"]) == 1


def test_score_endpoint():
    client = TestClient(create_app())
    resp = client.post(
        "/ai/score",
        headers={"Authorization": f"Bearer {TOKEN}"},
        json={
            "run_id": 1,
            "metric_weights_json": {
                "brand_mention_rate": 1.0,
                "top3_rate": 0.0,
                "competitor_suppression": 0.0,
                "citation_coverage": 0.0,
                "longtail_coverage": 0.0,
                "asset_completeness": 0.0,
            },
            "results": [
                {
                    "question_id": 1,
                    "brand_mentioned": True,
                    "in_top3": True,
                    "competitor_dominance": 0.0,
                    "citation_hit": True,
                    "is_longtail": False,
                    "asset_complete": 1.0,
                }
            ],
        },
    )
    assert resp.status_code == 200
    assert resp.json()["data"]["geo_score"] == 100.0
