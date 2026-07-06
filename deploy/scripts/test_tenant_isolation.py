#!/usr/bin/env python3
"""FR-807 tenant isolation smoke — tenant A must not read tenant B tourgeo resources.

Requires C25 DDL + seed tenant B (database/ddl/006_fr807_tenant_mapping.sql):
  - tenant B customer_project (default id=8 via TENANT_B_PROJECT_ID)
  - tenant B diagnostic_run id=100 (TENANT_B_RUN_ID)
  - tenant B lead id=100 (leads list uses project scope)
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import BASE, CLIENT_ID, login  # noqa: E402

FORBIDDEN_CODES = {403, 40300}


def api_get(path: str, token: str) -> tuple[int, dict]:
    """Return HTTP status and parsed JSON body."""
    req = urllib.request.Request(
        f"{BASE}{path}",
        headers={"clientid": CLIENT_ID, "Authorization": f"Bearer {token}"},
        method="GET",
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.status, json.loads(resp.read().decode())


def resolve_tenant_b_ids(verbose: bool) -> tuple[int, int]:
    """Tenant B fixture ids from seed (006_fr807 / 002_seed_demo)."""
    project_id = int(os.environ.get("TENANT_B_PROJECT_ID", "8"))
    run_id = int(os.environ.get("TENANT_B_RUN_ID", "100"))
    if verbose:
        print(
            f"0. tenant B fixtures projectId={project_id} runId={run_id} "
            f"(006_fr807 seed · override via TENANT_B_* env)"
        )
    return project_id, run_id


def assert_forbidden(label: str, path: str, token: str, verbose: bool) -> None:
    http_status, payload = api_get(path, token)
    code = payload.get("code")
    msg = payload.get("msg") or payload.get("message")
    if verbose:
        print(f"   HTTP {http_status} api.code={code} msg={msg}")
    if code in FORBIDDEN_CODES:
        return
    raise RuntimeError(f"{label}: expected api code 403, got {payload}")


def main() -> int:
    parser = argparse.ArgumentParser(description="FR-807 cross-tenant isolation smoke")
    parser.add_argument("--verbose", action="store_true", help="Print api.code per request")
    args = parser.parse_args()

    project_id, run_id = resolve_tenant_b_ids(args.verbose)

    print("1. Login as tenant A (admin / 000000 → business tenant 1)")
    token_a = login()
    if args.verbose:
        print("   OK")

    cases = [
        ("projects detail", f"/api/v1/projects/{project_id}"),
        ("diagnostics list", f"/api/v1/projects/{project_id}/diagnostics?pageNum=1&pageSize=10"),
        ("diagnostic run detail", f"/api/v1/diagnostics/{run_id}"),
        ("leads list", f"/api/v1/projects/{project_id}/leads?pageNum=1&pageSize=10"),
    ]

    for idx, (label, path) in enumerate(cases, start=2):
        print(f"{idx}. GET {path} ({label}, tenant B resource)")
        assert_forbidden(label, path, token_a, args.verbose)

    print(f"FR-807 tenant isolation smoke passed ({len(cases)} endpoints)")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        print(body, file=sys.stderr)
        raise SystemExit(1)
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
