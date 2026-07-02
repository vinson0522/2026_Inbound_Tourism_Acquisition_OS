#!/usr/bin/env python3
"""FR-807 smoke: tenant A (admin) cannot GET tenant B project → api code 403."""
from __future__ import annotations

import json
import os
import sys
import urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import login  # noqa: E402

BASE = os.environ.get("INBOUND_API_BASE", "http://localhost:8080")
CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e"
TENANT_B_PROJECT_ID = int(os.environ.get("TENANT_B_PROJECT_ID", "8"))


def main() -> int:
    print("1. Login as tenant A (admin / 000000 → business tenant 1)")
    token = login()
    project_id = TENANT_B_PROJECT_ID
    print(f"2. GET /api/v1/projects/{project_id} (tenant B resource, tenant_id=2)")
    req = urllib.request.Request(
        f"{BASE}/api/v1/projects/{project_id}",
        headers={"clientid": CLIENT_ID, "Authorization": f"Bearer {token}"},
        method="GET",
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        payload = json.loads(resp.read().decode())
    code = payload.get("code")
    print(f"   HTTP {resp.status} api.code={code} msg={payload.get('msg')}")
    if code in (403, 40300):
        print("FR-807 tenant isolation smoke passed")
        return 0
    raise RuntimeError(f"expected api code 403, got {payload}")


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
