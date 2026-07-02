#!/usr/bin/env python3
"""Smoke EPIC-7 M3: WhatsApp click beacon + lead detail stats + AI followup suggestion."""
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

    public_leads = "/api/v1/public/leads"
    print(f"3. POST {public_leads}")
    submitted = _http(
        "POST",
        public_leads,
        body={
            "landingPageId": page_id,
            "name": "WhatsApp Smoke Lead",
            "email": "whatsapp.smoke@example.com",
            "message": "Interested in private Great Wall tour",
            "budget": "USD 3000-5000",
            "source": "form",
        },
    )
    if submitted.get("code") != 200:
        raise RuntimeError(f"public submit failed: {submitted}")
    lead_id = int((submitted.get("data") or {}).get("leadId") or 0)
    if lead_id <= 0:
        raise RuntimeError(f"missing leadId: {submitted}")
    print(f"   leadId={lead_id}")

    events_path = "/api/v1/public/lead-events"
    for i in range(2):
        print(f"4.{i + 1} POST {events_path} whatsapp_click")
        recorded = _http(
            "POST",
            events_path,
            body={
                "eventType": "whatsapp_click",
                "projectId": PROJECT_ID,
                "landingPageId": page_id,
                "utm": {"source": "smoke", "medium": "whatsapp", "campaign": f"epic7-m3-{i}"},
                "device": "smoke-test",
            },
        )
        if recorded.get("code") != 200:
            raise RuntimeError(f"lead-events failed: {recorded}")
        event_id = int((recorded.get("data") or {}).get("eventId") or 0)
        if event_id <= 0:
            raise RuntimeError(f"missing eventId: {recorded}")
        print(f"   eventId={event_id}")

    detail_path = f"/api/v1/projects/{PROJECT_ID}/leads/{lead_id}"
    print(f"5. GET {detail_path}")
    detail = _http("GET", detail_path, headers=auth)
    if detail.get("code") != 200:
        raise RuntimeError(f"detail failed: {detail}")
    data = detail.get("data") or {}
    click_count = int(data.get("whatsappClickCount") or 0)
    if click_count < 2:
        raise RuntimeError(f"expected whatsappClickCount >= 2, got {click_count}: {data}")
    if not data.get("lastWhatsappClickAt"):
        raise RuntimeError(f"missing lastWhatsappClickAt: {data}")
    print(f"   whatsappClickCount={click_count} lastWhatsappClickAt={data.get('lastWhatsappClickAt')}")

    ai_path = f"{detail_path}/ai-suggestion"
    print(f"6. POST {ai_path}")
    suggestion = _http("POST", ai_path, body={}, headers=auth)
    if suggestion.get("code") != 200:
        raise RuntimeError(f"ai-suggestion failed: {suggestion}")
    ai_data = suggestion.get("data") or {}
    if not ai_data.get("suggestionEn") and not ai_data.get("suggestionZh"):
        raise RuntimeError(f"missing suggestion text: {suggestion}")
    if ai_data.get("needsHumanReview") is not True:
        raise RuntimeError(f"needsHumanReview must be true: {ai_data}")
    print(
        f"   needsHumanReview={ai_data.get('needsHumanReview')} "
        f"en_len={len(ai_data.get('suggestionEn') or '')} "
        f"zh_len={len(ai_data.get('suggestionZh') or '')}"
    )

    print(f"EPIC-7 M3 WhatsApp + AI smoke passed leadId={lead_id} clicks={click_count}")
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
