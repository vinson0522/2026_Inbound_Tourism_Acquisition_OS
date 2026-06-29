"""Async PostgreSQL pool (asyncpg)."""

from __future__ import annotations

import asyncpg

from app.config import Settings, get_settings

_pool: asyncpg.Pool | None = None


def normalize_database_url(url: str) -> str:
    """Strip SQLAlchemy-style driver suffix for asyncpg."""
    return url.replace("postgresql+asyncpg://", "postgresql://", 1)


async def get_pool(settings: Settings | None = None) -> asyncpg.Pool:
    global _pool
    cfg = settings or get_settings()
    if not cfg.database_url:
        raise RuntimeError("DATABASE_URL not configured")
    if _pool is None:
        _pool = await asyncpg.create_pool(normalize_database_url(cfg.database_url), min_size=1, max_size=5)
    return _pool


async def close_pool() -> None:
    global _pool
    if _pool is not None:
        await _pool.close()
        _pool = None
