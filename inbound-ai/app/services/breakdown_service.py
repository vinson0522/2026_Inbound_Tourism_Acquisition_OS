"""Viral video breakdown — frame extraction + seven-dimension analysis (FR-402/403)."""

from __future__ import annotations

import json
import logging
import re
import shutil
import subprocess
import tempfile
from pathlib import Path
from typing import Any

import httpx
from fastapi import HTTPException, status

from app.config import Settings, get_settings
from app.models.breakdown import (
    DIMENSION_KEYS,
    MOCK_FRAME_COUNT,
    BreakdownAnalyzeData,
    BreakdownAnalyzeRequest,
    BreakdownExtractData,
    BreakdownExtractRequest,
    BreakdownFrame,
)
from app.services import llm_gateway, template_service
from app.services.llm_provider import api_key_for_model

logger = logging.getLogger(__name__)

_JSON_SCHEMA_HINT = (
    '{"theme":"...","hook":"...","shot":"...","subtitle":"...","emotion":"...",'
    '"psychology":"...","reusable":"...","reusable_structure":"..."}'
)

_MOCK_FRAME_CAPTIONS = [
    "Opening skyline hook — contrast day vs night",
    "Traveler POV entering iconic street market",
    "Guide greeting guests with name card close-up",
    "Fast B-roll montage of highlights with captions",
    "Social proof clip — review stars and smiling guests",
    "Soft CTA end card with brand logo and link sticker",
]

_MOCK_DIMENSIONS = {
    "theme": "First-time China inbound trust building",
    "hook": "Skyline contrast in first 3 seconds",
    "shot": "Fast B-roll cuts with handheld POV",
    "subtitle": "Bold English keywords on screen",
    "emotion": "Curiosity shifting to aspiration",
    "psychology": "Social proof and risk reduction cues",
    "reusable": "Problem-evidence-CTA three-act structure",
}

_MOCK_REUSABLE_STRUCTURE = (
    "Open with a visual hook, establish credibility with proof points, "
    "close with a soft CTA that lowers booking friction."
)


def _format_timestamp(seconds: int) -> str:
    minutes, sec = divmod(max(seconds, 0), 60)
    return f"{minutes}:{sec:02d}"


def _mock_frames(source_url: str) -> list[BreakdownFrame]:
    frames: list[BreakdownFrame] = []
    for idx in range(MOCK_FRAME_COUNT):
        timestamp = idx * 5
        frames.append(
            BreakdownFrame(
                timestamp=timestamp,
                timestampLabel=_format_timestamp(timestamp),
                thumbnailUrl=f"https://placehold.co/160x90/png?text={idx + 1}",
                caption=_MOCK_FRAME_CAPTIONS[idx],
            )
        )
    return frames


def _mock_analyze(title: str | None, model: str | None) -> BreakdownAnalyzeData:
    theme = _MOCK_DIMENSIONS["theme"]
    if title and title.strip():
        theme = f"{title.strip()} — {_MOCK_DIMENSIONS['theme']}"
    dimensions = dict(_MOCK_DIMENSIONS)
    dimensions["theme"] = theme
    return BreakdownAnalyzeData(
        dimensions=dimensions,
        reusable_structure=_MOCK_REUSABLE_STRUCTURE,
        needs_human_review=True,
        model=model,
        capture_method="breakdown-mock",
    )


def _extract_json(content: str) -> dict[str, Any]:
    text = (content or "").strip()
    fence = re.search(r"```(?:json)?\s*([\s\S]*?)```", text)
    if fence:
        text = fence.group(1).strip()
    return json.loads(text)


def _parse_analyze_payload(payload: dict[str, Any]) -> BreakdownAnalyzeData:
    dimensions: dict[str, str] = {}
    for key in DIMENSION_KEYS:
        value = payload.get(key)
        if value is not None and str(value).strip():
            dimensions[key] = str(value).strip()
    if len(dimensions) < 2:
        raise ValueError("LLM response missing required dimension fields")

    reusable_structure = str(
        payload.get("reusable_structure")
        or payload.get("reusableStructure")
        or dimensions.get("reusable")
        or ""
    ).strip()
    if not reusable_structure:
        raise ValueError("LLM response missing reusable_structure")

    return BreakdownAnalyzeData(
        dimensions=dimensions,
        reusable_structure=reusable_structure,
        needs_human_review=True,
        capture_method="llm",
    )


def _build_analyze_prompt(request: BreakdownAnalyzeRequest) -> str:
    lines = [
        f"title={request.title or ''}",
        f"source_url={request.source_url or ''}",
        f"frame_count={len(request.frames)}",
        f"json_schema={_JSON_SCHEMA_HINT}",
        "frames:",
    ]
    for frame in request.frames[:12]:
        lines.append(
            f"- t={frame.timestamp_label} caption={frame.caption[:200]}"
        )
    lines.append(
        "Return actionable insights for inbound tourism marketers — inspiration, not copying."
    )
    return "\n".join(lines)


async def _try_ffmpeg_frames(source_url: str, interval_sec: int = 5, max_frames: int = 6) -> list[BreakdownFrame]:
    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg:
        return []

    async with httpx.AsyncClient(timeout=60.0, follow_redirects=True) as client:
        resp = await client.get(source_url)
        resp.raise_for_status()
        video_bytes = resp.content

    with tempfile.TemporaryDirectory(prefix="inbound-breakdown-") as tmp:
        video_path = Path(tmp) / "source.bin"
        video_path.write_bytes(video_bytes)
        out_pattern = str(Path(tmp) / "frame_%02d.jpg")
        cmd = [
            ffmpeg,
            "-hide_banner",
            "-loglevel",
            "error",
            "-i",
            str(video_path),
            "-vf",
            f"fps=1/{interval_sec}",
            "-frames:v",
            str(max_frames),
            out_pattern,
        ]
        proc = subprocess.run(cmd, capture_output=True, text=True, check=False)
        if proc.returncode != 0:
            logger.warning("ffmpeg extract failed: %s", proc.stderr[:500])
            return []

        frames: list[BreakdownFrame] = []
        for idx, image_path in enumerate(sorted(Path(tmp).glob("frame_*.jpg"))):
            timestamp = idx * interval_sec
            frames.append(
                BreakdownFrame(
                    timestamp=timestamp,
                    timestampLabel=_format_timestamp(timestamp),
                    thumbnailUrl=f"file://{image_path.as_posix()}",
                    caption=f"Frame at {_format_timestamp(timestamp)}",
                )
            )
        return frames


async def extract_frames(
    request: BreakdownExtractRequest,
    settings: Settings | None = None,
) -> BreakdownExtractData:
    cfg = settings or get_settings()
    source_url = request.source_url.strip()
    if not source_url:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="sourceUrl is required")

    if cfg.breakdown_mock_llm:
        logger.warning("BREAKDOWN_MOCK_LLM enabled — returning %s mock frames", MOCK_FRAME_COUNT)
        return BreakdownExtractData(frames=_mock_frames(source_url), capture_method="breakdown-mock")

    try:
        ffmpeg_frames = await _try_ffmpeg_frames(source_url)
        if ffmpeg_frames:
            return BreakdownExtractData(frames=ffmpeg_frames, capture_method="ffmpeg-interval")
    except Exception as exc:
        logger.warning("ffmpeg frame extraction skipped for %s: %s", source_url, exc)

    logger.warning("No ffmpeg frames — falling back to breakdown mock (local dev)")
    return BreakdownExtractData(frames=_mock_frames(source_url), capture_method="breakdown-mock")


async def analyze(
    request: BreakdownAnalyzeRequest,
    settings: Settings | None = None,
) -> BreakdownAnalyzeData:
    cfg = settings or get_settings()
    if not request.frames:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="frames is required")

    model = cfg.breakdown_model

    if cfg.breakdown_mock_llm:
        logger.warning("BREAKDOWN_MOCK_LLM enabled — returning canned seven-dimension analysis")
        return _mock_analyze(request.title, model)

    if not api_key_for_model(model, cfg):
        logger.warning("No LLM key — falling back to breakdown mock (local dev)")
        return _mock_analyze(request.title, model)

    prompt_template = await template_service.load_breakdown_analyze_prompt(
        request.tenant_id or 1,
        cfg,
    )
    user_prompt = _build_analyze_prompt(request)
    messages = [
        {"role": "system", "content": prompt_template},
        {"role": "user", "content": user_prompt},
    ]

    logger.info(
        "breakdown analyze frame_count=%s title=%r",
        len(request.frames),
        request.title,
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
        payload = _extract_json(content)
        result = _parse_analyze_payload(payload)
        result.model = response.model or model
        return result
    except (json.JSONDecodeError, ValueError, TypeError) as exc:
        logger.exception("breakdown analyze parse failed: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Failed to parse breakdown analysis response: {exc}",
        ) from exc


async def run_breakdown_job(
    *,
    source_url: str,
    title: str | None,
    tenant_id: int | None,
    project_id: int | None,
    material_id: int | None,
    settings: Settings | None = None,
) -> tuple[list[BreakdownFrame], BreakdownAnalyzeData]:
    """Full pipeline used by MQ worker: extract frames then analyze."""
    cfg = settings or get_settings()
    extract = await extract_frames(
        BreakdownExtractRequest(
            tenantId=tenant_id,
            projectId=project_id,
            materialId=material_id,
            sourceUrl=source_url,
            title=title,
        ),
        cfg,
    )
    analyze_data = await analyze(
        BreakdownAnalyzeRequest(
            tenantId=tenant_id,
            projectId=project_id,
            sourceUrl=source_url,
            title=title,
            frames=extract.frames,
        ),
        cfg,
    )
    return extract.frames, analyze_data
