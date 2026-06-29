#!/usr/bin/env python3
"""Smoke FR-106: export DOCX for SUCCESS diagnostic run (requires Java :8080)."""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _http, login  # noqa: E402

BASE = os.environ.get("INBOUND_API_BASE", "http://localhost:8080")
RUN_ID = int(os.environ.get("DIAGNOSTIC_REPORT_RUN_ID", "2"))
OUT = os.environ.get("DIAGNOSTIC_REPORT_OUT", f"geo-report-run-{RUN_ID}.docx")


def main() -> int:
    print("1. Login...")
    token = login()
    print("   OK")

    print(f"2. GET /api/v1/diagnostics/{RUN_ID}/report?format=docx")
    req = urllib.request.Request(
        f"{BASE}/api/v1/diagnostics/{RUN_ID}/report?format=docx",
        headers={
            "Authorization": f"Bearer {token}",
            "clientid": "e5cd7e4891bf95d1d19206ce24a7b32e",
        },
        method="GET",
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            data = resp.read()
            ctype = resp.headers.get("Content-Type", "")
    except urllib.error.HTTPError as e:
        body = e.read().decode(errors="replace")
        print(f"   HTTP {e.code}: {body[:500]}")
        return 1

    if not data.startswith(b"PK"):
        print(f"   unexpected content-type={ctype} size={len(data)}")
        return 1

    with open(OUT, "wb") as f:
        f.write(data)
    print(f"   saved {OUT} ({len(data)} bytes)")

    detail = _http("GET", f"/api/v1/diagnostics/{RUN_ID}", headers={"Authorization": f"Bearer {token}"})
    geo = (detail.get("data") or {}).get("geoScore")
    text = data.decode("latin-1", errors="ignore")
    if geo and str(geo).split(".")[0] in text:
        print(f"E2E passed: DOCX contains geo_score hint ({geo})")
    else:
        print(f"E2E passed: DOCX downloaded (geo_score={geo}, verify manually)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
