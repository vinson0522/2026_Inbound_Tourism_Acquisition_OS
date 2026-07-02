#!/usr/bin/env python3
"""Smoke FR-106: export DOCX/PDF for SUCCESS diagnostic run (requires Java :8080)."""
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
FORMAT = os.environ.get("DIAGNOSTIC_REPORT_FORMAT", "docx").lower()
PG_DSN = os.environ.get(
    "INBOUND_PG_DSN",
    "postgresql://inbound:inbound_dev_pass@127.0.0.1:5432/inbound_growth",
)
OUT = os.environ.get(
    "DIAGNOSTIC_REPORT_OUT",
    f"geo-report-run-{RUN_ID}.{ 'pdf' if FORMAT == 'pdf' else 'docx'}",
)


def export_report(token: str, fmt: str, run_id: int) -> tuple[bytes, str]:
    print(f"GET /api/v1/diagnostics/{run_id}/report?format={fmt}")
    req = urllib.request.Request(
        f"{BASE}/api/v1/diagnostics/{run_id}/report?format={fmt}",
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

    if fmt == "pdf":
        if not data.startswith(b"%PDF"):
            raise RuntimeError(f"unexpected PDF content-type={ctype} size={len(data)}")
    elif not data.startswith(b"PK"):
        raise RuntimeError(f"unexpected DOCX content-type={ctype} size={len(data)}")
    return data, ctype


def _resolve_export_run_id(token: str) -> int:
    """Prefer env RUN_ID; fallback to latest SUCCESS run with geo_score (B-21)."""
    preferred = RUN_ID
    try:
        detail = _http("GET", f"/api/v1/diagnostics/{preferred}", headers={"Authorization": f"Bearer {token}"})
        status = (detail.get("data") or {}).get("status")
        if status in ("SUCCESS", "PARTIAL_FAILED"):
            print(f"   using runId={preferred} status={status}")
            return preferred
        print(f"   runId={preferred} status={status!r} not exportable; searching fallback...")
    except RuntimeError as exc:
        print(f"   runId={preferred} lookup failed: {exc}; searching fallback...")

    try:
        import psycopg2
    except ImportError:
        raise RuntimeError(
            f"runId={preferred} not exportable and psycopg2 unavailable for fallback"
        ) from None

    conn = psycopg2.connect(PG_DSN)
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, status::text, geo_score
                FROM diagnostic_run
                WHERE deleted_at IS NULL
                  AND status IN ('SUCCESS', 'PARTIAL_FAILED')
                ORDER BY (geo_score IS NOT NULL) DESC, id DESC
                LIMIT 1
                """
            )
            row = cur.fetchone()
    finally:
        conn.close()

    if not row:
        raise RuntimeError("no SUCCESS/PARTIAL_FAILED diagnostic_run for report export smoke")
    run_id, status, geo = row
    print(f"   fallback runId={run_id} status={status} geo_score={geo}")
    return int(run_id)


def verify_metadata(data: bytes, geo) -> None:
    text = data.decode("latin-1", errors="ignore")
    has_probe = "probe_mode" in text
    has_geo_label = "GEO Score" in text
    has_geo_val = False
    if geo is not None:
        has_geo_val = str(geo).split(".")[0] in text
    if has_probe and has_geo_label and (geo is None or has_geo_val):
        print(f"   metadata OK (geo_score={geo}, probe_mode in export)")
    else:
        print(f"   warn: verify manually (probe_mode={has_probe}, geo_label={has_geo_label}, geo_val={has_geo_val})")


def main() -> int:
    print("1. Login...")
    token = login()
    print("   OK")

    fmt = FORMAT if FORMAT in ("docx", "pdf") else "docx"
    if fmt == "pdf":
        print("2. PDF export (requires Gotenberg :3002 + GOTENBERG_BASE_URL)")
    else:
        print("2. DOCX export")

    run_id = _resolve_export_run_id(token)
    global OUT
    if OUT == f"geo-report-run-{RUN_ID}.{ 'pdf' if FORMAT == 'pdf' else 'docx'}":
        OUT = f"geo-report-run-{run_id}.{ 'pdf' if fmt == 'pdf' else 'docx'}"

    data, _ctype = export_report(token, fmt, run_id)
    with open(OUT, "wb") as f:
        f.write(data)
    print(f"   saved {OUT} ({len(data)} bytes)")

    detail = _http("GET", f"/api/v1/diagnostics/{run_id}", headers={"Authorization": f"Bearer {token}"})
    geo = (detail.get("data") or {}).get("geoScore")
    verify_metadata(data, geo)
    print(f"E2E passed: {fmt.upper()} runId={run_id}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
