#!/usr/bin/env python3
"""Smoke EPIC-7 M2: lead CRM status + followups (Java :8080)."""
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
    print("1. Login...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    page_id = _pick_landing_page_id(auth)
    print(f"2. Using landingPageId={page_id}")

    public_path = "/api/v1/public/leads"
    print(f"3. POST {public_path}")
    submitted = _http(
        "POST",
        public_path,
        body={
            "landingPageId": page_id,
            "name": "CRM Smoke Lead",
            "email": "crm.smoke@example.com",
            "phone": "+1-555-0200",
            "message": "EPIC-7 M2 CRM smoke",
            "source": "form",
        },
    )
    if submitted.get("code") != 200:
        raise RuntimeError(f"public submit failed: {submitted}")
    lead_id = int((submitted.get("data") or {}).get("leadId") or 0)
    if lead_id <= 0:
        raise RuntimeError(f"missing leadId: {submitted}")
    print(f"   leadId={lead_id}")

    patch_path = f"/api/v1/projects/{PROJECT_ID}/leads/{lead_id}"
    print(f"4. PATCH {patch_path} status=FOLLOWING")
    patched = _http("PATCH", patch_path, body={"status": "FOLLOWING"}, headers=auth)
    if patched.get("code") != 200:
        raise RuntimeError(f"patch FOLLOWING failed: {patched}")
    if (patched.get("data") or {}).get("status") != "FOLLOWING":
        raise RuntimeError(f"expected FOLLOWING: {patched}")

    followups_path = f"{patch_path}/followups"
    print(f"5. POST {followups_path}")
    created_fu = _http(
        "POST",
        followups_path,
        body={"content": "Called prospect, interested in September tour.", "channel": "email"},
        headers=auth,
    )
    if created_fu.get("code") != 200:
        raise RuntimeError(f"create followup failed: {created_fu}")
    fu_data = created_fu.get("data") or {}
    if not fu_data.get("content"):
        raise RuntimeError(f"missing followup content: {created_fu}")
    print(f"   followupId={fu_data.get('id')}")

    print(f"6. PATCH {patch_path} status=QUOTED")
    quoted = _http("PATCH", patch_path, body={"status": "QUOTED"}, headers=auth)
    if quoted.get("code") != 200:
        raise RuntimeError(f"patch QUOTED failed: {quoted}")
    if (quoted.get("data") or {}).get("status") != "QUOTED":
        raise RuntimeError(f"expected QUOTED: {quoted}")

    print(f"7. GET {followups_path}")
    listed_fu = _http("GET", followups_path, headers=auth)
    if listed_fu.get("code") != 200:
        raise RuntimeError(f"list followups failed: {listed_fu}")
    followups = listed_fu.get("data") or []
    if len(followups) < 1:
        raise RuntimeError(f"expected followups: {listed_fu}")

    print(f"8. GET {patch_path}")
    detail = _http("GET", patch_path, headers=auth)
    if detail.get("code") != 200:
        raise RuntimeError(f"detail failed: {detail}")
    data = detail.get("data") or {}
    if data.get("status") != "QUOTED":
        raise RuntimeError(f"detail status expected QUOTED: {data}")
    detail_followups = data.get("followups") or []
    if len(detail_followups) < 1:
        raise RuntimeError(f"detail missing followups: {data}")

    print(f"9. PATCH invalid NEW→QUOTED (expect error)")
    bad = _http("PATCH", patch_path, body={"status": "NEW"}, headers=auth)
    if bad.get("code") == 200:
        raise RuntimeError("expected invalid status transition to fail")

    print(f"EPIC-7 leads CRM smoke passed leadId={lead_id} followups={len(detail_followups)}")
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
