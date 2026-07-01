#!/usr/bin/env python3
"""Smoke EPIC-6 M1: landing pages CRUD + generate (Java :8080 + inbound-ai :8090)."""
from __future__ import annotations

import os
import sys
import urllib.error

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _http, login  # noqa: E402

PROJECT_ID = int(os.environ.get("LANDING_PROJECT_ID", "1"))
TEMPLATE_TYPE = os.environ.get("LANDING_TEMPLATE_TYPE", "destination")

LANDING_MODULE_KEYS = (
    "hero",
    "why_this_trip",
    "itinerary",
    "what_we_provide",
    "traveler_reviews",
    "faq",
    "lead_form",
    "whatsapp_cta",
)


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

    list_path = f"/api/v1/projects/{PROJECT_ID}/landing-pages?pageNum=1&pageSize=10"
    print(f"3. GET {list_path}")
    listed = _http("GET", list_path, headers=auth)
    if listed.get("code") != 200:
        raise RuntimeError(f"list failed: {listed}")
    before_total = int(listed.get("total") or 0)
    print(f"   total={before_total}")

    create_path = f"/api/v1/projects/{PROJECT_ID}/landing-pages"
    print(f"4. POST {create_path}")
    created = _http(
        "POST",
        create_path,
        body={
            "keywordId": keyword_id,
            "templateType": TEMPLATE_TYPE,
            "language": "en",
            "targetMarket": "US",
        },
        headers=auth,
    )
    if created.get("code") != 200:
        raise RuntimeError(f"create failed: {created}")
    page_id = int(created["data"])
    print(f"   pageId={page_id}")

    gen_path = f"/api/v1/projects/{PROJECT_ID}/landing-pages/{page_id}/generate"
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
        f"   pageId={gen_data.get('pageId')} moduleCount={gen_data.get('moduleCount')} "
        f"needsHumanReview={gen_data.get('needsHumanReview')}"
    )
    if int(gen_data.get("moduleCount") or 0) < len(LANDING_MODULE_KEYS):
        raise RuntimeError(f"expected >= {len(LANDING_MODULE_KEYS)} modules, got: {gen_data}")

    detail_path = f"/api/v1/projects/{PROJECT_ID}/landing-pages/{page_id}"
    print(f"6. GET {detail_path}")
    detail = _http("GET", detail_path, headers=auth)
    if detail.get("code") != 200:
        raise RuntimeError(f"detail failed: {detail}")
    data = detail.get("data") or {}
    if not data.get("title"):
        raise RuntimeError(f"missing title: {data}")
    content_json = data.get("contentJson") or {}
    modules = content_json.get("modules") or []
    module_keys = [m.get("key") for m in modules if isinstance(m, dict)]
    for key in LANDING_MODULE_KEYS:
        if key not in module_keys:
            raise RuntimeError(f"missing module key {key!r}: {module_keys}")
    seo = data.get("seoMetaJson") or {}
    for key in ("title", "description"):
        if not seo.get(key):
            raise RuntimeError(f"missing seoMetaJson.{key}: {seo}")
    form = data.get("formConfigJson") or {}
    if not form.get("fields"):
        raise RuntimeError(f"missing formConfigJson.fields: {form}")
    print(f"   title={str(data.get('title'))[:60]!r} modules={len(modules)}")

    print("7. GET list again")
    listed2 = _http("GET", list_path, headers=auth)
    after_total = int(listed2.get("total") or 0)
    if after_total < before_total + 1:
        raise RuntimeError("list total did not increase after create")
    print(f"   total={after_total}")

    del_path = f"/api/v1/projects/{PROJECT_ID}/landing-pages/{page_id}"
    print(f"8. DELETE {del_path}")
    deleted = _http("DELETE", del_path, headers=auth)
    if deleted.get("code") != 200:
        raise RuntimeError(f"delete failed: {deleted}")
    print("   OK")

    print("EPIC-6 landing API smoke passed")
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
