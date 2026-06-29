#!/usr/bin/env python3
"""API walkthrough for Admin UI flows (login / projects / GEO diagnostics).

Simulates what the browser loads via Vite proxy /dev-api -> :8080.
"""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _encrypt_post, _http, login  # noqa: E402

BASE = os.environ.get("INBOUND_API_BASE", "http://localhost:8080")
PROXY = os.environ.get("VITE_PROXY_BASE", "http://localhost:80/dev-api")


def via_proxy(path: str, token: str) -> dict:
    req = urllib.request.Request(
        f"{PROXY}{path}",
        headers={
            "Authorization": f"Bearer {token}",
            "clientid": "e5cd7e4891bf95d1d19206ce24a7b32e",
        },
        method="GET",
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode())


def main() -> int:
    print("=== Admin UI 走查（API + Vite 代理）===\n")

    print("1. 登录 (POST /auth/login -> token)")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK\n")

    print("2. 项目列表 GET /api/v1/projects (浏览器: /projects)")
    projects = _http("GET", "/api/v1/projects?pageNum=1&pageSize=10", headers=auth)
    total = projects.get("total", 0)
    rows = projects.get("rows") or []
    print(f"   total={total}, first={rows[0].get('name') if rows else 'none'}")
    project_id = rows[0]["id"] if rows else 1
    print()

    print("3. Vite 代理 GET /dev-api/api/v1/projects (pnpm dev :80)")
    try:
        proxied = via_proxy("/api/v1/projects?pageNum=1&pageSize=10", token)
        print(f"   proxy OK total={proxied.get('total')}")
    except urllib.error.URLError as e:
        print(f"   proxy SKIP ({e})")
    print()

    print("4. GEO 诊断列表 GET /api/v1/projects/{id}/diagnostics")
    diag_list = _http(
        "GET",
        f"/api/v1/projects/{project_id}/diagnostics?pageNum=1&pageSize=10",
        headers=auth,
    )
    diag_rows = diag_list.get("rows") or []
    print(f"   projectId={project_id} runs={len(diag_rows)} total={diag_list.get('total')}")
    if diag_rows:
        run = diag_rows[0]
        run_id = run.get("id")
        print(f"   latest runId={run_id} status={run.get('status')} geoScore={run.get('geoScore')}")
    else:
        run_id = None
        print("   (no runs yet)")
    print()

    if run_id:
        print(f"5. 诊断详情 GET /api/v1/diagnostics/{run_id} (浏览器: /diagnostics/runs/{run_id})")
        detail = _http("GET", f"/api/v1/diagnostics/{run_id}", headers=auth)
        data = detail.get("data") or detail
        if isinstance(data, dict) and "data" in data:
            data = data["data"]
        d = data.get("data") if isinstance(data, dict) and "data" in data else data
        if not isinstance(d, dict):
            d = detail.get("data") or {}
        print(f"   status={d.get('status')} progress={d.get('progress')}% geoScore={d.get('geoScore')}")

        print(f"6. 诊断结果 GET /api/v1/diagnostics/{run_id}/results")
        results = _http("GET", f"/api/v1/diagnostics/{run_id}/results", headers=auth)
        rrows = results.get("data") or results.get("rows") or []
        print(f"   results={len(rrows)}")
    else:
        print("5–6. 跳过详情（无诊断 run）")

    print("\n=== 走查完成 ===")
    print("浏览器: http://localhost:80  账号 admin / admin123")
    print("  登录 -> 客户项目 /projects -> GEO诊断 /diagnostics/runs")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
