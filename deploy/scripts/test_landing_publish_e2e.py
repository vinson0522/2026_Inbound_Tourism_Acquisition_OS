#!/usr/bin/env python3
"""Smoke EPIC-6 M2: publish → public GET → public lead → unpublish (Java :8080)."""
from __future__ import annotations

import os
import sys
import urllib.error

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _http, login  # noqa: E402

PROJECT_ID = int(os.environ.get("LANDING_PROJECT_ID", "1"))
PUBLIC_BASE = os.environ.get("LANDING_PUBLIC_BASE_URL", "http://localhost:4321")


def _pick_keyword_id(auth: dict) -> int:
    path = f"/api/v1/projects/{PROJECT_ID}/keywords?pageNum=1&pageSize=1"
    listed = _http("GET", path, headers=auth)
    if listed.get("code") != 200:
        raise RuntimeError(f"keywords list failed: {listed}")
    rows = listed.get("rows") or []
    if not rows:
        raise RuntimeError("no keywords; run test_keywords_api.py first")
    return int(rows[0]["id"])


def _ensure_page_with_content(auth: dict) -> tuple[int, str]:
    page_id_env = os.environ.get("LANDING_PAGE_ID")
    if page_id_env:
        page_id = int(page_id_env)
        detail = _http(
            "GET",
            f"/api/v1/projects/{PROJECT_ID}/landing-pages/{page_id}",
            headers=auth,
        )
        if detail.get("code") != 200:
            raise RuntimeError(f"landing detail failed: {detail}")
        data = detail.get("data") or {}
        slug = data.get("slug")
        content = data.get("contentJson") or {}
        if not slug or not content:
            raise RuntimeError(f"LANDING_PAGE_ID={page_id} missing slug/content")
        return page_id, slug

    keyword_id = _pick_keyword_id(auth)
    created = _http(
        "POST",
        f"/api/v1/projects/{PROJECT_ID}/landing-pages",
        body={"keywordId": keyword_id, "templateType": "destination"},
        headers=auth,
    )
    if created.get("code") != 200:
        raise RuntimeError(f"create failed: {created}")
    page_id = int(created["data"])

    generated = _http(
        "POST",
        f"/api/v1/projects/{PROJECT_ID}/landing-pages/{page_id}/generate",
        body={"useRag": False},
        headers=auth,
    )
    if generated.get("code") != 200:
        raise RuntimeError(f"generate failed: {generated}")

    detail = _http(
        "GET",
        f"/api/v1/projects/{PROJECT_ID}/landing-pages/{page_id}",
        headers=auth,
    )
    if detail.get("code") != 200:
        raise RuntimeError(f"detail failed: {detail}")
    data = detail.get("data") or {}
    slug = data.get("slug")
    if not slug:
        raise RuntimeError(f"missing slug after generate: {data}")
    return page_id, slug


def main() -> int:
    print("1. Login...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    page_id, slug = _ensure_page_with_content(auth)
    print(f"2. Using pageId={page_id} slug={slug!r}")

    publish_path = f"/api/v1/projects/{PROJECT_ID}/landing-pages/{page_id}/publish"
    print(f"3. POST {publish_path}")
    published = _http("POST", publish_path, headers=auth)
    if published.get("code") != 200:
        raise RuntimeError(f"publish failed: {published}")
    pub_data = published.get("data") or {}
    published_url = pub_data.get("publishedUrl") or ""
    expected_suffix = f"/p/{PROJECT_ID}/{slug}"
    if expected_suffix not in published_url:
        raise RuntimeError(f"unexpected publishedUrl: {published_url}")
    print(f"   publishedUrl={published_url}")

    public_path = f"/api/v1/public/landing-pages/{slug}?projectId={PROJECT_ID}"
    print(f"4. GET {public_path} (no auth)")
    public = _http("GET", public_path)
    if public.get("code") != 200:
        raise RuntimeError(f"public GET failed: {public}")
    pub_page = public.get("data") or {}
    if pub_page.get("id") != page_id:
        raise RuntimeError(f"public id mismatch: {pub_page}")
    modules = (pub_page.get("contentJson") or {}).get("modules") or []
    if not modules:
        raise RuntimeError(f"public contentJson.modules empty: {pub_page}")
    print(f"   title={pub_page.get('title')!r} modules={len(modules)}")

    print("5. POST /api/v1/public/leads")
    lead = _http(
        "POST",
        "/api/v1/public/leads",
        body={
            "landingPageId": page_id,
            "name": "Publish E2E Lead",
            "email": "publish.e2e@example.com",
            "message": "EPIC-6 M2 publish smoke",
            "source": "form",
        },
    )
    if lead.get("code") != 200:
        raise RuntimeError(f"public lead failed: {lead}")
    lead_id = int((lead.get("data") or {}).get("leadId") or 0)
    if lead_id <= 0:
        raise RuntimeError(f"missing leadId: {lead}")
    print(f"   leadId={lead_id}")

    unpublish_path = f"/api/v1/projects/{PROJECT_ID}/landing-pages/{page_id}/unpublish"
    print(f"6. POST {unpublish_path}")
    unpublished = _http("POST", unpublish_path, headers=auth)
    if unpublished.get("code") != 200:
        raise RuntimeError(f"unpublish failed: {unpublished}")
    if (unpublished.get("data") or {}).get("status") != "DRAFT":
        raise RuntimeError(f"expected DRAFT after unpublish: {unpublished}")

    print(f"7. GET {public_path} (expect not found)")
    gone = _http("GET", public_path)
    if gone.get("code") == 200:
        raise RuntimeError("public GET should fail after unpublish")
    print(f"   code={gone.get('code')} (unpublished)")

    print(f"E2E passed: pageId={page_id} slug={slug} leadId={lead_id}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
