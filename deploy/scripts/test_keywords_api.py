#!/usr/bin/env python3
"""Smoke EPIC-3 M1: keywords list + AI generate (Java :8080 + inbound-ai :8090)."""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _http, login  # noqa: E402

PROJECT_ID = int(os.environ.get("KEYWORDS_PROJECT_ID", "1"))
MARKET = os.environ.get("KEYWORDS_MARKET", "US")
STAGE = os.environ.get("KEYWORDS_STAGE", "inspiration")
WORDS_PER_STAGE = int(os.environ.get("KEYWORDS_WORDS_PER_STAGE", "3"))


def main() -> int:
    print("1. Login...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    list_path = f"/api/v1/projects/{PROJECT_ID}/keywords?stage={STAGE}&pageNum=1&pageSize=10"
    print(f"2. GET {list_path}")
    listed = _http("GET", list_path, headers=auth)
    if listed.get("code") != 200:
        raise RuntimeError(f"list failed: {listed}")
    before_total = int(listed.get("total") or 0)
    print(f"   total={before_total}")

    gen_path = f"/api/v1/projects/{PROJECT_ID}/keywords/generate"
    print(f"3. POST {gen_path} (stage={STAGE}, wordsPerStage={WORDS_PER_STAGE})")
    generated = _http(
        "POST",
        gen_path,
        body={
            "market": MARKET,
            "locale": "en",
            "stages": [STAGE],
            "wordsPerStage": WORDS_PER_STAGE,
            "useRag": False,
        },
        headers=auth,
    )
    if generated.get("code") != 200:
        raise RuntimeError(f"generate failed: {generated}")

    data = generated.get("data") or {}
    inserted = int(data.get("insertedCount") or 0)
    print(f"   insertedCount={inserted} needsHumanReview={data.get('needsHumanReview')}")
    if inserted < WORDS_PER_STAGE:
        raise RuntimeError(f"expected >= {WORDS_PER_STAGE} inserted, got {inserted}")

    print(f"4. GET list again (stage={STAGE})")
    listed2 = _http("GET", list_path, headers=auth)
    after_total = int(listed2.get("total") or 0)
    rows = listed2.get("rows") or []
    print(f"   total={after_total} rows={len(rows)}")
    if after_total < before_total + inserted:
        raise RuntimeError(f"total did not increase by {inserted}")

    sample = rows[0] if rows else {}
    for key in ("keyword", "market", "stage", "status"):
        if key not in sample:
            raise RuntimeError(f"missing field {key}: {sample}")
    source = sample.get("sourceJson") or {}
    if source.get("source") != "ai":
        print(f"   warn: sourceJson.source={source.get('source')}")

    print(f"   sample keyword={sample.get('keyword')!r} stage={sample.get('stage')}")
    print("EPIC-3 keywords API smoke passed")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except urllib.error.HTTPError as e:
        print(e.read().decode(), file=sys.stderr)
        raise SystemExit(1)
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
