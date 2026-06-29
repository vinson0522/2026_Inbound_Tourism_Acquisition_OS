import pytest
from fastapi import HTTPException
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app
from app.models.llm import ChatMessage, LlmCompleteRequest
from app.services.llm_gateway import (
    PROBE_CONFIG_ERROR,
    ProbeConfigError,
    complete,
    validate_probe_config,
)

TOKEN = "test_internal_token"


def test_validate_probe_config_rejects_grounded_without_flag():
    with pytest.raises(ProbeConfigError, match="grounding_enabled=true"):
        validate_probe_config("grounded-api", False)


def test_validate_probe_config_allows_grounded_with_flag():
    validate_probe_config("grounded-api", True)


def test_validate_probe_config_allows_chat_without_flag():
    validate_probe_config("chat", False)


@pytest.mark.asyncio
async def test_complete_no_api_key_returns_503():
    settings = Settings(ai_service_internal_token=TOKEN)
    req = LlmCompleteRequest(
        model="openai/gpt-4o-mini",
        messages=[ChatMessage(role="user", content="ping")],
        probe_mode="chat",
    )
    with pytest.raises(HTTPException) as exc:
        await complete(req, settings=settings)
    assert exc.value.status_code == 503


@pytest.mark.asyncio
async def test_complete_grounded_rejected_before_llm():
    settings = Settings(ai_service_internal_token=TOKEN, openai_api_key="sk-test")
    req = LlmCompleteRequest(
        model="perplexity/sonar-pro",
        messages=[ChatMessage(role="user", content="ping")],
        probe_mode="grounded-api",
        grounding_enabled=False,
    )
    with pytest.raises(ProbeConfigError):
        await complete(req, settings=settings)


def test_llm_complete_endpoint_grounded_rejected():
    client = TestClient(create_app())
    resp = client.post(
        "/ai/llm/complete",
        headers={"Authorization": f"Bearer {TOKEN}"},
        json={
            "model": "perplexity/sonar-pro",
            "messages": [{"role": "user", "content": "ping"}],
            "probe_mode": "grounded-api",
            "grounding_enabled": False,
            "max_tokens": 16,
        },
    )
    assert resp.status_code == 400
    body = resp.json()
    assert body["code"] == 40001
    assert PROBE_CONFIG_ERROR in body["message"]


@pytest.mark.asyncio
async def test_complete_success_mocked(monkeypatch):
    class FakeMessage:
        content = "pong"

    class FakeChoice:
        message = FakeMessage()

    class FakeUsage:
        def model_dump(self):
            return {"total_tokens": 5}

    class FakeResponse:
        model = "openai/gpt-4o-mini"
        choices = [FakeChoice()]
        usage = FakeUsage()

        def model_dump(self):
            return {"model": self.model}

    async def fake_acompletion(**_kwargs):
        return FakeResponse()

    import litellm

    monkeypatch.setattr(litellm, "acompletion", fake_acompletion)

    settings = Settings(ai_service_internal_token=TOKEN, openai_api_key="sk-test")
    req = LlmCompleteRequest(
        model="openai/gpt-4o-mini",
        messages=[ChatMessage(role="user", content="ping")],
        max_tokens=16,
    )
    result = await complete(req, settings=settings)
    assert result.content == "pong"
    assert result.model == "openai/gpt-4o-mini"
