#!/usr/bin/env python3
"""Smoke EPIC-2 M3 FR-109: PUT diagnostic schedule + internal trigger creates run."""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import login  # noqa: E402

BASE = os.environ.get("INBOUND_API_BASE", "http://localhost:8080")
CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e"
PROJECT_ID = int(os.environ.get("DIAGNOSTIC_PROJECT_ID", "1"))
INTERNAL_TOKEN = os.environ.get("AI_SERVICE_INTERNAL_TOKEN", "dev_internal_token_change_me")
PG_DSN = os.environ.get(
    "INBOUND_PG_DSN",
    "postgresql://inbound:inbound_dev_pass@127.0.0.1:5432/inbound_growth",
)


def _request(
    method: str,
    path: str,
    body: dict | None = None,
    headers: dict | None = None,
) -> tuple[int, dict]:
    url = f"{BASE}{path}"
    hdrs = {"Content-Type": "application/json", "clientid": CLIENT_ID}
    if headers:
        hdrs.update(headers)
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, headers=hdrs, method=method)
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            raw = resp.read().decode()
            return resp.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read().decode(errors="replace")
        try:
            payload = json.loads(raw) if raw else {}
        except json.JSONDecodeError:
            payload = {"msg": raw}
        return e.code, payload


def _pg_connect():
    try:
        import psycopg2
    except ImportError:
        print("pip install psycopg2-binary", file=sys.stderr)
        raise
    return psycopg2.connect(PG_DSN)


def _ensure_ddl(conn) -> None:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = 'diagnostic_schedule'
            """
        )
        if cur.fetchone():
            return
    ddl_path = os.path.join(
        os.path.dirname(__file__), "..", "..", "database", "ddl", "001_schema.sql"
    )
    raise RuntimeError(
        f"diagnostic_schedule table missing; apply DDL from {os.path.abspath(ddl_path)}"
    )


def main() -> int:
    print("1. Login...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    schedule_body = {
        "enabled": True,
        "frequency": "WEEKLY",
        "market": "US",
        "locale": "en-US",
        "region": "us-east",
        "probeModes": ["grounded-api"],
        "models": ["gemini"],
        "sampleCount": 1,
        "calibrationRatio": 0,
        "questionScope": "all",
    }
    put_path = f"/api/v1/projects/{PROJECT_ID}/diagnostics/schedule"
    print(f"2. PUT {put_path}")
    status, put_resp = _request("PUT", put_path, body=schedule_body, headers=auth)
    if status != 200 or put_resp.get("code") != 200:
        raise RuntimeError(f"schedule PUT failed: HTTP {status} {put_resp}")
    put_data = put_resp.get("data") or {}
    if not put_data.get("enabled"):
        raise RuntimeError(f"expected enabled=true after PUT: {put_data}")
    print(f"   configured={put_data.get('configured')} nextRunAt={put_data.get('nextRunAt')!r}")

    print(f"3. GET {put_path}")
    status, get_resp = _request("GET", put_path, headers=auth)
    if status != 200 or get_resp.get("code") != 200:
        raise RuntimeError(f"schedule GET failed: HTTP {status} {get_resp}")
    get_data = get_resp.get("data") or {}
    if get_data.get("frequency") != "WEEKLY":
        raise RuntimeError(f"unexpected frequency: {get_data}")
    print("   OK")

    trigger_path = (
        f"/api/v1/internal/diagnostics/schedule-trigger?"
        f"{urllib.parse.urlencode({'projectId': PROJECT_ID, 'force': 'true'})}"
    )
    trigger_headers = {"Authorization": f"Bearer {INTERNAL_TOKEN}"}
    print(f"4. POST {trigger_path}")
    status, trigger_resp = _request("POST", trigger_path, headers=trigger_headers)
    if status != 200 or trigger_resp.get("code") != 200:
        raise RuntimeError(f"schedule trigger failed: HTTP {status} {trigger_resp}")
    run_id = trigger_resp.get("data")
    if not run_id:
        raise RuntimeError(f"expected runId from trigger, got: {trigger_resp}")
    print(f"   runId={run_id}")

    print(f"5. GET /api/v1/diagnostics/{run_id}")
    status, run_resp = _request("GET", f"/api/v1/diagnostics/{run_id}", headers=auth)
    if status != 200 or run_resp.get("code") != 200:
        raise RuntimeError(f"run GET failed: HTTP {status} {run_resp}")
    run_data = run_resp.get("data") or {}
    if run_data.get("id") != run_id:
        raise RuntimeError(f"run id mismatch: {run_data}")
    print(f"   status={run_data.get('status')!r} name={run_data.get('name')!r}")

    print(f"6. GET {put_path} — verify lastRunId")
    status, after_resp = _request("GET", put_path, headers=auth)
    after_data = after_resp.get("data") or {}
    if after_data.get("lastRunId") != run_id:
        raise RuntimeError(f"lastRunId not updated: {after_data}")
    if not after_data.get("lastTriggeredAt"):
        raise RuntimeError(f"lastTriggeredAt missing: {after_data}")
    if not after_data.get("nextRunAt"):
        raise RuntimeError(f"nextRunAt not advanced: {after_data}")
    print(f"   lastRunId={after_data.get('lastRunId')} nextRunAt={after_data.get('nextRunAt')!r}")

    conn = _pg_connect()
    try:
        _ensure_ddl(conn)
    finally:
        conn.close()

    print("E2E passed: diagnostic schedule PUT + internal trigger + run created")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
