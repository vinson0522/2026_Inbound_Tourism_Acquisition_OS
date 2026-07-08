#!/usr/bin/env python3
"""Smoke A1: marketing-portal contact form public endpoint (Java :8080).

Verifies POST /api/v1/public/marketing-contact:
  - happy path returns code=200 + leadId
  - missing name is rejected
  - missing email AND phone is rejected

No auth, no Turnstile (inbound.turnstile.enabled=false in dev). Does not consume quota.
"""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request

BASE = os.environ.get("INBOUND_API_BASE", "http://localhost:8080")
CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e"
PATH = "/api/v1/public/marketing-contact"


def _post(body: dict) -> dict:
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        f"{BASE}{PATH}",
        data=data,
        headers={"Content-Type": "application/json", "clientid": CLIENT_ID},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode())


def main() -> int:
    print("1. POST happy path (name + email + company)")
    ok = _post(
        {
            "name": "Jane Ops",
            "email": "jane@acme-smoke.com",
            "company": "Acme Tours",
            "message": "Interested in GEO diagnostics",
            "source": "marketing",
        }
    )
    if ok.get("code") != 200 or not ok.get("data", {}).get("leadId"):
        print(f"   FAIL: {ok}")
        return 1
    print(f"   OK leadId={ok['data']['leadId']}")

    print("2. POST missing name (expect rejection)")
    no_name = _post({"email": "x@y.com"})
    if no_name.get("code") == 200:
        print(f"   FAIL: expected rejection, got {no_name}")
        return 1
    print(f"   OK rejected: {no_name.get('msg')}")

    print("3. POST missing email AND phone (expect rejection)")
    no_contact = _post({"name": "NoContact"})
    if no_contact.get("code") == 200:
        print(f"   FAIL: expected rejection, got {no_contact}")
        return 1
    print(f"   OK rejected: {no_contact.get('msg')}")

    print("A1 marketing-contact smoke passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
