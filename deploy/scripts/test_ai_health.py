#!/usr/bin/env python3
"""Smoke test inbound-ai health endpoints."""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request

BASE = os.environ.get("INBOUND_AI_BASE", "http://localhost:8090").rstrip("/")
TOKEN = os.environ.get("AI_SERVICE_INTERNAL_TOKEN", "dev_internal_token_change_me")


def _get(path: str, headers: dict | None = None) -> dict:
    req = urllib.request.Request(f"{BASE}{path}", headers=headers or {}, method="GET")
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode())


def main() -> int:
    print(f"1. GET {BASE}/health")
    pub = _get("/health")
    assert pub.get("status") == "ok", pub
    assert pub.get("service") == "inbound-ai", pub
    print(f"   OK version={pub.get('version')}")

    print(f"2. GET {BASE}/ai/health (Bearer token)")
    ai = _get("/ai/health", headers={"Authorization": f"Bearer {TOKEN}"})
    assert ai.get("code") == 0, ai
    assert ai.get("data", {}).get("status") == "ok", ai
    print(f"   OK litellm={ai['data'].get('litellm')} trace_id={ai.get('trace_id')}")

    print("All AI health checks passed.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except urllib.error.HTTPError as e:
        print(e.read().decode(), file=sys.stderr)
        raise SystemExit(1)
    except urllib.error.URLError as e:
        print(f"Connection failed: {e}", file=sys.stderr)
        raise SystemExit(1)
