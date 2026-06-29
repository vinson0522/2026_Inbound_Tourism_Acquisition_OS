"""Tests for LiteLLM provider key routing."""

import pytest
from fastapi import HTTPException

from app.config import Settings
from app.services.llm_provider import (
    api_key_for_model,
    build_completion_kwargs,
    resolve_provider,
)

TOKEN = "test_internal_token"


@pytest.mark.parametrize(
    ("model", "expected"),
    [
        ("openai/gpt-4o-mini", "openai"),
        ("gemini/gemini-2.0-flash", "gemini"),
        ("perplexity/sonar-pro", "perplexity"),
    ],
)
def test_resolve_provider(model, expected):
    assert resolve_provider(model) == expected


def test_build_completion_kwargs_openai():
    settings = Settings(
        ai_service_internal_token=TOKEN,
        openai_api_key="sk-openai",
        openai_api_base="https://api.openai.com/v1",
    )
    kwargs = build_completion_kwargs("openai/gpt-4o-mini", [{"role": "user", "content": "hi"}], settings)
    assert kwargs["api_key"] == "sk-openai"
    assert kwargs["api_base"] == "https://api.openai.com/v1"


def test_build_completion_kwargs_gemini():
    settings = Settings(ai_service_internal_token=TOKEN, gemini_api_key="gem-key")
    kwargs = build_completion_kwargs("gemini/gemini-2.0-flash", [{"role": "user", "content": "hi"}], settings)
    assert kwargs["api_key"] == "gem-key"
    assert "api_base" not in kwargs


def test_build_completion_kwargs_perplexity():
    settings = Settings(ai_service_internal_token=TOKEN, perplexity_api_key="pplx-key")
    kwargs = build_completion_kwargs("perplexity/sonar-pro", [{"role": "user", "content": "hi"}], settings)
    assert kwargs["api_key"] == "pplx-key"
    assert "api_base" not in kwargs


def test_missing_key_raises_503():
    settings = Settings(ai_service_internal_token=TOKEN)
    with pytest.raises(HTTPException) as exc:
        build_completion_kwargs("perplexity/sonar-pro", [{"role": "user", "content": "hi"}], settings)
    assert exc.value.status_code == 503
    assert "PERPLEXITY_API_KEY" in str(exc.value.detail)


def test_api_key_for_model_routes_correctly():
    settings = Settings(
        ai_service_internal_token=TOKEN,
        openai_api_key="sk-o",
        gemini_api_key="sk-g",
        perplexity_api_key="sk-p",
    )
    assert api_key_for_model("openai/gpt-4o-mini", settings) == "sk-o"
    assert api_key_for_model("gemini/gemini-2.0-flash", settings) == "sk-g"
    assert api_key_for_model("perplexity/sonar-pro", settings) == "sk-p"
