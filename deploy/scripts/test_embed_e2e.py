#!/usr/bin/env python3
"""E2E: embed demo knowledge asset → knowledge_chunk READY (EPIC-10 Phase 2).

Requires local Docker (ADR-09):
  postgres :5432, rabbitmq :5672, ai-api :8090 with EMBED_WORKER_ENABLED=true
Optional Java :8080 for MQ trigger via POST .../knowledge-assets/{id}/reindex
"""
from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.request

AI_BASE = os.environ.get("AI_SERVICE_BASE", "http://localhost:8090")
JAVA_BASE = os.environ.get("INBOUND_API_BASE", "http://localhost:8080")
TOKEN = os.environ.get("AI_SERVICE_INTERNAL_TOKEN", "dev_internal_token_change_me")
ASSET_ID = int(os.environ.get("EMBED_ASSET_ID", "1"))
TENANT_ID = int(os.environ.get("EMBED_TENANT_ID", "1"))
PROJECT_ID = int(os.environ.get("EMBED_PROJECT_ID", "1"))
POLL_SEC = int(os.environ.get("EMBED_POLL_SEC", "60"))
MODE = os.environ.get("EMBED_E2E_MODE", "direct")  # direct | mq


def _http_json(method: str, url: str, body: dict | None = None, headers: dict | None = None) -> dict:
    hdrs = {"Content-Type": "application/json"}
    if headers:
        hdrs.update(headers)
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, headers=hdrs, method=method)
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode())


def ensure_demo_asset_pg() -> int:
    global ASSET_ID
    try:
        import psycopg2
    except ImportError:
        print("   skip ensure asset (psycopg2 not installed)")
        return ASSET_ID
    dsn = os.environ.get(
        "EMBED_PG_DSN",
        "postgresql://inbound:inbound_dev_pass@127.0.0.1:5432/inbound_growth",
    )
    conn = psycopg2.connect(dsn)
    conn.autocommit = True
    cur = conn.cursor()
    cur.execute("SELECT id FROM knowledge_asset WHERE id=%s", (ASSET_ID,))
    if cur.fetchone():
        conn.close()
        return ASSET_ID
    cur.execute(
        """
        INSERT INTO knowledge_asset (tenant_id, project_id, type, title, content, vector_status, created_by)
        VALUES (%s, %s, 'DOCUMENT', 'Demo Brand Overview',
                'Dragon Journey Travel private China tours for English-speaking travelers.',
                'PENDING', 1)
        RETURNING id
        """,
        (TENANT_ID, PROJECT_ID),
    )
    new_id = cur.fetchone()[0]
    conn.close()
    ASSET_ID = int(new_id)
    print(f"   inserted demo knowledge_asset id={ASSET_ID}")
    return ASSET_ID


def poll_vector_status() -> str:
    import psycopg2

    dsn = os.environ.get(
        "EMBED_PG_DSN",
        "postgresql://inbound:inbound_dev_pass@127.0.0.1:5432/inbound_growth",
    )
    conn = psycopg2.connect(dsn)
    cur = conn.cursor()
    cur.execute(
        "SELECT vector_status::text FROM knowledge_asset WHERE id=%s",
        (ASSET_ID,),
    )
    row = cur.fetchone()
    conn.close()
    return row[0] if row else "MISSING"


def poll_chunk_count() -> int:
    import psycopg2

    dsn = os.environ.get(
        "EMBED_PG_DSN",
        "postgresql://inbound:inbound_dev_pass@127.0.0.1:5432/inbound_growth",
    )
    conn = psycopg2.connect(dsn)
    cur = conn.cursor()
    cur.execute(
        "SELECT COUNT(*) FROM knowledge_chunk WHERE asset_id=%s AND deleted_at IS NULL",
        (ASSET_ID,),
    )
    count = cur.fetchone()[0]
    conn.close()
    return int(count)


def trigger_direct_embed() -> dict:
    body = {
        "assetId": ASSET_ID,
        "tenantId": TENANT_ID,
        "projectId": PROJECT_ID,
    }
    resp = _http_json(
        "POST",
        f"{AI_BASE}/ai/embed",
        body=body,
        headers={"Authorization": f"Bearer {TOKEN}"},
    )
    if resp.get("code") != 0:
        raise RuntimeError(f"/ai/embed failed: {resp}")
    data = resp.get("data") or {}
    print(f"   direct embed: status={data.get('vector_status')} chunks={data.get('chunk_count')}")
    return data


def trigger_mq_embed() -> None:
    try:
        import pika
    except ImportError:
        raise RuntimeError("pika required for mq mode: pip install pika") from None
    url = os.environ.get("RABBITMQ_URL", "amqp://inbound:inbound_dev_pass@127.0.0.1:5672/")
    params = pika.URLParameters(url)
    conn = pika.BlockingConnection(params)
    ch = conn.channel()
    ch.queue_declare(queue="ai.embed", durable=True)
    payload = json.dumps(
        {
            "trace_id": "embed-e2e",
            "assetId": ASSET_ID,
            "tenantId": TENANT_ID,
            "projectId": PROJECT_ID,
        }
    )
    ch.basic_publish(exchange="", routing_key="ai.embed", body=payload.encode())
    conn.close()
    print("   published ai.embed message")


def main() -> int:
    print("1. Ensure demo knowledge_asset exists...")
    ensure_demo_asset_pg()

    print(f"2. Trigger embed (mode={MODE})...")
    if MODE == "mq":
        trigger_mq_embed()
    else:
        data = trigger_direct_embed()
        if data.get("vector_status") == "READY" and int(data.get("chunk_count") or 0) > 0:
            print(f"E2E passed: vector_status=READY chunks={data.get('chunk_count')}")
            return 0
        try:
            status = poll_vector_status()
            chunks = poll_chunk_count()
        except ImportError:
            print("E2E failed: embed response not READY and psycopg2 unavailable for DB poll")
            return 1
        if status == "READY" and chunks > 0:
            print(f"E2E passed: vector_status=READY chunks={chunks}")
            return 0
        print(f"   unexpected direct result status={status} chunks={chunks}")
        return 1

    print(f"3. Poll vector_status (max {POLL_SEC}s)...")
    deadline = time.time() + POLL_SEC
    while time.time() < deadline:
        status = poll_vector_status()
        chunks = poll_chunk_count()
        print(f"   status={status} chunks={chunks}")
        if status == "READY" and chunks > 0:
            print(f"E2E passed: vector_status=READY chunks={chunks}")
            return 0
        time.sleep(2)

    print("E2E failed: timeout waiting for READY")
    return 1


if __name__ == "__main__":
    sys.exit(main())
