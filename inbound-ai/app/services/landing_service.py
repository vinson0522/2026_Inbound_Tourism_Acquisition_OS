"""Landing page copy generation (FR-502~505 / EPIC-6 M1)."""

from __future__ import annotations

import json
import logging
import re
from typing import Any

from fastapi import HTTPException, status

from app.config import Settings, get_settings
from app.models.embed import RagSearchRequest
from app.models.landing import (
    LANDING_MODULE_KEYS,
    SUPPORTED_TEMPLATE_TYPES,
    FormConfigJson,
    LandingContentJson,
    LandingGenerateData,
    LandingGenerateRequest,
    LandingModule,
    SeoMetaJson,
)
from app.services import llm_gateway, rag_service, template_service
from app.services.llm_provider import api_key_for_model

logger = logging.getLogger(__name__)

_JSON_SCHEMA_HINT = (
    '{"title":"...","content_json":{"modules":['
    '{"key":"hero","content":{"headline":"...","subtitle":"...","cta_text":"...","image_hint":"..."}},'
    '{"key":"why_this_trip","content":{"headline":"...","bullets":["..."]}},'
    '{"key":"itinerary","content":{"days":[{"day":1,"title":"...","highlights":["..."]}]}},'
    '{"key":"what_we_provide","content":{"items":[{"title":"...","description":"..."}]}},'
    '{"key":"traveler_reviews","content":{"reviews":[{"name":"...","country":"...","rating":5,"quote":"..."}]}},'
    '{"key":"faq","content":{"items":[{"question":"...","answer":"..."}]}},'
    '{"key":"lead_form","content":{"headline":"...","subheadline":"..."}},'
    '{"key":"whatsapp_cta","content":{"headline":"...","button_label":"..."}}'
    ']},"seo_meta_json":{"title":"...","description":"...","h1":"...","faq_schema":[]},'
    '"form_config_json":{"fields":["name","email","phone","travel_date","party_size","budget","message"],'
    '"submit_label":"...","whatsapp_link":"https://wa.me/...","whatsapp_label":"..."},"chunk_ids":[1]}'
)


def normalize_template_type(raw: str) -> str:
    key = (raw or "").strip().lower().replace(" ", "_").replace("-", "_")
    aliases = {
        "dest": "destination",
        "destination_page": "destination",
        "route_page": "route",
        "theme_tour": "theme",
        "theme_page": "theme",
        "visa_policy": "visa",
        "visa_page": "visa",
        "event_page": "event",
    }
    return aliases.get(key, key)


def _mock_modules(keyword: str, template_type: str, target_market: str) -> list[LandingModule]:
    headline = f"{keyword.title()} — Private China Tour for {target_market} Travelers"
    return [
        LandingModule(
            key="hero",
            content={
                "headline": headline,
                "subtitle": f"English-speaking guides, flexible dates, and curated highlights for {keyword}.",
                "cta_text": "Get a free itinerary draft",
                "image_hint": f"Aerial skyline and iconic landmarks — {keyword}",
            },
        ),
        LandingModule(
            key="why_this_trip",
            content={
                "headline": "Why travelers choose this trip",
                "bullets": [
                    f"Skip crowded group tours with a private {keyword} experience.",
                    "Licensed inbound operator with English support and 24/7 assistance.",
                    "Flexible pacing — adjust days, hotels, and activities to your style.",
                ],
            },
        ),
        LandingModule(
            key="itinerary",
            content={
                "headline": "Sample itinerary",
                "days": [
                    {
                        "day": 1,
                        "title": "Arrival & welcome",
                        "highlights": ["Private airport pickup", "Evening orientation walk"],
                    },
                    {
                        "day": 2,
                        "title": f"Highlights of {keyword}",
                        "highlights": ["Iconic sights with local guide", "Flexible photo stops"],
                    },
                    {
                        "day": 3,
                        "title": "Culture & departure",
                        "highlights": ["Local market or museum visit", "Transfer to airport"],
                    },
                ],
            },
        ),
        LandingModule(
            key="what_we_provide",
            content={
                "headline": "What we provide",
                "items": [
                    {"title": "Private transfers", "description": "Airport pickup and daily transport."},
                    {"title": "English guide", "description": "Licensed guide throughout your trip."},
                    {"title": "Hotel booking support", "description": "Curated 4-star options with breakfast."},
                    {"title": "Visa guidance", "description": "Checklist and document review — confirm with embassy."},
                ],
            },
        ),
        LandingModule(
            key="traveler_reviews",
            content={
                "headline": "Traveler reviews",
                "reviews": [
                    {
                        "name": "Sarah M.",
                        "country": "United States",
                        "rating": 5,
                        "quote": f"Our {keyword} trip was seamless — the guide made everything easy.",
                    },
                    {
                        "name": "James L.",
                        "country": "Canada",
                        "rating": 5,
                        "quote": "Flexible itinerary and great English support from start to finish.",
                    },
                ],
            },
        ),
        LandingModule(
            key="faq",
            content={
                "headline": "Frequently asked questions",
                "items": [
                    {
                        "question": "Do I need a visa for China?",
                        "answer": "Most visitors need a visa — requirements vary by nationality. We provide a checklist; confirm with your embassy.",
                    },
                    {
                        "question": "Is it safe to travel?",
                        "answer": "We work with licensed operators and provide 24/7 support. Follow local guidance and travel insurance is recommended.",
                    },
                    {
                        "question": "How do I pay?",
                        "answer": "Deposit by bank transfer or card; balance due before departure. All prices subject to human confirmation.",
                    },
                ],
            },
        ),
        LandingModule(
            key="lead_form",
            content={
                "headline": "Plan your trip",
                "subheadline": "Tell us your dates and group size — we reply within 24 hours.",
            },
        ),
        LandingModule(
            key="whatsapp_cta",
            content={
                "headline": "Prefer to chat now?",
                "button_label": "Message us on WhatsApp",
            },
        ),
    ]


def _mock_seo(keyword: str, target_market: str) -> SeoMetaJson:
    title = f"{keyword.title()} Private Tour | China Inbound Travel"
    description = (
        f"Book a private {keyword} tour for {target_market} travelers. "
        "English guides, flexible itineraries, and visa support — request a free quote."
    )
    return SeoMetaJson(
        title=title[:60],
        description=description[:160],
        h1=f"Private {keyword.title()} Tour",
        faq_schema=[
            {
                "@type": "Question",
                "name": "Do I need a visa for China?",
                "acceptedAnswer": {
                    "@type": "Answer",
                    "text": "Most visitors need a visa — confirm requirements with your embassy.",
                },
            }
        ],
    )


def _mock_form_config() -> FormConfigJson:
    return FormConfigJson(
        fields=["name", "email", "phone", "travel_date", "party_size", "budget", "message"],
        submit_label="Request itinerary",
        whatsapp_link="https://wa.me/8613800138000",
        whatsapp_label="Chat on WhatsApp",
    )


def _mock_result(
    request: LandingGenerateRequest,
    chunk_ids: list[int],
    model: str | None,
) -> LandingGenerateData:
    keyword = request.keyword_text.strip()
    template_type = normalize_template_type(request.template_type)
    title = f"{keyword.title()} | Private China {template_type.replace('_', ' ').title()} Page"
    modules = _mock_modules(keyword, template_type, request.target_market)
    return LandingGenerateData(
        title=title,
        content_json=LandingContentJson(modules=modules),
        seo_meta_json=_mock_seo(keyword, request.target_market),
        form_config_json=_mock_form_config(),
        needs_human_review=True,
        chunk_ids=chunk_ids[:3] if chunk_ids else None,
        model=model,
        capture_method="landing-mock",
    )


async def _fetch_rag_chunks(
    request: LandingGenerateRequest,
    settings: Settings,
) -> list[tuple[int, str]]:
    if not request.use_rag or not settings.database_url:
        return []
    query = (
        f"{request.keyword_text} China inbound private tour landing page "
        f"for {request.target_market} {request.language} travelers brand facts itinerary"
    )
    try:
        rag = await rag_service.search(
            RagSearchRequest(
                tenantId=request.tenant_id,
                projectId=request.project_id,
                query=query[:4000],
                topK=3,
            ),
            settings,
        )
        return [(hit.chunk_id, hit.chunk_text) for hit in rag.hits]
    except Exception as exc:
        logger.warning(
            "RAG context skipped tenant_id=%s project_id=%s: %s",
            request.tenant_id,
            request.project_id,
            exc,
        )
        return []


def _build_user_prompt(
    request: LandingGenerateRequest,
    rag_chunks: list[tuple[int, str]],
) -> str:
    template_type = normalize_template_type(request.template_type)
    module_keys = ", ".join(LANDING_MODULE_KEYS)
    lines = [
        f"keyword={request.keyword_text}",
        f"template_type={template_type}",
        f"language={request.language}",
        f"target_market={request.target_market}",
        f"project_id={request.project_id}",
        f"required_module_keys={module_keys}",
        f"json_schema={_JSON_SCHEMA_HINT}",
        "compliance: mark price/visa/policy claims for human review; do not invent guarantees.",
    ]
    if request.keyword_id:
        lines.append(f"keyword_id={request.keyword_id}")
    if rag_chunks:
        lines.append("knowledge_chunks:")
        for chunk_id, text in rag_chunks:
            snippet = text[:400].replace("\n", " ")
            lines.append(f"- chunk_id={chunk_id}: {snippet}")
    return "\n".join(lines)


def _extract_json(content: str) -> dict[str, Any]:
    text = (content or "").strip()
    fence = re.search(r"```(?:json)?\s*([\s\S]*?)```", text)
    if fence:
        text = fence.group(1).strip()
    return json.loads(text)


def _parse_modules(raw: Any) -> list[LandingModule]:
    content_json = raw
    if isinstance(content_json, dict):
        modules_raw = content_json.get("modules")
    elif isinstance(raw, list):
        modules_raw = raw
    else:
        raise ValueError("content_json.modules must be an array")

    if not isinstance(modules_raw, list) or not modules_raw:
        raise ValueError("content_json.modules must be a non-empty array")

    modules: list[LandingModule] = []
    seen_keys: set[str] = set()
    for item in modules_raw:
        if not isinstance(item, dict):
            continue
        key = str(item.get("key") or "").strip().lower()
        if not key:
            raise ValueError("module missing key")
        content = item.get("content")
        if not isinstance(content, dict):
            content = {k: v for k, v in item.items() if k != "key"}
        if not content:
            raise ValueError(f"module {key} missing content")
        modules.append(LandingModule(key=key, content=content))
        seen_keys.add(key)

    if not modules:
        raise ValueError("no valid modules after parse")

    missing = [k for k in LANDING_MODULE_KEYS if k not in seen_keys]
    if missing:
        raise ValueError(f"missing required module keys: {', '.join(missing)}")
    return modules


def _parse_seo_meta(raw: Any) -> SeoMetaJson:
    if not isinstance(raw, dict):
        raise ValueError("seo_meta_json must be an object")
    title = str(raw.get("title") or "").strip()
    description = str(raw.get("description") or "").strip()
    if not title or not description:
        raise ValueError("seo_meta_json missing title or description")
    faq_schema = raw.get("faq_schema") or raw.get("faqSchema")
    return SeoMetaJson(
        title=title,
        description=description,
        h1=raw.get("h1"),
        faq_schema=faq_schema if isinstance(faq_schema, list) else None,
    )


def _parse_form_config(raw: Any) -> FormConfigJson:
    if not isinstance(raw, dict):
        raise ValueError("form_config_json must be an object")
    fields = raw.get("fields")
    if not isinstance(fields, list) or not fields:
        raise ValueError("form_config_json.fields must be a non-empty array")
    return FormConfigJson(
        fields=[str(f) for f in fields],
        submit_label=raw.get("submit_label") or raw.get("submitLabel"),
        whatsapp_link=raw.get("whatsapp_link") or raw.get("whatsappLink"),
        whatsapp_label=raw.get("whatsapp_label") or raw.get("whatsappLabel"),
    )


def _parse_llm_content(
    payload: dict[str, Any],
    rag_chunk_ids: list[int],
) -> LandingGenerateData:
    title = str(payload.get("title") or "").strip()
    if not title:
        raise ValueError("LLM response missing title")

    content_raw = payload.get("content_json") or payload.get("contentJson")
    modules = _parse_modules(content_raw)
    seo_meta = _parse_seo_meta(payload.get("seo_meta_json") or payload.get("seoMetaJson"))
    form_config = _parse_form_config(payload.get("form_config_json") or payload.get("formConfigJson"))

    chunk_ids = payload.get("chunk_ids") or payload.get("chunkIds")
    if chunk_ids is None and rag_chunk_ids:
        chunk_ids = rag_chunk_ids[:3]

    return LandingGenerateData(
        title=title,
        content_json=LandingContentJson(modules=modules),
        seo_meta_json=seo_meta,
        form_config_json=form_config,
        needs_human_review=True,
        chunk_ids=[int(x) for x in chunk_ids] if chunk_ids else None,
        capture_method="llm",
    )


async def generate_landing(
    request: LandingGenerateRequest,
    settings: Settings | None = None,
) -> LandingGenerateData:
    cfg = settings or get_settings()
    if not request.keyword_text.strip():
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="keyword_text is required")

    template_type = normalize_template_type(request.template_type)
    if template_type not in SUPPORTED_TEMPLATE_TYPES:
        logger.info("template_type=%s not in MVP set; proceeding", template_type)

    rag_chunks = await _fetch_rag_chunks(request, cfg)
    rag_chunk_ids = [cid for cid, _ in rag_chunks]
    model = cfg.landing_model

    if cfg.landing_mock_llm:
        logger.warning("LANDING_MOCK_LLM enabled — returning canned landing result")
        return _mock_result(request, rag_chunk_ids, model)

    if not api_key_for_model(model, cfg):
        logger.warning("No LLM key — falling back to landing mock (local dev)")
        return _mock_result(request, rag_chunk_ids, model)

    prompt_template = await template_service.load_landing_generate_prompt(request.tenant_id, cfg)
    user_prompt = _build_user_prompt(request, rag_chunks)
    messages = [
        {"role": "system", "content": prompt_template},
        {"role": "user", "content": user_prompt},
    ]

    logger.info(
        "landing generate tenant_id=%s project_id=%s template_type=%s use_rag=%s",
        request.tenant_id,
        request.project_id,
        template_type,
        request.use_rag,
    )

    response = await llm_gateway.acompletion(
        model,
        messages,
        cfg,
        max_tokens=4096,
        temperature=0.5,
        extra={"response_format": {"type": "json_object"}} if model.startswith("openai/") else None,
    )
    content = response.choices[0].message.content or ""
    try:
        payload = _extract_json(content)
        result = _parse_llm_content(payload, rag_chunk_ids)
        result.model = response.model or model
        return result
    except (json.JSONDecodeError, ValueError, TypeError) as exc:
        logger.exception("landing LLM parse failed: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Failed to parse landing generation response: {exc}",
        ) from exc
