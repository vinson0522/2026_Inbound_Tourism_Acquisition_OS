#!/usr/bin/env python3
"""Smoke EPIC-6 M2 Astro: published page renders 8 modules + 404 when unpublished."""
from __future__ import annotations

import os
import sys
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _http, login  # noqa: E402

PROJECT_ID = int(os.environ.get("LANDING_PROJECT_ID", "1"))
LANDING_BASE = os.environ.get("LANDING_PUBLIC_BASE_URL", "http://localhost:4321")
PAGE_ID = int(os.environ.get("LANDING_PAGE_ID", "8"))


def main() -> int:
    auth = {"Authorization": f"Bearer {login()}"}
    detail = _http(
        "GET",
        f"/api/v1/projects/{PROJECT_ID}/landing-pages/{PAGE_ID}",
        headers=auth,
    )
    if detail.get("code") != 200:
        raise RuntimeError(f"landing detail failed: {detail}")
    slug = (detail.get("data") or {}).get("slug")
    if not slug:
        raise RuntimeError(f"page {PAGE_ID} missing slug")

    _http(
        "POST",
        f"/api/v1/projects/{PROJECT_ID}/landing-pages/{PAGE_ID}/publish",
        headers=auth,
    )

    url = f"{LANDING_BASE.rstrip('/')}/p/{PROJECT_ID}/{slug}"
    req = urllib.request.Request(url, headers={"User-Agent": "astro-smoke"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        html = resp.read().decode("utf-8", errors="replace")
        status = resp.status
    print(f"Astro GET {url} -> {status} len={len(html)}")

    checks = [
        "lp-hero",
        "lp-section",
        "lp-faq",
        'id="lead-form"',
        "inbound-lead-form",
        "lp-wa-sticky",
        "application/ld+json",
    ]
    missing = [c for c in checks if c not in html]
    if missing:
        raise RuntimeError(f"HTML missing markers: {missing}")

    bad = f"{LANDING_BASE.rstrip('/')}/p/{PROJECT_ID}/definitely-not-a-slug-xyz"
    try:
        urllib.request.urlopen(bad, timeout=15)
        raise RuntimeError("expected 404 for bad slug")
    except urllib.error.HTTPError as e:
        if e.code != 404:
            raise RuntimeError(f"expected 404, got {e.code}") from e
        body = e.read().decode("utf-8", errors="replace")
        if "lp-not-found" not in body and "Page not found" not in body:
            raise RuntimeError("404 page missing friendly copy")
    print("404 bad slug OK")

    _http(
        "POST",
        f"/api/v1/projects/{PROJECT_ID}/landing-pages/{PAGE_ID}/unpublish",
        headers=auth,
    )
    print(f"Astro E2E passed: pageId={PAGE_ID} slug={slug}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
