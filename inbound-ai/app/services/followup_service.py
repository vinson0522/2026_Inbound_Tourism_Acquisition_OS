"""Lead follow-up script generation (FR-603 / EPIC-7 M3)."""

from __future__ import annotations

import json
import logging
import re
from typing import Any

from fastapi import HTTPException, status

from app.config import Settings, get_settings
from app.models.followup import FollowupGenerateData, FollowupGenerateRequest
from app.services import llm_gateway, template_service
from app.services.llm_provider import api_key_for_model

logger = logging.getLogger(__name__)

_JSON_SCHEMA_HINT = (
    '{"suggestion_en":"...","suggestion_zh":"...","needs_human_review":true}'
)

_FORBIDDEN_GUARANTEE_PATTERNS = (
    r"\bguaranteed\b",
    r"\b100%\s*(approved|approval)\b",
    r"保证.*?签证",
    r"一定.*?通过",
    r"承诺.*?价格",
)


def _mock_result(
    request: FollowupGenerateRequest,
    model: str | None,
) -> FollowupGenerateData:
    name = (request.name or "there").strip() or "there"
    topic = (request.keyword_text or "your China trip").strip()
    travel_hint = f" around {request.travel_date.isoformat()}" if request.travel_date else ""
    budget_hint = f" Their budget note: {request.budget}." if request.budget else ""
    message_hint = ""
    if request.message and request.message.strip():
        message_hint = f" They wrote: \"{request.message.strip()[:120]}\"."
    source_hint = f" Source channel: {request.source}." if request.source else ""

    suggestion_en = (
        f"Hi {name}, thanks for reaching out about {topic}{travel_hint}.{message_hint}{source_hint} "
        "I'd love to learn more about your interests and group size so we can tailor options. "
        "Pricing and visa requirements vary by itinerary — our team will confirm details after a quick call."
        f"{budget_hint}"
    )
    suggestion_zh = (
        f"您好 {name}，感谢您对{topic}的关注{travel_hint}。{message_hint}{source_hint} "
        "想进一步了解您的出行人数与偏好，以便为您定制方案。"
        "具体报价与签证要求因行程而异，需人工确认后再回复客户。"
        f"{budget_hint}"
    )
    return FollowupGenerateData(
        suggestion_en=suggestion_en,
        suggestion_zh=suggestion_zh,
        needs_human_review=True,
        model=model,
        capture_method="followup-mock",
    )


def _build_user_prompt(request: FollowupGenerateRequest) -> str:
    lines = [
        f"lead_id={request.lead_id}",
        f"project_id={request.project_id}",
        f"name={request.name or ''}",
        f"message={request.message or ''}",
        f"budget={request.budget or ''}",
        f"travel_date={request.travel_date.isoformat() if request.travel_date else ''}",
        f"source={request.source or ''}",
        f"keyword_text={request.keyword_text or ''}",
        f"json_schema={_JSON_SCHEMA_HINT}",
        "compliance: never guarantee price or visa outcomes; always set needs_human_review true.",
    ]
    return "\n".join(lines)


def _extract_json(content: str) -> dict[str, Any]:
    text = (content or "").strip()
    fence = re.search(r"```(?:json)?\s*([\s\S]*?)```", text)
    if fence:
        text = fence.group(1).strip()
    return json.loads(text)


def _assert_no_guarantees(text: str) -> None:
    lowered = text.lower()
    for pattern in _FORBIDDEN_GUARANTEE_PATTERNS:
        if re.search(pattern, lowered, flags=re.IGNORECASE):
            raise ValueError(f"follow-up text contains forbidden guarantee language: {pattern}")


def _parse_llm_followup(payload: dict[str, Any]) -> FollowupGenerateData:
    suggestion_en = str(
        payload.get("suggestion_en") or payload.get("suggestionEn") or ""
    ).strip()
    suggestion_zh = str(
        payload.get("suggestion_zh") or payload.get("suggestionZh") or ""
    ).strip()
    if not suggestion_en and not suggestion_zh:
        raise ValueError("LLM response missing suggestion_en and suggestion_zh")

    for text in (suggestion_en, suggestion_zh):
        if text:
            _assert_no_guarantees(text)

    return FollowupGenerateData(
        suggestion_en=suggestion_en or suggestion_zh,
        suggestion_zh=suggestion_zh or suggestion_en,
        needs_human_review=True,
        capture_method="llm",
    )


async def generate_followup(
    payload: FollowupGenerateRequest,
    settings: Settings | None = None,
) -> FollowupGenerateData:
    cfg = settings or get_settings()
    model = cfg.followup_model

    if cfg.followup_mock_llm:
        logger.warning("FOLLOWUP_MOCK_LLM enabled — returning canned follow-up result")
        return _mock_result(payload, model)

    if not api_key_for_model(model, cfg):
        logger.warning("No LLM key — falling back to follow-up mock (local dev)")
        return _mock_result(payload, model)

    prompt_template = await template_service.load_followup_generate_prompt(payload.tenant_id, cfg)
    user_prompt = _build_user_prompt(payload)
    messages = [
        {"role": "system", "content": prompt_template},
        {"role": "user", "content": user_prompt},
    ]

    logger.info(
        "followup generate tenant_id=%s project_id=%s lead_id=%s source=%s",
        payload.tenant_id,
        payload.project_id,
        payload.lead_id,
        payload.source,
    )

    response = await llm_gateway.acompletion(
        model,
        messages,
        cfg,
        max_tokens=2048,
        temperature=0.4,
        extra={"response_format": {"type": "json_object"}} if model.startswith("openai/") else None,
    )
    content = response.choices[0].message.content or ""
    try:
        parsed = _extract_json(content)
        result = _parse_llm_followup(parsed)
        result.model = response.model or model
        return result
    except (json.JSONDecodeError, ValueError, TypeError) as exc:
        logger.exception("followup LLM parse failed: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Failed to parse follow-up generation response: {exc}",
        ) from exc
