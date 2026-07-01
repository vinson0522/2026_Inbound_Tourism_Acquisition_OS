#!/usr/bin/env python3
"""Smoke EPIC-8 M1: report list → weekly create → export DOCX (Java :8080)."""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from datetime import date, timedelta

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _http, login  # noqa: E402

BASE = os.environ.get("INBOUND_API_BASE", "http://localhost:8080")
PROJECT_ID = int(os.environ.get("REPORT_PROJECT_ID", "1"))
MIN_DOCX_BYTES = int(os.environ.get("REPORT_MIN_DOCX_BYTES", "1024"))


def _export_docx(token: str, report_id: int) -> bytes:
    path = f"/api/v1/projects/{PROJECT_ID}/reports/{report_id}/export?format=docx"
    print(f"4. GET {path}")
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


def main() -> int:
    print("1. Login...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    list_path = f"/api/v1/projects/{PROJECT_ID}/reports?pageNum=1&pageSize=10"
    print(f"2. GET {list_path}")
    listed = _http("GET", list_path, headers=auth)
    if listed.get("code") != 200:
        raise RuntimeError(f"list failed: {listed}")
    before_total = int(listed.get("total") or 0)
    print(f"   total={before_total}")

    end = date.today()
    start = end - timedelta(days=6)
    weekly_path = f"/api/v1/projects/{PROJECT_ID}/reports/weekly"
    print(f"3. POST {weekly_path}")
    created = _http(
        "POST",
        weekly_path,
        headers=auth,
        body={"periodStart": start.isoformat(), "periodEnd": end.isoformat()},
    )
    if created.get("code") != 200:
        raise RuntimeError(f"weekly create failed: {created}")
    report_id = int(created.get("data") or 0)
    if report_id <= 0:
        raise RuntimeError(f"missing reportId: {created}")
    print(f"   reportId={report_id}")

    data = _export_docx(token, report_id)
    out = os.environ.get("REPORT_DOCX_OUT", f"weekly-report-{report_id}.docx")
    with open(out, "wb") as f:
        f.write(data)
    print(f"   saved {out} ({len(data)} bytes)")

    detail_path = f"/api/v1/projects/{PROJECT_ID}/reports/{report_id}"
    print(f"5. GET {detail_path}")
    detail = _http("GET", detail_path, headers=auth)
    if detail.get("code") != 200:
        raise RuntimeError(f"detail failed: {detail}")
    summary = (detail.get("data") or {}).get("summary") or {}
    recs = summary.get("recommendations") or []
    if len(recs) < 3:
        raise RuntimeError(f"expected 3 recommendations, got {len(recs)}")
    print(f"   recommendations={len(recs)}")

    listed2 = _http("GET", list_path, headers=auth)
    after_total = int(listed2.get("total") or 0)
    if after_total < before_total + 1:
        raise RuntimeError(f"list total did not increase: before={before_total} after={after_total}")

    print(f"E2E passed: reportId={report_id} docx={len(data)}B")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
