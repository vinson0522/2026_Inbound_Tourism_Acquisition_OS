"""LiteLLM gateway with GEO grounded-api enforcement."""

from __future__ import annotations

import logging
from typing import Any

import litellm
from fastapi import HTTPException, status

from app.config import Settings, get_settings
from app.models.llm import LlmCompleteData, LlmCompleteRequest
from app.services.llm_provider import build_completion_kwargs

logger = logging.getLogger(__name__)

PROBE_CONFIG_ERROR = (
    "GEO diagnostic requires grounded API: probe_mode=grounded-api "
    "requires grounding_enabled=true"
)


class ProbeConfigError(ValueError):
    """Raised when grounded-api probe is requested without grounding."""


def validate_probe_config(probe_mode: str, grounding_enabled: bool) -> None:
    if probe_mode == "grounded-api" and not grounding_enabled:
        raise ProbeConfigError(PROBE_CONFIG_ERROR)


def _configure_langfuse(settings: Settings) -> None:
    if settings.langfuse_public_key and settings.langfuse_secret_key and settings.langfuse_host:
        litellm.success_callback = ["langfuse"]
        litellm.failure_callback = ["langfuse"]
        logger.info("Langfuse callbacks enabled for LiteLLM")


def response_to_dict(response: Any) -> dict[str, Any]:
    if hasattr(response, "model_dump"):
        try:
            return response.model_dump()
        except Exception:
            pass
    return {}


async def acompletion(
    model: str,
    messages: list[dict[str, Any]],
    settings: Settings | None = None,
    *,
    max_tokens: int = 1024,
    temperature: float = 0.2,
    extra: dict[str, Any] | None = None,
) -> Any:
    """Call LiteLLM with provider-aware API key routing."""
    cfg = settings or get_settings()
    _configure_langfuse(cfg)
    kwargs = build_completion_kwargs(
        model,
        messages,
        cfg,
        max_tokens=max_tokens,
        temperature=temperature,
        extra=extra,
    )
    try:
        return await litellm.acompletion(**kwargs)
    except Exception as exc:
        logger.exception("LiteLLM completion failed model=%s", model)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"LLM provider error: {exc}",
        ) from exc


async def complete(
    request: LlmCompleteRequest,
    settings: Settings | None = None,
) -> LlmCompleteData:
    cfg = settings or get_settings()
    validate_probe_config(request.probe_mode, request.grounding_enabled)

    if not cfg.has_llm_key:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="No LLM API key configured (OPENAI_API_KEY / GEMINI_API_KEY / PERPLEXITY_API_KEY)",
        )

    if request.probe_mode == "grounded-api":
        logger.info(
            "grounded-api request model=%s grounding_enabled=%s",
            request.model,
            request.grounding_enabled,
        )

    messages = [m.model_dump() for m in request.messages]
    response = await acompletion(
        request.model,
        messages,
        cfg,
        max_tokens=request.max_tokens,
        temperature=request.temperature,
    )

    choice = response.choices[0]
    content = choice.message.content or ""
    usage = getattr(response, "usage", None)
    usage_dict = usage.model_dump() if hasattr(usage, "model_dump") else dict(usage) if usage else None

    return LlmCompleteData(
        model=response.model or request.model,
        content=content,
        usage=usage_dict,
        raw=response_to_dict(response),
    )
