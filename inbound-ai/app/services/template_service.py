"""Load prompt templates from DB (template table) with short config fallback."""

from __future__ import annotations

import json
import logging

from app.config import Settings, get_settings
from app.db import get_pool

logger = logging.getLogger(__name__)


async def load_keyword_generate_prompt(
    tenant_id: int,
    settings: Settings | None = None,
) -> str:
    """Return prompt body for keyword generation (FR-201)."""
    cfg = settings or get_settings()
    if cfg.database_url:
        try:
            pool = await get_pool()
            row = await pool.fetchrow(
                """
                SELECT config_json
                FROM template
                WHERE tenant_id = $1
                  AND name = $2
                  AND type = 'CONTENT'::template_type
                  AND deleted_at IS NULL
                ORDER BY is_default DESC, id ASC
                LIMIT 1
                """,
                tenant_id,
                cfg.keywords_template_name,
            )
            if row:
                payload = row["config_json"]
                if isinstance(payload, str):
                    payload = json.loads(payload)
                prompt = payload.get("prompt") or payload.get("body")
                if prompt and str(prompt).strip():
                    return str(prompt).strip()
        except Exception as exc:
            logger.warning("template load failed tenant_id=%s name=%s: %s", tenant_id, cfg.keywords_template_name, exc)
    return cfg.keywords_prompt_fallback.strip()


async def load_content_script_prompt(
    tenant_id: int,
    settings: Settings | None = None,
) -> str:
    """Return prompt body for content script generation (FR-301/302)."""
    cfg = settings or get_settings()
    if cfg.database_url:
        try:
            pool = await get_pool()
            row = await pool.fetchrow(
                """
                SELECT config_json
                FROM template
                WHERE tenant_id = $1
                  AND name = $2
                  AND type = 'CONTENT'::template_type
                  AND deleted_at IS NULL
                ORDER BY is_default DESC, id ASC
                LIMIT 1
                """,
                tenant_id,
                cfg.content_template_name,
            )
            if row:
                payload = row["config_json"]
                if isinstance(payload, str):
                    payload = json.loads(payload)
                prompt = payload.get("prompt") or payload.get("body")
                if prompt and str(prompt).strip():
                    return str(prompt).strip()
        except Exception as exc:
            logger.warning(
                "template load failed tenant_id=%s name=%s: %s",
                tenant_id,
                cfg.content_template_name,
                exc,
            )
    return cfg.content_prompt_fallback.strip()


async def load_landing_generate_prompt(
    tenant_id: int,
    settings: Settings | None = None,
) -> str:
    """Return prompt body for landing page generation (FR-502~505)."""
    cfg = settings or get_settings()
    if cfg.database_url:
        try:
            pool = await get_pool()
            row = await pool.fetchrow(
                """
                SELECT config_json
                FROM template
                WHERE tenant_id = $1
                  AND name = $2
                  AND type = 'CONTENT'::template_type
                  AND deleted_at IS NULL
                ORDER BY is_default DESC, id ASC
                LIMIT 1
                """,
                tenant_id,
                cfg.landing_template_name,
            )
            if row:
                payload = row["config_json"]
                if isinstance(payload, str):
                    payload = json.loads(payload)
                prompt = payload.get("prompt") or payload.get("body")
                if prompt and str(prompt).strip():
                    return str(prompt).strip()
        except Exception as exc:
            logger.warning(
                "template load failed tenant_id=%s name=%s: %s",
                tenant_id,
                cfg.landing_template_name,
                exc,
            )
    return cfg.landing_prompt_fallback.strip()


async def load_breakdown_analyze_prompt(
    tenant_id: int,
    settings: Settings | None = None,
) -> str:
    """Return prompt body for viral video seven-dimension analysis (FR-403)."""
    cfg = settings or get_settings()
    if cfg.database_url:
        try:
            pool = await get_pool()
            row = await pool.fetchrow(
                """
                SELECT config_json
                FROM template
                WHERE tenant_id = $1
                  AND name = $2
                  AND type = 'CONTENT'::template_type
                  AND deleted_at IS NULL
                ORDER BY is_default DESC, id ASC
                LIMIT 1
                """,
                tenant_id,
                cfg.breakdown_template_name,
            )
            if row:
                payload = row["config_json"]
                if isinstance(payload, str):
                    payload = json.loads(payload)
                prompt = payload.get("prompt") or payload.get("body")
                if prompt and str(prompt).strip():
                    return str(prompt).strip()
        except Exception as exc:
            logger.warning(
                "template load failed tenant_id=%s name=%s: %s",
                tenant_id,
                cfg.breakdown_template_name,
                exc,
            )
    return cfg.breakdown_prompt_fallback.strip()


async def load_followup_generate_prompt(
    tenant_id: int,
    settings: Settings | None = None,
) -> str:
    """Return prompt body for lead follow-up suggestion (FR-603)."""
    cfg = settings or get_settings()
    if cfg.database_url:
        try:
            pool = await get_pool()
            row = await pool.fetchrow(
                """
                SELECT config_json
                FROM template
                WHERE tenant_id = $1
                  AND name = $2
                  AND type = 'CONTENT'::template_type
                  AND deleted_at IS NULL
                ORDER BY is_default DESC, id ASC
                LIMIT 1
                """,
                tenant_id,
                cfg.followup_template_name,
            )
            if row:
                payload = row["config_json"]
                if isinstance(payload, str):
                    payload = json.loads(payload)
                prompt = payload.get("prompt") or payload.get("body")
                if prompt and str(prompt).strip():
                    return str(prompt).strip()
        except Exception as exc:
            logger.warning(
                "template load failed tenant_id=%s name=%s: %s",
                tenant_id,
                cfg.followup_template_name,
                exc,
            )
    return cfg.followup_prompt_fallback.strip()


class KeywordScoreTemplateConfig:
    __slots__ = ("prompt", "weights", "weights_version")

    def __init__(self, prompt: str, weights: dict[str, float], weights_version: str):
        self.prompt = prompt
        self.weights = weights
        self.weights_version = weights_version


async def load_keyword_score_template(
    tenant_id: int,
    settings: Settings | None = None,
) -> KeywordScoreTemplateConfig:
    """Return prompt + dimension weights for keyword scoring (FR-203 / ADR-19)."""
    cfg = settings or get_settings()
    default_weights = dict(cfg.keyword_score_default_weights)
    if cfg.database_url:
        try:
            pool = await get_pool()
            row = await pool.fetchrow(
                """
                SELECT config_json
                FROM template
                WHERE tenant_id = $1
                  AND name = $2
                  AND type = 'CONTENT'::template_type
                  AND deleted_at IS NULL
                ORDER BY is_default DESC, id ASC
                LIMIT 1
                """,
                tenant_id,
                cfg.keyword_score_template_name,
            )
            if row:
                payload = row["config_json"]
                if isinstance(payload, str):
                    payload = json.loads(payload)
                prompt = payload.get("prompt") or payload.get("body")
                weights_raw = payload.get("weights") or payload.get("metric_weights")
                weights = _normalize_score_weights(weights_raw, default_weights)
                version = str(payload.get("version") or cfg.keyword_score_weights_version)
                prompt_text = (
                    str(prompt).strip() if prompt else cfg.keyword_score_prompt_fallback.strip()
                )
                return KeywordScoreTemplateConfig(prompt_text, weights, version)
        except Exception as exc:
            logger.warning(
                "keyword score template load failed tenant_id=%s name=%s: %s",
                tenant_id,
                cfg.keyword_score_template_name,
                exc,
            )
    return KeywordScoreTemplateConfig(
        cfg.keyword_score_prompt_fallback.strip(),
        default_weights,
        cfg.keyword_score_weights_version,
    )


def _normalize_score_weights(
    raw: object,
    defaults: dict[str, float],
) -> dict[str, float]:
    if not isinstance(raw, dict):
        return dict(defaults)
    merged = dict(defaults)
    for key in merged:
        if key in raw and raw[key] is not None:
            merged[key] = float(raw[key])
    total = sum(merged.values())
    if total <= 0:
        return dict(defaults)
    if abs(total - 1.0) > 0.01:
        merged = {k: v / total for k, v in merged.items()}
    return merged
