"""LiteLLM provider resolution and API key routing."""

from __future__ import annotations

from typing import Any

from fastapi import HTTPException, status

from app.config import Settings

OPENAI_PREFIXES = ("openai/", "gpt-", "o1", "o3")
GEMINI_PREFIXES = ("gemini/", "google/")
PERPLEXITY_PREFIXES = ("perplexity/", "pplx/")


def resolve_provider(model: str) -> str:
    lower = model.lower()
    if lower.startswith(PERPLEXITY_PREFIXES):
        return "perplexity"
    if lower.startswith(GEMINI_PREFIXES):
        return "gemini"
    if lower.startswith(OPENAI_PREFIXES):
        return "openai"
    if "/" in model:
        return model.split("/", 1)[0].lower()
    return "openai"


def api_key_for_provider(provider: str, settings: Settings) -> str | None:
    if provider == "openai":
        return settings.openai_api_key
    if provider == "gemini":
        return settings.gemini_api_key
    if provider == "perplexity":
        return settings.perplexity_api_key
    return None


def api_key_for_model(model: str, settings: Settings) -> str | None:
    return api_key_for_provider(resolve_provider(model), settings)


def require_api_key_for_model(model: str, settings: Settings) -> str:
    provider = resolve_provider(model)
    key = api_key_for_provider(provider, settings)
    if not key:
        env_name = {
            "openai": "OPENAI_API_KEY",
            "gemini": "GEMINI_API_KEY",
            "perplexity": "PERPLEXITY_API_KEY",
        }.get(provider, "LLM API key")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"No API key configured for {provider} model {model} ({env_name})",
        )
    return key


def build_completion_kwargs(
    model: str,
    messages: list[dict[str, Any]],
    settings: Settings,
    *,
    max_tokens: int = 1024,
    temperature: float = 0.2,
    extra: dict[str, Any] | None = None,
) -> dict[str, Any]:
    provider = resolve_provider(model)
    api_key = require_api_key_for_model(model, settings)
    kwargs: dict[str, Any] = {
        "model": model,
        "messages": messages,
        "max_tokens": max_tokens,
        "temperature": temperature,
        "api_key": api_key,
    }
    if provider == "openai" and settings.openai_api_base:
        kwargs["api_base"] = settings.openai_api_base.rstrip("/")
    if extra:
        kwargs.update(extra)
    return kwargs
