#!/usr/bin/env python3
"""Smoke FR-005: knowledge RAG search via Java proxy (requires Java :8080, asset#1 READY)."""
from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _http, login  # noqa: E402

BASE = os.environ.get("INBOUND_API_BASE", "http://localhost:8080")
PROJECT_ID = int(os.environ.get("KNOWLEDGE_PROJECT_ID", "1"))
QUERY = os.environ.get(
    "KNOWLEDGE_RAG_QUERY",
    "Dragon Journey Travel private China tours for English-speaking travelers",
)


def main() -> int:
    print("1. Login...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    path = f"/api/v1/projects/{PROJECT_ID}/knowledge-assets/search"
    print(f"2. POST {path}")
    data = _http(
        "POST",
        path,
        body={"query": QUERY, "topK": 3},
        headers=auth,
    )

    if data.get("code") != 200:
        raise RuntimeError(f"search failed: {data}")

    hits = (data.get("data") or {}).get("hits") or []
    print(f"   hits={len(hits)}")
    if not hits:
        raise RuntimeError("expected >=1 hit for READY knowledge base")

    top = hits[0]
    for key in ("chunkId", "assetId", "chunkText", "score"):
        if key not in top:
            raise RuntimeError(f"missing field {key}: {top}")
    print(f"   top chunkId={top['chunkId']} assetId={top['assetId']} score={top['score']}")
    print("FR-005 RAG search smoke passed")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
