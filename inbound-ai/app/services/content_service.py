"""Short-form content script generation (FR-301/302 / EPIC-4 M1)."""

from __future__ import annotations

import json
import logging
import re
from typing import Any

from fastapi import HTTPException, status

from app.config import Settings, get_settings
from app.models.content import (
    SUPPORTED_DURATIONS,
    ContentGenerateData,
    ContentGenerateRequest,
    StoryboardScene,
)
from app.models.embed import RagSearchRequest
from app.services import llm_gateway, rag_service, template_service
from app.services.llm_provider import api_key_for_model

logger = logging.getLogger(__name__)

_JSON_SCHEMA_HINT = (
    '{"title":"...","hook":"...","target_audience":"...","script":"...",'
    '"voiceover":"...","on_screen_text":"...","cta":"...","hashtags":"...",'
    '"landing_page_suggestion":"...",'
    '"storyboard":[{"scene":1,"duration":3,"visual":"...","note":"..."}]}'
)


def normalize_platform(raw: str) -> str:
    key = (raw or "").strip().lower().replace(" ", "_").replace("-", "_")
    aliases = {
        "youtube_shorts": "youtube_shorts",
        "youtube_short": "youtube_shorts",
        "shorts": "youtube_shorts",
        "tiktok": "tiktok",
        "instagram": "instagram",
        "ig": "instagram",
        "youtube": "youtube",
        "facebook": "facebook",
        "fb": "facebook",
        "x": "x",
        "twitter": "x",
        "rednote": "rednote",
        "xiaohongshu": "rednote",
    }
    return aliases.get(key, key)


def _scene_count(duration_sec: int) -> int:
    if duration_sec <= 15:
        return 3
    if duration_sec <= 30:
        return 4
    return 6


def _mock_storyboard(keyword: str, duration_sec: int, platform: str) -> list[StoryboardScene]:
    total_scenes = _scene_count(duration_sec)
    per_scene = max(2, duration_sec // total_scenes)
    visuals = [
        f"Aerial skyline intro — {keyword}",
        "Traveler POV walking through iconic street market",
        "Close-up of private tour guide greeting guests",
        "Montage of highlights with on-screen captions",
        "Customer testimonial clip with warm lighting",
        "End card with brand logo and CTA button",
    ]
    scenes: list[StoryboardScene] = []
    for idx in range(total_scenes):
        scenes.append(
            StoryboardScene(
                scene=idx + 1,
                duration=per_scene if idx < total_scenes - 1 else max(2, duration_sec - per_scene * (total_scenes - 1)),
                visual=visuals[idx % len(visuals)],
                note=f"Mock scene for {platform} ({duration_sec}s).",
            )
        )
    return scenes


def _mock_result(
    request: ContentGenerateRequest,
    chunk_ids: list[int],
    model: str | None,
) -> ContentGenerateData:
    keyword = request.keyword_text.strip()
    platform = normalize_platform(request.platform)
    title = f"{keyword.title()} | Private China Tour for {request.target_market} Travelers"
    hook = f"Planning {keyword}? Here is what most first-time visitors miss."
    script = (
        f"Hook: {hook}\n\n"
        f"Introduce a trusted inbound tour operator specializing in {request.target_market} travelers. "
        f"Highlight private guides, flexible pacing, and English support. "
        f"Close with a soft CTA to request a custom itinerary."
    )
    voiceover = (
        f"If you are searching for {keyword}, skip the crowded group tours. "
        f"Our private China itineraries are built for English-speaking {request.target_market} travelers "
        f"who want comfort, culture, and zero guesswork."
    )
    on_screen = f"{keyword}\nPrivate tours · English guide · Flexible dates"
    cta = "Tap the link to get a free itinerary draft — spots fill fast this season."
    return ContentGenerateData(
        title=title,
        hook=hook,
        script=script,
        voiceover=voiceover,
        on_screen_text=on_screen,
        cta=cta,
        storyboard_json=_mock_storyboard(keyword, request.duration_sec, platform),
        needs_human_review=True,
        chunk_ids=chunk_ids[:3] if chunk_ids else None,
        target_audience=f"{request.target_market} English-speaking leisure travelers",
        hashtags=f"#ChinaTravel #{request.target_market}Travel #PrivateTour",
        landing_page_suggestion=f"Hero landing page for '{keyword}' with itinerary module and lead form.",
        model=model,
        capture_method="content-mock",
    )


async def _fetch_rag_chunks(
    request: ContentGenerateRequest,
    settings: Settings,
) -> list[tuple[int, str]]:
    if not request.use_rag or not settings.database_url:
        return []
    query = (
        f"{request.keyword_text} China inbound private tour for {request.target_market} "
        f"{request.language} travelers brand facts"
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
    request: ContentGenerateRequest,
    rag_chunks: list[tuple[int, str]],
) -> str:
    platform = normalize_platform(request.platform)
    lines = [
        f"keyword={request.keyword_text}",
        f"platform={platform}",
        f"duration_sec={request.duration_sec}",
        f"tone={request.tone}",
        f"language={request.language}",
        f"target_market={request.target_market}",
        f"project_id={request.project_id}",
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


def _parse_storyboard(raw: Any, duration_sec: int) -> list[StoryboardScene]:
    if not isinstance(raw, list) or not raw:
        raise ValueError("storyboard must be a non-empty array")
    scenes: list[StoryboardScene] = []
    for item in raw:
        if not isinstance(item, dict):
            continue
        scene_no = int(item.get("scene") or len(scenes) + 1)
        duration = int(item.get("duration") or max(2, duration_sec // max(len(raw), 1)))
        visual = str(item.get("visual") or "").strip()
        if not visual:
            raise ValueError(f"storyboard scene {scene_no} missing visual")
        scenes.append(
            StoryboardScene(
                scene=scene_no,
                duration=duration,
                visual=visual,
                note=item.get("note"),
            )
        )
    if not scenes:
        raise ValueError("storyboard array empty after parse")
    return scenes


def _parse_llm_content(
    payload: dict[str, Any],
    duration_sec: int,
    rag_chunk_ids: list[int],
) -> ContentGenerateData:
    hook = str(payload.get("hook") or "").strip()
    script = str(payload.get("script") or "").strip()
    voiceover = str(payload.get("voiceover") or script).strip()
    on_screen = str(payload.get("on_screen_text") or payload.get("onScreenText") or "").strip()
    cta = str(payload.get("cta") or "").strip()
    if not hook or not script or not cta:
        raise ValueError("LLM response missing hook, script, or cta")

    storyboard_raw = payload.get("storyboard") or payload.get("storyboard_json")
    storyboard = _parse_storyboard(storyboard_raw, duration_sec)

    chunk_ids = payload.get("chunk_ids") or payload.get("chunkIds")
    if chunk_ids is None and rag_chunk_ids:
        chunk_ids = rag_chunk_ids[:3]

    return ContentGenerateData(
        title=payload.get("title"),
        hook=hook,
        script=script,
        voiceover=voiceover,
        on_screen_text=on_screen or hook,
        cta=cta,
        storyboard_json=storyboard,
        needs_human_review=True,
        chunk_ids=[int(x) for x in chunk_ids] if chunk_ids else None,
        target_audience=payload.get("target_audience") or payload.get("targetAudience"),
        hashtags=payload.get("hashtags"),
        landing_page_suggestion=payload.get("landing_page_suggestion")
        or payload.get("landingPageSuggestion"),
        capture_method="llm",
    )


async def generate_content(
    request: ContentGenerateRequest,
    settings: Settings | None = None,
) -> ContentGenerateData:
    cfg = settings or get_settings()
    if not request.keyword_text.strip():
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="keyword_text is required")

    platform = normalize_platform(request.platform)
    if request.duration_sec not in SUPPORTED_DURATIONS:
        logger.info("duration_sec=%s not in MVP tiers; proceeding", request.duration_sec)

    rag_chunks = await _fetch_rag_chunks(request, cfg)
    rag_chunk_ids = [cid for cid, _ in rag_chunks]
    model = cfg.content_model

    if cfg.content_mock_llm:
        logger.warning("CONTENT_MOCK_LLM enabled — returning canned content result")
        return _mock_result(request, rag_chunk_ids, model)

    if not api_key_for_model(model, cfg):
        logger.warning("No LLM key — falling back to content mock (local dev)")
        return _mock_result(request, rag_chunk_ids, model)

    prompt_template = await template_service.load_content_script_prompt(request.tenant_id, cfg)
    user_prompt = _build_user_prompt(request, rag_chunks)
    messages = [
        {"role": "system", "content": prompt_template},
        {"role": "user", "content": user_prompt},
    ]

    logger.info(
        "content generate tenant_id=%s project_id=%s platform=%s duration=%s use_rag=%s",
        request.tenant_id,
        request.project_id,
        platform,
        request.duration_sec,
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
        result = _parse_llm_content(payload, request.duration_sec, rag_chunk_ids)
        result.model = response.model or model
        return result
    except (json.JSONDecodeError, ValueError, TypeError) as exc:
        logger.exception("content LLM parse failed: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Failed to parse content generation response: {exc}",
        ) from exc
