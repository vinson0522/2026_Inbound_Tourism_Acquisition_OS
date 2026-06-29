"""knowledge_asset / knowledge_chunk persistence (tenant-scoped)."""

from __future__ import annotations

from dataclasses import dataclass

import asyncpg

from app.db import get_pool
from app.services.embedding_service import vector_to_pg


@dataclass
class KnowledgeAssetRow:
    id: int
    tenant_id: int
    project_id: int
    title: str
    content: str | None
    file_url: str | None
    vector_status: str


async def fetch_asset(asset_id: int, tenant_id: int, project_id: int) -> KnowledgeAssetRow | None:
    pool = await get_pool()
    row = await pool.fetchrow(
        """
        SELECT id, tenant_id, project_id, title, content, file_url, vector_status::text
        FROM knowledge_asset
        WHERE id = $1 AND tenant_id = $2 AND project_id = $3 AND deleted_at IS NULL
        """,
        asset_id,
        tenant_id,
        project_id,
    )
    if not row:
        return None
    return KnowledgeAssetRow(
        id=row["id"],
        tenant_id=row["tenant_id"],
        project_id=row["project_id"],
        title=row["title"],
        content=row["content"],
        file_url=row["file_url"],
        vector_status=row["vector_status"],
    )


async def set_asset_status(asset_id: int, status: str) -> None:
    pool = await get_pool()
    await pool.execute(
        """
        UPDATE knowledge_asset
        SET vector_status = $2::vector_index_status, updated_at = NOW()
        WHERE id = $1
        """,
        asset_id,
        status,
    )


async def replace_chunks(
    *,
    tenant_id: int,
    project_id: int,
    asset_id: int,
    chunks: list[tuple[int, str, int, list[float]]],
) -> int:
    """Soft-delete old chunks and insert new ones. Returns chunk count."""
    pool = await get_pool()
    async with pool.acquire() as conn:
        async with conn.transaction():
            await conn.execute(
                """
                DELETE FROM knowledge_chunk
                WHERE asset_id = $1
                """,
                asset_id,
            )
            for chunk_index, chunk_text, token_count, embedding in chunks:
                await conn.execute(
                    """
                    INSERT INTO knowledge_chunk (
                        tenant_id, project_id, asset_id, chunk_index,
                        chunk_text, token_count, embedding
                    ) VALUES ($1, $2, $3, $4, $5, $6, $7::vector)
                    """,
                    tenant_id,
                    project_id,
                    asset_id,
                    chunk_index,
                    chunk_text,
                    token_count,
                    vector_to_pg(embedding),
                )
    return len(chunks)


async def search_chunks(
    *,
    tenant_id: int,
    project_id: int,
    query_vector: list[float],
    top_k: int,
) -> list[asyncpg.Record]:
    pool = await get_pool()
    return await pool.fetch(
        """
        SELECT id, asset_id, chunk_index, chunk_text,
               1 - (embedding <=> $1::vector) AS score
        FROM knowledge_chunk
        WHERE tenant_id = $2
          AND project_id = $3
          AND deleted_at IS NULL
          AND embedding IS NOT NULL
        ORDER BY embedding <=> $1::vector
        LIMIT $4
        """,
        vector_to_pg(query_vector),
        tenant_id,
        project_id,
        top_k,
    )
