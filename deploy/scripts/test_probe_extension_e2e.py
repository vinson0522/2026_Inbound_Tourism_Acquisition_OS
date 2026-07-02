#!/usr/bin/env python3
"""Smoke EPIC-11 M1: probe register → browser-extension run → poll → mock result (Java :8080)."""
from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _encrypt_post, _http, login  # noqa: E402

BASE = os.environ.get("INBOUND_API_BASE", "http://localhost:8080")
CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e"
PROJECT_ID = int(os.environ.get("PROBE_PROJECT_ID", "1"))
NODE_KEY = os.environ.get("PROBE_NODE_KEY", "demo-probe-1")
PLATFORM = os.environ.get("PROBE_PLATFORM", "perplexity")
QUESTION_ID = int(os.environ.get("PROBE_QUESTION_ID", "1"))
POLL_SEC = int(os.environ.get("PROBE_POLL_SEC", "60"))


def _probe_request(
    method: str,
    path: str,
    body: dict | None = None,
    node_key: str = NODE_KEY,
) -> dict:
    url = f"{BASE}{path}"
    hdrs = {"Content-Type": "application/json", "clientid": CLIENT_ID, "X-Probe-Node-Key": node_key}
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, headers=hdrs, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            raw = resp.read().decode()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read().decode(errors="replace")
        try:
            payload = json.loads(raw) if raw else {}
        except json.JSONDecodeError:
            payload = {"msg": raw}
        raise RuntimeError(f"HTTP {e.code}: {payload}") from e


def main() -> int:
    print("1. POST /api/v1/probe/nodes/register")
    reg = _probe_request(
        "POST",
        "/api/v1/probe/nodes/register",
        {
            "nodeKey": NODE_KEY,
            "region": "us-east",
            "platforms": [PLATFORM],
            "extensionVersion": "0.1.0-smoke",
        },
    )
    if reg.get("code") != 200:
        raise RuntimeError(f"register failed: {reg}")
    print(f"   nodeId={reg.get('data')}")

    print("2. GET /api/v1/probe/adapters")
    adapters = _probe_request("GET", "/api/v1/probe/adapters")
    rows = adapters.get("data") or []
    if not rows:
        raise RuntimeError("no platform adapters — run 002_seed_demo.sql platform_adapter insert")
    print(f"   adapters={len(rows)}")

    print("3. Login + create browser-extension diagnostic (1 question)")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    body = {
        "name": "E2E Extension Probe Smoke",
        "market": "US",
        "locale": "en-US",
        "region": "us-east",
        "probeModes": ["browser-extension"],
        "models": ["perplexity"],
        "sampleCount": 1,
        "questionIds": [QUESTION_ID],
    }
    enc_body, extra_hdrs = _encrypt_post(body)
    req = urllib.request.Request(
        f"{BASE}/api/v1/projects/{PROJECT_ID}/diagnostics",
        data=enc_body.encode(),
        headers={"Content-Type": "application/json", "clientid": CLIENT_ID, "Authorization": f"Bearer {token}", **extra_hdrs},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        created = json.loads(resp.read().decode())
    if created.get("code") != 200:
        raise RuntimeError(f"create diagnostic failed: {created}")
    run_id = created["data"]
    print(f"   runId={run_id}")

    print(f"4. GET /api/v1/probe/tasks/poll?platform={PLATFORM}")
    polled = _probe_request("GET", f"/api/v1/probe/tasks/poll?platform={PLATFORM}")
    task = polled.get("data")
    if not task or not task.get("probeTaskId"):
        raise RuntimeError(f"no pending probe task: {polled}")
    task_id = task["probeTaskId"]
    print(f"   probeTaskId={task_id} question={task.get('question', '')[:60]!r}...")

    print(f"5. POST /api/v1/probe/tasks/{task_id}/result")
    result_body = {
        "status": "SUCCESS",
        "result": {
            "probe_mode": "browser-extension",
            "platform": PLATFORM,
            "answer_text": "Dragon Journey Travel offers curated first-time China private tours.",
            "citations": [
                {
                    "url": "https://example.com/china-tour",
                    "title": "China Private Tours",
                    "domain": "example.com",
                    "rank": 1,
                }
            ],
            "mentioned_brands": ["Dragon Journey Travel"],
            "raw_response_json": {},
        },
    }
    submitted = _probe_request("POST", f"/api/v1/probe/tasks/{task_id}/result", result_body)
    if submitted.get("code") != 200:
        raise RuntimeError(f"submit result failed: {submitted}")
    print("   OK")

    print(f"6. Poll GET /api/v1/diagnostics/{run_id} (max {POLL_SEC}s)")
    deadline = time.time() + POLL_SEC
    final = None
    while time.time() < deadline:
        detail = _http("GET", f"/api/v1/diagnostics/{run_id}", headers=auth)
        data = detail.get("data") or {}
        status = data.get("status")
        progress = data.get("progress")
        print(f"   status={status} progress={progress}%")
        if status in ("SUCCESS", "PARTIAL_FAILED", "FAILED"):
            final = data
            break
        time.sleep(2)

    if not final or final.get("status") != "SUCCESS":
        raise RuntimeError(f"run did not succeed: {final}")

    results = _http("GET", f"/api/v1/diagnostics/{run_id}/results", headers=auth)
    rows = results.get("data") or []
    if not rows:
        raise RuntimeError("no diagnostic results after extension callback")
    if rows[0].get("probeMode") != "browser-extension":
        raise RuntimeError(f"unexpected probeMode: {rows[0].get('probeMode')}")

    print(f"E2E passed: runId={run_id} status=SUCCESS results={len(rows)} probeMode=browser-extension")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
