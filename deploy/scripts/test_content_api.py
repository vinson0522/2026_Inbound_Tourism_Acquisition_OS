#!/usr/bin/env python3
"""Smoke EPIC-4 M1: content tasks CRUD + generate (Java :8080 + inbound-ai :8090)."""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _http, login  # noqa: E402

PROJECT_ID = int(os.environ.get("CONTENT_PROJECT_ID", "1"))
PLATFORM = os.environ.get("CONTENT_PLATFORM", "tiktok")
DURATION = int(os.environ.get("CONTENT_DURATION", "30"))


def _pick_keyword_id(auth: dict) -> int:
    path = f"/api/v1/projects/{PROJECT_ID}/keywords?pageNum=1&pageSize=1"
    listed = _http("GET", path, headers=auth)
    if listed.get("code") != 200:
        raise RuntimeError(f"keywords list failed: {listed}")
    rows = listed.get("rows") or []
    if not rows:
        raise RuntimeError("no keywords in project; run test_keywords_api.py first or seed demo data")
    return int(rows[0]["id"])


def main() -> int:
    print("1. Login...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    keyword_id = _pick_keyword_id(auth)
    print(f"2. Using keywordId={keyword_id}")

    list_path = f"/api/v1/projects/{PROJECT_ID}/content-tasks?pageNum=1&pageSize=10"
    print(f"3. GET {list_path}")
    listed = _http("GET", list_path, headers=auth)
    if listed.get("code") != 200:
        raise RuntimeError(f"list failed: {listed}")
    before_total = int(listed.get("total") or 0)
    print(f"   total={before_total}")

    create_path = f"/api/v1/projects/{PROJECT_ID}/content-tasks"
    print(f"4. POST {create_path}")
    created = _http(
        "POST",
        create_path,
        body={
            "keywordId": keyword_id,
            "platform": PLATFORM,
            "format": "short_video",
            "duration": DURATION,
            "tone": "friendly",
            "language": "en",
            "targetMarket": "US",
        },
        headers=auth,
    )
    if created.get("code") != 200:
        raise RuntimeError(f"create failed: {created}")
    task_id = int(created["data"])
    print(f"   taskId={task_id}")

    gen_path = f"/api/v1/projects/{PROJECT_ID}/content-tasks/{task_id}/generate"
    print(f"5. POST {gen_path} (useRag=false)")
    generated = _http(
        "POST",
        gen_path,
        body={"useRag": False},
        headers=auth,
    )
    if generated.get("code") != 200:
        raise RuntimeError(f"generate failed: {generated}")
    gen_data = generated.get("data") or {}
    print(
        f"   contentId={gen_data.get('contentId')} version={gen_data.get('version')} "
        f"needsHumanReview={gen_data.get('needsHumanReview')}"
    )
    if not gen_data.get("contentId"):
        raise RuntimeError("generate did not return contentId")

    detail_path = f"/api/v1/projects/{PROJECT_ID}/content-tasks/{task_id}"
    print(f"6. GET {detail_path}")
    detail = _http("GET", detail_path, headers=auth)
    if detail.get("code") != 200:
        raise RuntimeError(f"detail failed: {detail}")
    data = detail.get("data") or {}
    gc = data.get("generatedContent") or {}
    for key in ("hook", "script", "voiceover", "onScreenText", "cta"):
        if not gc.get(key):
            raise RuntimeError(f"missing generatedContent.{key}: {gc}")
    storyboard = gc.get("storyboardJson") or []
    if len(storyboard) < 1:
        raise RuntimeError(f"expected storyboardJson scenes, got: {storyboard}")
    print(f"   hook={str(gc.get('hook'))[:60]!r} scenes={len(storyboard)}")

    print(f"7. GET list again")
    listed2 = _http("GET", list_path, headers=auth)
    after_total = int(listed2.get("total") or 0)
    if after_total < before_total + 1:
        raise RuntimeError("list total did not increase after create")
    print(f"   total={after_total}")

    del_path = f"/api/v1/projects/{PROJECT_ID}/content-tasks/{task_id}"
    print(f"8. DELETE {del_path}")
    deleted = _http("DELETE", del_path, headers=auth)
    if deleted.get("code") != 200:
        raise RuntimeError(f"delete failed: {deleted}")
    print("   OK")

    print("EPIC-4 content API smoke passed")
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
