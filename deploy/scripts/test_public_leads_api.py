#!/usr/bin/env python3
"""Smoke EPIC-7 M1: public lead submit + Admin list/detail (Java :8080)."""
from __future__ import annotations

import os
import sys
import urllib.error

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _http, login  # noqa: E402

PROJECT_ID = int(os.environ.get("LEAD_PROJECT_ID", "1"))
LANDING_PAGE_ID = os.environ.get("LEAD_LANDING_PAGE_ID")


def _pick_landing_page_id(auth: dict) -> int:
    if LANDING_PAGE_ID:
        return int(LANDING_PAGE_ID)
    path = f"/api/v1/projects/{PROJECT_ID}/landing-pages?pageNum=1&pageSize=1"
    listed = _http("GET", path, headers=auth)
    if listed.get("code") != 200:
        raise RuntimeError(f"landing pages list failed: {listed}")
    rows = listed.get("rows") or []
    if not rows:
        raise RuntimeError("no landing pages; run test_landing_api.py first or set LEAD_LANDING_PAGE_ID")
    return int(rows[0]["id"])


def main() -> int:
    print("1. Login (for Admin list + landing page lookup)...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    page_id = _pick_landing_page_id(auth)
    print(f"2. Using landingPageId={page_id}")

    list_path = f"/api/v1/projects/{PROJECT_ID}/leads?pageNum=1&pageSize=10"
    print(f"3. GET {list_path}")
    listed = _http("GET", list_path, headers=auth)
    if listed.get("code") != 200:
        raise RuntimeError(f"list failed: {listed}")
    before_total = int(listed.get("total") or 0)
    print(f"   total={before_total}")

    public_path = "/api/v1/public/leads"
    print(f"4. POST {public_path} (no auth)")
    submitted = _http(
        "POST",
        public_path,
        body={
            "landingPageId": page_id,
            "name": "Smoke Test Lead",
            "email": "smoke.lead@example.com",
            "phone": "+1-555-0100",
            "travelDate": "2026-09-15",
            "partySize": 2,
            "budget": "USD 3000",
            "message": "EPIC-7 smoke test inquiry",
            "source": "form",
            "utm": {
                "utm_source": "smoke",
                "utm_medium": "test",
                "utm_campaign": "epic7",
            },
            "device": "Mozilla/5.0 (smoke test)",
        },
    )
    if submitted.get("code") != 200:
        raise RuntimeError(f"public submit failed: {submitted}")
    lead_id = int((submitted.get("data") or {}).get("leadId") or 0)
    if lead_id <= 0:
        raise RuntimeError(f"missing leadId: {submitted}")
    print(f"   leadId={lead_id}")

    print("5. POST public leads without email/phone (expect error)")
    bad = _http(
        "POST",
        public_path,
        body={"landingPageId": page_id, "name": "No Contact"},
    )
    if bad.get("code") == 200:
        raise RuntimeError("expected validation error when email and phone both missing")
    print(f"   rejected code={bad.get('code')} msg={bad.get('msg')!r}")

    print(f"6. GET {list_path}")
    listed2 = _http("GET", list_path, headers=auth)
    if listed2.get("code") != 200:
        raise RuntimeError(f"list after submit failed: {listed2}")
    after_total = int(listed2.get("total") or 0)
    if after_total < before_total + 1:
        raise RuntimeError("list total did not increase after public submit")
    rows = listed2.get("rows") or []
    found = next((r for r in rows if int(r.get("id") or 0) == lead_id), None)
    if not found:
        raise RuntimeError(f"lead {lead_id} not in list rows")
    if found.get("status") != "NEW":
        raise RuntimeError(f"expected status NEW, got {found.get('status')!r}")
    if not found.get("landingPageTitle"):
        raise RuntimeError(f"missing landingPageTitle join: {found}")
    print(f"   total={after_total} status={found.get('status')}")

    detail_path = f"/api/v1/projects/{PROJECT_ID}/leads/{lead_id}"
    print(f"7. GET {detail_path}")
    detail = _http("GET", detail_path, headers=auth)
    if detail.get("code") != 200:
        raise RuntimeError(f"detail failed: {detail}")
    data = detail.get("data") or {}
    if data.get("email") != "smoke.lead@example.com":
        raise RuntimeError(f"unexpected email: {data}")
    utm = data.get("utm") or {}
    if utm.get("utm_source") != "smoke":
        raise RuntimeError(f"unexpected utm: {utm}")
    print(f"   email={data.get('email')} source={data.get('source')}")

    print("EPIC-7 public leads API smoke passed")
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
