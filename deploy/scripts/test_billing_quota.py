#!/usr/bin/env python3
"""Smoke EPIC-9 M1: GET subscription + quota 402 on overage (Java :8080)."""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import login  # noqa: E402

BASE = os.environ.get("INBOUND_API_BASE", "http://localhost:8080")
CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e"
PROJECT_ID = int(os.environ.get("BILLING_PROJECT_ID", "1"))
TENANT_ID = int(os.environ.get("BILLING_TENANT_ID", "1"))
PG_DSN = os.environ.get(
    "INBOUND_PG_DSN",
    "postgresql://inbound:inbound_dev_pass@127.0.0.1:5432/inbound_growth",
)
QUOTA_KEY = os.environ.get("BILLING_QUOTA_KEY", "reports_per_month")
QUOTA_EXCEEDED_CODE = 40201


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
        with urllib.request.urlopen(req, timeout=60) as resp:
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


def _load_subscription_row(conn) -> tuple[int, dict, dict]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT id, quota_json, used_json
            FROM subscription
            WHERE tenant_id = %s AND status = 'ACTIVE' AND deleted_at IS NULL
            ORDER BY id DESC
            LIMIT 1
            """,
            (TENANT_ID,),
        )
        row = cur.fetchone()
    if not row:
        raise RuntimeError(f"no ACTIVE subscription for tenant_id={TENANT_ID}")
    sub_id, quota_json, used_json = row
    return int(sub_id), quota_json or {}, used_json or {}


def _set_used_json(conn, sub_id: int, used: dict) -> None:
    with conn.cursor() as cur:
        cur.execute(
            "UPDATE subscription SET used_json = %s::jsonb, updated_at = NOW() WHERE id = %s",
            (json.dumps(used), sub_id),
        )
    conn.commit()


def main() -> int:
    print("1. Login...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    print("2. GET /api/v1/settings/billing")
    status, billing = _request("GET", "/api/v1/settings/billing", headers=auth)
    if status != 200 or billing.get("code") != 200:
        raise RuntimeError(f"billing GET failed: HTTP {status} {billing}")
    data = billing.get("data") or {}
    quotas = data.get("quotas") or []
    if len(quotas) < 6:
        raise RuntimeError(f"expected 6 quota items, got {len(quotas)}")
    print(f"   planCode={data.get('planCode')!r} quotas={len(quotas)}")

    conn = _pg_connect()
    try:
        sub_id, quota_json, original_used = _load_subscription_row(conn)
        limit = int(quota_json.get(QUOTA_KEY, 0))
        if limit <= 0:
            raise RuntimeError(f"quota key {QUOTA_KEY!r} missing or zero in subscription")
        print(f"3. Exhaust quota {QUOTA_KEY} used={limit} limit={limit} (subId={sub_id})")
        exhausted = dict(original_used)
        exhausted[QUOTA_KEY] = limit
        _set_used_json(conn, sub_id, exhausted)

        weekly_path = f"/api/v1/projects/{PROJECT_ID}/reports/weekly"
        end = os.environ.get("BILLING_PERIOD_END")
        body = {}
        if end:
            body["periodEnd"] = end
        print(f"4. POST {weekly_path} (expect HTTP 402 code {QUOTA_EXCEEDED_CODE})")
        http_code, resp = _request("POST", weekly_path, body=body or None, headers=auth)
        if http_code != 402:
            raise RuntimeError(f"expected HTTP 402, got {http_code}: {resp}")
        if resp.get("code") != QUOTA_EXCEEDED_CODE:
            raise RuntimeError(f"expected code {QUOTA_EXCEEDED_CODE}, got {resp}")
        msg = resp.get("msg") or resp.get("message") or ""
        if "额度" not in msg:
            raise RuntimeError(f"unexpected message: {msg!r}")
        print(f"   HTTP {http_code} code={resp.get('code')} msg={msg!r}")

        print("5. Restore subscription.used_json")
        _set_used_json(conn, sub_id, original_used)
    finally:
        conn.close()

    print("E2E passed: billing GET + quota 402 guard")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
