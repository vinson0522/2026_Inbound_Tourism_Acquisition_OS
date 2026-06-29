#!/usr/bin/env python3
"""E2E: create GEO diagnostic → poll until SUCCESS → verify geo_score (EPIC-2 M1).

Requires:
  - Java :8080, inbound-ai :8090 with worker enabled, RabbitMQ tunnel :5672
  - PG tunnel :15432, Redis :6380
  - GEMINI_API_KEY in inbound-ai env (ADR-08 default probe)
"""
from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.request

# Reuse login helpers from projects smoke test
sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _encrypt_post, _http, login  # noqa: E402

BASE = os.environ.get("INBOUND_API_BASE", "http://localhost:8080")
CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e"
PROJECT_ID = int(os.environ.get("DIAGNOSTIC_PROJECT_ID", "1"))
MODELS = os.environ.get("DIAGNOSTIC_MODELS", "Gemini").split(",")
POLL_SEC = int(os.environ.get("DIAGNOSTIC_POLL_SEC", "180"))
POLL_INTERVAL = float(os.environ.get("DIAGNOSTIC_POLL_INTERVAL", "5"))


def main() -> int:
    print("1. Login...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    print(f"2. POST /api/v1/projects/{PROJECT_ID}/diagnostics (Gemini grounded-api, sample=1)")
    body = {
        "name": "E2E Gemini Diagnostic",
        "market": "US",
        "locale": "en-US",
        "region": "us-east",
        "probeModes": ["grounded-api"],
        "models": [m.strip() for m in MODELS if m.strip()],
        "sampleCount": 1,
        "calibrationRatio": 0,
        "questionScope": "all",
    }
    enc_body, extra_hdrs = _encrypt_post(body)
    req = urllib.request.Request(
        f"{BASE}/api/v1/projects/{PROJECT_ID}/diagnostics",
        data=enc_body.encode(),
        headers={
            "Content-Type": "application/json",
            "clientid": CLIENT_ID,
            "Authorization": f"Bearer {token}",
            **extra_hdrs,
        },
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        created = json.loads(resp.read().decode())
    if created.get("code") != 200:
        raise RuntimeError(f"create failed: {created}")
    run_id = created["data"]
    print(f"   runId={run_id}")

    print(f"3. Poll GET /api/v1/diagnostics/{run_id} (max {POLL_SEC}s)...")
    deadline = time.time() + POLL_SEC
    final = None
    while time.time() < deadline:
        detail = _http("GET", f"/api/v1/diagnostics/{run_id}", headers=auth)
        data = detail.get("data") or {}
        status = data.get("status")
        progress = data.get("progress")
        geo = data.get("geoScore")
        print(f"   status={status} progress={progress}% geo_score={geo}")
        if status in ("SUCCESS", "PARTIAL_FAILED", "FAILED"):
            final = data
            break
        time.sleep(POLL_INTERVAL)

    if not final:
        raise RuntimeError("timeout waiting for terminal status")
    if final["status"] not in ("SUCCESS", "PARTIAL_FAILED"):
        raise RuntimeError(f"unexpected terminal status: {final['status']}")

    print("4. GET /api/v1/diagnostics/{runId}/results")
    results = _http("GET", f"/api/v1/diagnostics/{run_id}/results", headers=auth)
    rows = results.get("data") or []
    print(f"   results={len(rows)}")
    if not rows:
        raise RuntimeError("no diagnostic results")

    if final.get("geoScore") is None:
        raise RuntimeError("geo_score is null on SUCCESS/PARTIAL_FAILED run")

    print(f"E2E passed: status={final['status']} geo_score={final['geoScore']} results={len(rows)}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except urllib.error.HTTPError as e:
        print(e.read().decode(), file=sys.stderr)
        raise SystemExit(1)
