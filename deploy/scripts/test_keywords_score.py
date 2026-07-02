#!/usr/bin/env python3
"""Smoke EPIC-3 M2: keyword score single + list assert (Java :8080 + inbound-ai :8090)."""
from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _http, login  # noqa: E402

PROJECT_ID = int(os.environ.get("KEYWORDS_PROJECT_ID", "1"))


def _pick_keyword_id(rows: list[dict]) -> int:
    for row in rows:
        kid = row.get("id")
        if kid is not None:
            return int(kid)
    raise RuntimeError("no keyword rows to score")


def main() -> int:
    print("1. Login...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    list_path = f"/api/v1/projects/{PROJECT_ID}/keywords?pageNum=1&pageSize=20"
    print(f"2. GET {list_path}")
    listed = _http("GET", list_path, headers=auth)
    if listed.get("code") != 200:
        raise RuntimeError(f"list failed: {listed}")
    rows = listed.get("rows") or []
    if not rows:
        raise RuntimeError("no keywords — run test_keywords_api.py first or create keywords")
    keyword_id = _pick_keyword_id(rows)
    print(f"   keywordId={keyword_id} sample={rows[0].get('keyword', '')[:50]!r}")

    score_path = f"/api/v1/projects/{PROJECT_ID}/keywords/{keyword_id}/score?useRag=false"
    print(f"3. POST {score_path}")
    scored = _http("POST", score_path, body={}, headers=auth)
    if scored.get("code") != 200:
        raise RuntimeError(f"score failed: {scored}")
    data = scored.get("data") or {}
    score = data.get("score")
    detail = data.get("scoreDetailJson") or data.get("score_detail_json") or {}
    print(f"   score={score} weights={detail.get('weights_version') or detail.get('weightsVersion')}")
    if score is None:
        raise RuntimeError(f"score is null: {data}")

    sort_path = (
        f"/api/v1/projects/{PROJECT_ID}/keywords?pageNum=1&pageSize=5"
        f"&orderByColumn=score&isAsc=desc"
    )
    print(f"4. GET {sort_path}")
    sorted_list = _http("GET", sort_path, headers=auth)
    if sorted_list.get("code") != 200:
        raise RuntimeError(f"sorted list failed: {sorted_list}")
    hit = next((r for r in (sorted_list.get("rows") or []) if r.get("id") == keyword_id), None)
    if not hit or hit.get("score") is None:
        raise RuntimeError(f"keyword {keyword_id} score not persisted in list: {hit}")

    batch_path = f"/api/v1/projects/{PROJECT_ID}/keywords/score-batch"
    print(f"5. POST {batch_path} (single id batch)")
    batched = _http(
        "POST",
        batch_path,
        body={"keywordIds": [keyword_id], "useRag": False},
        headers=auth,
    )
    if batched.get("code") != 200:
        raise RuntimeError(f"batch score failed: {batched}")
    batch_data = batched.get("data") or {}
    if int(batch_data.get("scoredCount") or 0) < 1:
        raise RuntimeError(f"batch scoredCount unexpected: {batch_data}")

    print(f"EPIC-3 keyword score smoke passed keywordId={keyword_id} score={hit.get('score')}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
