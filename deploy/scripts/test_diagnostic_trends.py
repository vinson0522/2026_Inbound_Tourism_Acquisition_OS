#!/usr/bin/env python3
"""Smoke test FR-108 diagnostic trends API (login + GET trends, expect >=2 SUCCESS runs)."""
from __future__ import annotations

import os
import sys
import urllib.error

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import _http, login  # noqa: E402

PROJECT_ID = int(os.environ.get("DIAGNOSTIC_PROJECT_ID", "1"))


def main() -> int:
    print("1. Login...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    path = f"/api/v1/projects/{PROJECT_ID}/diagnostics/trends?limit=12"
    print(f"2. GET {path}")
    resp = _http("GET", path, headers=auth)
    if resp.get("code") != 200:
        raise RuntimeError(f"trends failed: {resp}")

    data = resp.get("data") or {}
    runs = data.get("runs") or []
    print(f"   runs={len(runs)}")
    if len(runs) < 2:
        raise RuntimeError(f"expected >=2 trend points, got {len(runs)}")

    prev_finished = None
    for i, run in enumerate(runs):
        for key in ("runId", "name", "geoScore", "finishedAt", "metrics"):
            if key not in run:
                raise RuntimeError(f"run[{i}] missing {key}: {run}")
        metrics = run["metrics"]
        for mk in (
            "brandMentionRate",
            "top3Rate",
            "competitorSuppression",
            "citationCoverage",
            "longtailCoverage",
            "assetCompleteness",
        ):
            if mk not in metrics:
                raise RuntimeError(f"run[{i}] metrics missing {mk}: {metrics}")
        finished = run["finishedAt"]
        if prev_finished is not None and finished < prev_finished:
            raise RuntimeError(f"runs not sorted ASC by finishedAt: {prev_finished} > {finished}")
        prev_finished = finished
        print(
            f"   [{i}] runId={run['runId']} geoScore={run['geoScore']} "
            f"finishedAt={run['finishedAt']} brandMentionRate={metrics['brandMentionRate']}"
        )

    print(f"FR-108 trends smoke passed: {len(runs)} points")

    wide_path = f"{path}&from=2020-01-01&to=2099-12-31"
    print(f"3. GET {wide_path} (date range filter)")
    wide = _http("GET", wide_path, headers=auth)
    if wide.get("code") != 200:
        raise RuntimeError(f"trends with from/to failed: {wide}")
    wide_runs = (wide.get("data") or {}).get("runs") or []
    print(f"   runs={len(wide_runs)} (from/to accepted)")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except urllib.error.HTTPError as e:
        print(e.read().decode(), file=sys.stderr)
        raise SystemExit(1)
