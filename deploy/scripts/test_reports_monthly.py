#!/usr/bin/env python3
"""Smoke EPIC-8 M2: report template → monthly create → detail → export DOCX (Java :8080)."""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from datetime import date

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _http, login  # noqa: E402

BASE = os.environ.get("INBOUND_API_BASE", "http://localhost:8080")
PROJECT_ID = int(os.environ.get("REPORT_PROJECT_ID", "1"))
MIN_DOCX_BYTES = int(os.environ.get("REPORT_MIN_DOCX_BYTES", "1024"))


def _export_docx(token: str, report_id: int) -> bytes:
    path = f"/api/v1/projects/{PROJECT_ID}/reports/{report_id}/export?format=docx"
    print(f"5. GET {path}")
    req = urllib.request.Request(
        f"{BASE}{path}",
        headers={
            "Authorization": f"Bearer {token}",
            "clientid": "e5cd7e4891bf95d1d19206ce24a7b32e",
        },
        method="GET",
    )
    try:
        with urllib.request.urlopen(req, timeout=180) as resp:
            data = resp.read()
            ctype = resp.headers.get("Content-Type", "")
    except urllib.error.HTTPError as e:
        body = e.read().decode(errors="replace")
        try:
            err = json.loads(body)
            msg = err.get("msg") or err.get("message") or body[:300]
        except json.JSONDecodeError:
            msg = body[:500]
        raise RuntimeError(f"HTTP {e.code}: {msg}") from e

    if not data.startswith(b"PK"):
        raise RuntimeError(f"unexpected DOCX content-type={ctype} size={len(data)}")
    if len(data) < MIN_DOCX_BYTES:
        raise RuntimeError(f"DOCX too small: {len(data)} bytes (min {MIN_DOCX_BYTES})")
    return data


def _prev_month() -> tuple[int, int]:
    today = date.today()
    if today.month == 1:
        return today.year - 1, 12
    return today.year, today.month - 1


def main() -> int:
    print("1. Login...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    template_body = {
        "logoUrl": "https://cdn.example.com/logo.png",
        "coverTitle": "Inbound Growth Report",
        "companyName": "Dragon Journey Travel",
        "primaryColor": "#059669",
        "footerText": "Confidential · TourGEO Agent",
        "sections": ["geo", "keywords", "content", "landing", "leads", "recommendations"],
    }
    print("2. PUT /api/v1/settings/report-template")
    saved = _http("PUT", "/api/v1/settings/report-template", headers=auth, body=template_body)
    if saved.get("code") != 200:
        raise RuntimeError(f"template save failed: {saved}")
    template_id = (saved.get("data") or {}).get("templateId")
    print(f"   templateId={template_id}")

    print("3. GET /api/v1/settings/report-template")
    got = _http("GET", "/api/v1/settings/report-template", headers=auth)
    if got.get("code") != 200:
        raise RuntimeError(f"template get failed: {got}")
    cfg = got.get("data") or {}
    if cfg.get("companyName") != template_body["companyName"]:
        raise RuntimeError(f"template round-trip mismatch: {cfg}")

    year, month = _prev_month()
    monthly_path = f"/api/v1/projects/{PROJECT_ID}/reports/monthly"
    print(f"4. POST {monthly_path} year={year} month={month}")
    created = _http("POST", monthly_path, headers=auth, body={"year": year, "month": month})
    if created.get("code") != 200:
        raise RuntimeError(f"monthly create failed: {created}")
    report_id = int(created.get("data") or 0)
    if report_id <= 0:
        raise RuntimeError(f"missing reportId: {created}")
    print(f"   reportId={report_id}")

    detail_path = f"/api/v1/projects/{PROJECT_ID}/reports/{report_id}"
    print(f"6. GET {detail_path}")
    detail = _http("GET", detail_path, headers=auth)
    if detail.get("code") != 200:
        raise RuntimeError(f"detail failed: {detail}")
    summary = (detail.get("data") or {}).get("summary") or {}
    recs = summary.get("recommendations") or []
    if len(recs) < 5:
        raise RuntimeError(f"expected 5 recommendations, got {len(recs)}")
    leads = summary.get("leads") or {}
    if "byStatus" not in leads or "wonCount" not in leads:
        raise RuntimeError(f"missing CRM fields in leads: {leads}")
    geo = summary.get("geo") or {}
    if "momDelta" not in geo:
        raise RuntimeError(f"missing geo.momDelta: {geo}")
    print(f"   recommendations={len(recs)} momDelta={geo.get('momDelta')}")

    data = _export_docx(token, report_id)
    out = os.environ.get("REPORT_DOCX_OUT", f"monthly-report-{report_id}.docx")
    with open(out, "wb") as f:
        f.write(data)
    print(f"   saved {out} ({len(data)} bytes)")

    print(f"E2E passed: reportId={report_id} docx={len(data)}B templateId={template_id}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
