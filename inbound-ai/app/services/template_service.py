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
