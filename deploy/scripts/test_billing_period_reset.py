#!/usr/bin/env python3
"""Smoke EPIC-9 M2: PUT subscription + internal period reset clears monthly used_json."""
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
TENANT_ID = int(os.environ.get("BILLING_TENANT_ID", "1"))
INTERNAL_TOKEN = os.environ.get("AI_SERVICE_INTERNAL_TOKEN", "dev_internal_token_change_me")
PG_DSN = os.environ.get(
    "INBOUND_PG_DSN",
    "postgresql://inbound:inbound_dev_pass@127.0.0.1:5432/inbound_growth",
)

MONTHLY_KEYS = (
    "diagnostics_per_month",
    "keywords_per_month",
    "content_per_month",
    "landing_pages_per_month",
    "reports_per_month",
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


def _quota_map(data: dict) -> dict[str, dict]:
    quotas = data.get("quotas") or []
    return {item["key"]: item for item in quotas if item.get("key")}


def _pg_connect():
    try:
        import psycopg2
    except ImportError:
        print("pip install psycopg2-binary", file=sys.stderr)
        raise
    return psycopg2.connect(PG_DSN)


def _load_subscription_row(conn) -> tuple[int, dict, dict, str, str]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT id, quota_json, used_json, period_start::text, period_end::text
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
    sub_id, quota_json, used_json, period_start, period_end = row
    return int(sub_id), quota_json or {}, used_json or {}, period_start, period_end


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
    before = billing.get("data") or {}
    before_map = _quota_map(before)
    print(f"   planCode={before.get('planCode')!r}")

    conn = _pg_connect()
    try:
        sub_id, quota_json, original_used, period_start, period_end = _load_subscription_row(conn)
        new_diag_limit = int(quota_json.get("diagnostics_per_month", 4)) + 2
        put_body = {
            "planCode": before.get("planCode") or "growth_service",
            "quotaJson": dict(quota_json),
            "periodStart": period_start,
            "periodEnd": period_end,
        }
        put_body["quotaJson"]["diagnostics_per_month"] = new_diag_limit

        print(f"3. PUT /api/v1/settings/billing/subscription diagnostics limit -> {new_diag_limit}")
        status, put_resp = _request(
            "PUT",
            "/api/v1/settings/billing/subscription",
            body=put_body,
            headers=auth,
        )
        if status != 200 or put_resp.get("code") != 200:
            raise RuntimeError(f"billing PUT failed: HTTP {status} {put_resp}")
        put_data = put_resp.get("data") or {}
        put_map = _quota_map(put_data)
        if put_map.get("diagnostics_per_month", {}).get("limit") != new_diag_limit:
            raise RuntimeError(f"PUT did not apply new limit: {put_map}")
        print("   OK")

        projects_used = int(before_map.get("projects", {}).get("used") or original_used.get("projects") or 0)
        simulated_used = dict(original_used)
        simulated_used["diagnostics_per_month"] = 3
        simulated_used["reports_per_month"] = 2
        if projects_used > 0:
            simulated_used["projects"] = projects_used
        print(f"4. Simulate consumption used_json={simulated_used}")
        _set_used_json(conn, sub_id, simulated_used)

        status, consumed = _request("GET", "/api/v1/settings/billing", headers=auth)
        consumed_map = _quota_map(consumed.get("data") or {})
        if consumed_map.get("diagnostics_per_month", {}).get("used") != 3:
            raise RuntimeError(f"expected diagnostics used=3, got {consumed_map}")
        print("   diagnostics used=3 OK")

        reset_path = f"/api/v1/internal/billing/period-reset?{urllib.parse.urlencode({'tenantId': TENANT_ID})}"
        reset_headers = {"Authorization": f"Bearer {INTERNAL_TOKEN}"}
        print(f"5. POST {reset_path}")
        status, reset_resp = _request("POST", reset_path, headers=reset_headers)
        if status != 200 or reset_resp.get("code") != 200:
            raise RuntimeError(f"period reset failed: HTTP {status} {reset_resp}")

        after_map = _quota_map(reset_resp.get("data") or {})
        for key in MONTHLY_KEYS:
            used = after_map.get(key, {}).get("used")
            if used != 0:
                raise RuntimeError(f"expected {key} used=0 after reset, got {used}")
        projects_after = after_map.get("projects", {}).get("used")
        if projects_after != projects_used:
            raise RuntimeError(
                f"projects used should be preserved ({projects_used}), got {projects_after}"
            )
        print(f"   monthly keys reset to 0 · projects used preserved={projects_after}")

        print("6. Restore subscription.used_json")
        _set_used_json(conn, sub_id, original_used)
    finally:
        conn.close()

    print("E2E passed: billing PUT + period reset")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
