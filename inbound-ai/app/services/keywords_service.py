"""Keyword opportunity generation (FR-201 / EPIC-3 M1)."""

from __future__ import annotations

import json
import logging
import re
from datetime import UTC, datetime
from typing import Any

from fastapi import HTTPException, status

from app.config import Settings, get_settings
from app.models.embed import RagSearchRequest
from app.models.keywords import (
    LIFECYCLE_STAGES,
    GeneratedKeyword,
    KeywordGenerateData,
    KeywordGenerateRequest,
    KeywordScoreData,
    KeywordScoreDetail,
    KeywordScoreRequest,
    SCORE_DIMENSIONS,
    StageKeywords,
)
from app.services import llm_gateway, rag_service, template_service
from app.services.llm_provider import api_key_for_model

logger = logging.getLogger(__name__)

_STAGE_ALIASES: dict[str, str] = {
    "INSPIRATION": "inspiration",
    "PLANTING": "planting",
    "COMPARISON": "comparison",
    "VISA": "visa",
    "PLANNING": "planning",
    "TRUST": "trust",
    "DECISION": "decision",
    "REPURCHASE": "repurchase",
    "灵感": "inspiration",
    "种草": "planting",
    "比较": "comparison",
    "签证": "visa",
    "规划": "planning",
    "信任": "trust",
    "决策": "decision",
    "复购": "repurchase",
}

_JSON_SCHEMA_HINT = (
    '{"stages":[{"stage":"inspiration","keywords":[{"text":"...","rationale":"..."}]}]}'
)


def normalize_stage(raw: str) -> str | None:
    key = (raw or "").strip()
    if not key:
        return None
    lower = key.lower()
    if lower in LIFECYCLE_STAGES:
        return lower
    upper = key.upper()
    if upper in _STAGE_ALIASES:
        return _STAGE_ALIASES[upper]
    if key in _STAGE_ALIASES:
        return _STAGE_ALIASES[key]
    return None


def resolve_stages(request: KeywordGenerateRequest) -> list[str]:
    if not request.stages:
        return list(LIFECYCLE_STAGES)
    normalized: list[str] = []
    for raw in request.stages:
        stage = normalize_stage(raw)
        if stage is None:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Unknown lifecycle stage: {raw}",
            )
        if stage not in normalized:
            normalized.append(stage)
    return normalized


async def _fetch_rag_chunks(
    request: KeywordGenerateRequest,
    settings: Settings,
) -> list[tuple[int, str]]:
    if not request.use_rag or not settings.database_url:
        return []
    query = (
        f"China inbound tourism private tours for {request.market} market "
        f"English-speaking travelers"
    )
    try:
        rag = await rag_service.search(
            RagSearchRequest(
                tenantId=request.tenant_id,
                projectId=request.project_id,
                query=query,
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


def _mock_keywords_for_stage(
    stage: str,
    market: str,
    count: int,
    chunk_ids: list[int],
) -> list[GeneratedKeyword]:
    samples = {
        "inspiration": "unique places to visit in China first time",
        "planting": "Chongqing cyberpunk city tour TikTok",
        "comparison": "private tour vs group tour China",
        "visa": "China visa free transit for US citizens",
        "planning": "10 day China itinerary first timers",
        "trust": "best China travel agency for foreigners",
        "decision": "private China tour cost USD",
        "repurchase": "where to go after Beijing Shanghai",
    }
    base = samples.get(stage, f"{stage} China travel keyword")
    keywords: list[GeneratedKeyword] = []
    for idx in range(count):
        text = f"{base} {market} #{idx + 1}".strip()
        keywords.append(
            GeneratedKeyword(
                text=text,
                rationale=f"Mock FR-201 keyword for {stage} stage ({market}).",
                chunk_ids=chunk_ids[:1] if chunk_ids else None,
                suggested_score=None,
                needs_human_review=True,
            )
        )
    return keywords


def _mock_result(
    request: KeywordGenerateRequest,
    stages: list[str],
    chunk_ids: list[int],
    model: str | None,
) -> KeywordGenerateData:
    return KeywordGenerateData(
        needs_human_review=True,
        model=model,
        capture_method="keywords-mock",
        stages=[
            StageKeywords(
                stage=stage,
                keywords=_mock_keywords_for_stage(
                    stage,
                    request.market,
                    request.words_per_stage,
                    chunk_ids,
                ),
            )
            for stage in stages
        ],
    )


def _build_user_prompt(
    request: KeywordGenerateRequest,
    stages: list[str],
    rag_chunks: list[tuple[int, str]],
) -> str:
    lines = [
        f"market={request.market}",
        f"locale={request.locale}",
        f"project_id={request.project_id}",
        f"stages={','.join(stages)}",
        f"words_per_stage={request.words_per_stage}",
        f"json_schema={_JSON_SCHEMA_HINT}",
    ]
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


def _parse_llm_keywords(
    payload: dict[str, Any],
    stages: list[str],
    words_per_stage: int,
    rag_chunk_ids: list[int],
) -> list[StageKeywords]:
    raw_stages = payload.get("stages")
    if not isinstance(raw_stages, list):
        raise ValueError("LLM response missing stages array")

    by_stage: dict[str, list[GeneratedKeyword]] = {}
    for item in raw_stages:
        if not isinstance(item, dict):
            continue
        stage = normalize_stage(str(item.get("stage", "")))
        if stage is None:
            continue
        keywords_raw = item.get("keywords") or []
        parsed: list[GeneratedKeyword] = []
        if isinstance(keywords_raw, list):
            for kw in keywords_raw[:words_per_stage]:
                if not isinstance(kw, dict):
                    continue
                text = str(kw.get("text") or "").strip()
                if not text:
                    continue
                chunk_ids = kw.get("chunk_ids") or kw.get("chunkIds")
                if chunk_ids is None and rag_chunk_ids:
                    chunk_ids = rag_chunk_ids[:1]
                parsed.append(
                    GeneratedKeyword(
                        text=text,
                        rationale=kw.get("rationale"),
                        chunk_ids=[int(x) for x in chunk_ids] if chunk_ids else None,
                        suggested_score=None,
                        needs_human_review=True,
                    )
                )
        if parsed:
            by_stage[stage] = parsed

    result: list[StageKeywords] = []
    for stage in stages:
        keywords = by_stage.get(stage)
        if not keywords:
            raise ValueError(f"LLM response missing keywords for stage={stage}")
        result.append(StageKeywords(stage=stage, keywords=keywords))
    return result


async def generate_keywords(
    request: KeywordGenerateRequest,
    settings: Settings | None = None,
) -> KeywordGenerateData:
    cfg = settings or get_settings()
    stages = resolve_stages(request)
    rag_chunks = await _fetch_rag_chunks(request, cfg)
    rag_chunk_ids = [cid for cid, _ in rag_chunks]
    model = cfg.keywords_model

    if cfg.keywords_mock_llm:
        logger.warning("KEYWORDS_MOCK_LLM enabled — returning canned keyword result")
        return _mock_result(request, stages, rag_chunk_ids, model)

    if not api_key_for_model(model, cfg):
        logger.warning("No LLM key — falling back to keywords mock (local dev)")
        return _mock_result(request, stages, rag_chunk_ids, model)

    prompt_template = await template_service.load_keyword_generate_prompt(request.tenant_id, cfg)
    user_prompt = _build_user_prompt(request, stages, rag_chunks)
    messages = [
        {"role": "system", "content": prompt_template},
        {"role": "user", "content": user_prompt},
    ]

    logger.info(
        "keywords generate tenant_id=%s project_id=%s market=%s stages=%s use_rag=%s",
        request.tenant_id,
        request.project_id,
        request.market,
        len(stages),
        request.use_rag,
    )

    response = await llm_gateway.acompletion(
        model,
        messages,
        cfg,
        max_tokens=4096,
        temperature=0.4,
        extra={"response_format": {"type": "json_object"}} if model.startswith("openai/") else None,
    )
    content = response.choices[0].message.content or ""
    try:
        payload = _extract_json(content)
        parsed_stages = _parse_llm_keywords(
            payload,
            stages,
            request.words_per_stage,
            rag_chunk_ids,
        )
    except (json.JSONDecodeError, ValueError, TypeError) as exc:
        logger.exception("keywords LLM parse failed: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Failed to parse keyword generation response: {exc}",
        ) from exc

    return KeywordGenerateData(
        needs_human_review=True,
        stages=parsed_stages,
        model=response.model or model,
        capture_method="llm",
    )


_SCORE_JSON_HINT = (
    '{"relevance":0-100,"long_tail_value":0-100,"producibility":0-100,'
    '"landing_value":0-100,"competitive_pressure":0-100}'
)


def resolve_score_stage(raw: str) -> str:
    stage = normalize_stage(raw)
    if stage is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unknown lifecycle stage: {raw}",
        )
    return stage


def compute_weighted_score(dimensions: dict[str, float], weights: dict[str, float]) -> float:
    total = 0.0
    for key in SCORE_DIMENSIONS:
        weight = weights.get(key, 0.0)
        total += float(dimensions[key]) * weight
    return round(total, 1)


def _clamp_score(value: float) -> float:
    return round(max(0.0, min(100.0, float(value))), 1)


def _mock_dimension_scores(
    request: KeywordScoreRequest,
    stage: str,
) -> dict[str, float]:
    seed = hash((request.keyword_id, request.keyword.strip().lower(), stage)) & 0xFFFFFFFF

    def dim(offset: int) -> float:
        return float(50 + (seed + offset * 17) % 46)

    competitive = dim(4)
    if request.geo_score is not None:
        competitive = _clamp_score(float(request.geo_score) * 0.92 + 8)

    return {
        "relevance": dim(0),
        "long_tail_value": dim(1),
        "producibility": dim(2),
        "landing_value": dim(3),
        "competitive_pressure": competitive,
    }


async def _fetch_score_rag_chunks(
    request: KeywordScoreRequest,
    settings: Settings,
) -> list[tuple[int, str]]:
    if not request.use_rag or not settings.database_url:
        return []
    query = request.keyword_en or request.keyword
    try:
        rag = await rag_service.search(
            RagSearchRequest(
                tenantId=request.tenant_id,
                projectId=request.project_id,
                query=query,
                topK=3,
            ),
            settings,
        )
        return [(hit.chunk_id, hit.chunk_text) for hit in rag.hits]
    except Exception as exc:
        logger.warning(
            "score RAG skipped tenant_id=%s project_id=%s: %s",
            request.tenant_id,
            request.project_id,
            exc,
        )
        return []


def _build_score_user_prompt(
    request: KeywordScoreRequest,
    stage: str,
    rag_chunks: list[tuple[int, str]],
) -> str:
    lines = [
        f"keyword_id={request.keyword_id}",
        f"keyword={request.keyword}",
        f"keyword_en={request.keyword_en or request.keyword}",
        f"stage={stage}",
        f"market={request.market}",
        f"json_schema={_SCORE_JSON_HINT}",
    ]
    if request.brand_name:
        lines.append(f"brand_name={request.brand_name}")
    if request.competitors:
        lines.append(f"competitors={','.join(request.competitors)}")
    if request.geo_score is not None:
        lines.append(f"geo_score={request.geo_score}")
    if rag_chunks:
        lines.append("knowledge_chunks:")
        for chunk_id, text in rag_chunks:
            snippet = text[:400].replace("\n", " ")
            lines.append(f"- chunk_id={chunk_id}: {snippet}")
    return "\n".join(lines)


def _parse_llm_dimensions(payload: dict[str, Any]) -> dict[str, float]:
    dimensions: dict[str, float] = {}
    for key in SCORE_DIMENSIONS:
        raw = payload.get(key)
        if raw is None:
            raise ValueError(f"LLM response missing dimension: {key}")
        dimensions[key] = _clamp_score(float(raw))
    return dimensions


def _build_score_detail(
    dimensions: dict[str, float],
    weights_version: str,
    geo_score_input: float | None,
) -> KeywordScoreDetail:
    return KeywordScoreDetail(
        relevance=dimensions["relevance"],
        long_tail_value=dimensions["long_tail_value"],
        producibility=dimensions["producibility"],
        landing_value=dimensions["landing_value"],
        competitive_pressure=dimensions["competitive_pressure"],
        geo_score_input=geo_score_input,
        weights_version=weights_version,
        computed_at=datetime.now(UTC).replace(microsecond=0).isoformat(),
    )


def _mock_score_result(
    request: KeywordScoreRequest,
    stage: str,
    template_cfg: template_service.KeywordScoreTemplateConfig,
    model: str | None,
) -> KeywordScoreData:
    dimensions = _mock_dimension_scores(request, stage)
    score = compute_weighted_score(dimensions, template_cfg.weights)
    return KeywordScoreData(
        score=score,
        score_detail=_build_score_detail(
            dimensions,
            template_cfg.weights_version,
            request.geo_score,
        ),
        needs_human_review=False,
        model=model,
        capture_method="keyword-score-mock",
    )


async def score_keyword(
    request: KeywordScoreRequest,
    settings: Settings | None = None,
) -> KeywordScoreData:
    cfg = settings or get_settings()
    stage = resolve_score_stage(request.stage)
    template_cfg = await template_service.load_keyword_score_template(request.tenant_id, cfg)
    rag_chunks = await _fetch_score_rag_chunks(request, cfg)
    model = cfg.keyword_score_model

    if cfg.keyword_score_mock_llm:
        logger.warning("KEYWORD_SCORE_MOCK_LLM enabled — returning deterministic score")
        return _mock_score_result(request, stage, template_cfg, model)

    if not api_key_for_model(model, cfg):
        logger.warning("No LLM key — falling back to keyword score mock (local dev)")
        return _mock_score_result(request, stage, template_cfg, model)

    user_prompt = _build_score_user_prompt(request, stage, rag_chunks)
    messages = [
        {"role": "system", "content": template_cfg.prompt},
        {"role": "user", "content": user_prompt},
    ]

    logger.info(
        "keyword score tenant_id=%s project_id=%s keyword_id=%s stage=%s use_rag=%s",
        request.tenant_id,
        request.project_id,
        request.keyword_id,
        stage,
        request.use_rag,
    )

    response = await llm_gateway.acompletion(
        model,
        messages,
        cfg,
        max_tokens=1024,
        temperature=0.2,
        extra={"response_format": {"type": "json_object"}} if model.startswith("openai/") else None,
    )
    content = response.choices[0].message.content or ""
    try:
        payload = _extract_json(content)
        dimensions = _parse_llm_dimensions(payload)
    except (json.JSONDecodeError, ValueError, TypeError) as exc:
        logger.exception("keyword score LLM parse failed: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Failed to parse keyword score response: {exc}",
        ) from exc

    score = compute_weighted_score(dimensions, template_cfg.weights)
    return KeywordScoreData(
        score=score,
        score_detail=_build_score_detail(
            dimensions,
            template_cfg.weights_version,
            request.geo_score,
        ),
        needs_human_review=False,
        model=response.model or model,
        capture_method="llm",
    )
