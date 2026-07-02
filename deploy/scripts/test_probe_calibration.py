#!/usr/bin/env python3
"""Smoke EPIC-11 M2: platform-adapters Admin API + calibration_ratio fork + calibration GET."""
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
INTERNAL_TOKEN = os.environ.get("AI_SERVICE_INTERNAL_TOKEN", "dev_internal_token_change_me")
POLL_SEC = int(os.environ.get("PROBE_POLL_SEC", "90"))


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


def _internal_callback(probe_task_id: int, result: dict) -> None:
    body = {"probeTaskId": probe_task_id, "status": "SUCCESS", "result": result}
    req = urllib.request.Request(
        f"{BASE}/api/v1/internal/diagnostics/probe-callback",
        data=json.dumps(body).encode(),
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {INTERNAL_TOKEN}",
        },
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        payload = json.loads(resp.read().decode())
    if payload.get("code") != 200:
        raise RuntimeError(f"internal callback failed: {payload}")


def main() -> int:
    token = login()
    auth = {"Authorization": f"Bearer {token}"}

    print("1. GET /api/v1/settings/platform-adapters")
    listed = _http("GET", "/api/v1/settings/platform-adapters", headers=auth)
    if listed.get("code") != 200:
        raise RuntimeError(f"list adapters failed: {listed}")
    rows = listed.get("data") or []
    platforms = {row.get("platform") for row in rows}
    print(f"   adapters={len(rows)} platforms={sorted(platforms)}")
    if "perplexity" not in platforms:
        raise RuntimeError("missing perplexity adapter — run 002_seed_demo.sql")
    if "chatgpt" not in platforms:
        raise RuntimeError("missing chatgpt adapter — run updated 002_seed_demo.sql")

    print("2. GET/PUT /api/v1/settings/platform-adapters/perplexity")
    detail = _http("GET", f"/api/v1/settings/platform-adapters/{PLATFORM}", headers=auth)
    if detail.get("code") != 200:
        raise RuntimeError(f"get adapter failed: {detail}")
    adapter = detail["data"]
    save_body = {
        "version": adapter.get("version") or "1.0",
        "enabled": True,
        "domSelectorsJson": adapter.get("domSelectorsJson") or {"input": "textarea"},
        "apiPatternsJson": adapter.get("apiPatternsJson") or {"chatApi": "/api/chat"},
        "parseRulesJson": adapter.get("parseRulesJson") or {"citationsPath": "citations"},
    }
    saved = _http("PUT", f"/api/v1/settings/platform-adapters/{PLATFORM}", body=save_body, headers=auth)
    if saved.get("code") != 200:
        raise RuntimeError(f"save adapter failed: {saved}")
    print("   PUT OK")

    print("3. Register probe node")
    reg = _probe_request(
        "POST",
        "/api/v1/probe/nodes/register",
        {
            "nodeKey": NODE_KEY,
            "region": "us-east",
            "platforms": [PLATFORM],
            "extensionVersion": "0.1.0-calibration-smoke",
        },
    )
    if reg.get("code") != 200:
        raise RuntimeError(f"register failed: {reg}")

    print("4. Create dual-mode diagnostic with calibration_ratio=1.0")
    body = {
        "name": "Calibration Smoke M2",
        "market": "US",
        "locale": "en-US",
        "region": "us-east",
        "probeModes": ["grounded-api", "browser-extension"],
        "models": [PLATFORM],
        "sampleCount": 2,
        "calibrationRatio": 1.0,
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

    print("5. Verify probe task fork (1 grounded + 1 extension, sampleCount forced to 1)")
    tasks = _http("GET", f"/api/v1/diagnostics/{run_id}/probe-tasks", headers=auth)
    task_rows = tasks.get("data") or []
    grounded = [t for t in task_rows if t.get("probeMode") == "grounded-api"]
    extension = [t for t in task_rows if t.get("probeMode") == "browser-extension"]
    if len(grounded) != 1 or len(extension) != 1:
        raise RuntimeError(f"unexpected task fork: grounded={len(grounded)} extension={len(extension)}")
    print(f"   groundedTaskId={grounded[0]['id']} extensionTaskId={extension[0]['id']}")

    print("6. Mock grounded-api + browser-extension results")
    brand = "Dragon Journey Travel"
    _internal_callback(
        grounded[0]["id"],
        {
            "probe_mode": "grounded-api",
            "platform": PLATFORM,
            "answer_text": f"{brand} offers curated first-time China private tours with expert guides.",
            "mentioned_brands": [brand],
            "rank": 2,
            "citations": [{"url": "https://example.com/a", "title": "A", "domain": "example.com", "rank": 1}],
        },
    )

    polled = _probe_request("GET", f"/api/v1/probe/tasks/poll?platform={PLATFORM}")
    task = polled.get("data")
    if not task or task.get("probeTaskId") != extension[0]["id"]:
        raise RuntimeError(f"poll mismatch: {polled}")

    _probe_request(
        "POST",
        f"/api/v1/probe/tasks/{extension[0]['id']}/result",
        {
            "status": "SUCCESS",
            "result": {
                "probe_mode": "browser-extension",
                "platform": PLATFORM,
                "answer_text": "Several tour operators provide private China tours for visitors.",
                "mentioned_brands": [],
                "rank": 4,
                "citations": [{"url": "https://example.com/b", "title": "B", "domain": "example.com", "rank": 1}],
            },
        },
    )
    print("   callbacks OK")

    print(f"7. Wait for run completion (max {POLL_SEC}s)")
    deadline = time.time() + POLL_SEC
    while time.time() < deadline:
        detail_run = _http("GET", f"/api/v1/diagnostics/{run_id}", headers=auth)
        status = (detail_run.get("data") or {}).get("status")
        if status in ("SUCCESS", "PARTIAL_FAILED", "FAILED"):
            break
        time.sleep(2)

    print("8. GET /api/v1/projects/{projectId}/diagnostics/{runId}/calibration")
    cal = _http("GET", f"/api/v1/projects/{PROJECT_ID}/diagnostics/{run_id}/calibration", headers=auth)
    if cal.get("code") != 200:
        raise RuntimeError(f"calibration GET failed: {cal}")
    data = cal.get("data") or {}
    paired = data.get("pairedCount", 0)
    pairs = data.get("pairs") or []
    if paired < 1 or not pairs:
        raise RuntimeError(f"expected calibration pairs, got: {data}")
    if data.get("deviationRate") is None or data.get("brandMentionAgreementRate") is None:
        raise RuntimeError(f"missing KPI fields: {data}")
    if pairs[0].get("groundedApi") is None or pairs[0].get("browserExtension") is None:
        raise RuntimeError(f"missing side payloads: {pairs[0]}")
    print(
        f"   pairedCount={paired} deviationRate={data.get('deviationRate')} "
        f"brandMentionAgreementRate={data.get('brandMentionAgreementRate')}"
    )

    print(f"M2 calibration smoke passed: runId={run_id} pairedCount={paired}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
