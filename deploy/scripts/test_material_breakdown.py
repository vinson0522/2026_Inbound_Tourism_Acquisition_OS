#!/usr/bin/env python3
"""Smoke EPIC-5 M1: material upload + breakdown trigger + internal callback (Java :8080)."""
from __future__ import annotations

import json
import os
import sys
import tempfile
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from test_projects_api import login  # noqa: E402

BASE = os.environ.get("INBOUND_API_BASE", "http://localhost:8080")
CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e"
PROJECT_ID = int(os.environ.get("MATERIAL_PROJECT_ID", "1"))
INTERNAL_TOKEN = os.environ.get("AI_SERVICE_INTERNAL_TOKEN", "dev_internal_token_change_me")

MOCK_FRAMES = [
    {
        "timestamp": 0,
        "timestampLabel": "0:00",
        "thumbnailUrl": "https://placehold.co/120x68/png?text=1",
        "caption": "Opening skyline hook",
    },
    {
        "timestamp": 5,
        "timestampLabel": "0:05",
        "thumbnailUrl": "https://placehold.co/120x68/png?text=2",
        "caption": "Traveler POV street scene",
    },
]

MOCK_DIMENSIONS = {
    "theme": "First-time China inbound trust building",
    "hook": "Skyline contrast in first 3 seconds",
    "shot": "Fast B-roll cuts with handheld POV",
    "subtitle": "Bold English keywords on screen",
    "emotion": "Curiosity to aspiration",
    "psychology": "Social proof and risk reduction",
    "reusable": "Problem-evidence-CTA three-act",
}


def _multipart_upload(path: str, fields: dict, file_field: str, file_path: str, filename: str, content_type: str, auth: dict) -> tuple[int, dict]:
    boundary = "----InboundMaterialBoundary7MA4YWxk"
    body_parts: list[bytes] = []

    for key, value in fields.items():
        body_parts.append(f"--{boundary}\r\n".encode())
        body_parts.append(f'Content-Disposition: form-data; name="{key}"\r\n\r\n'.encode())
        body_parts.append(f"{value}\r\n".encode())

    with open(file_path, "rb") as fh:
        file_data = fh.read()
    body_parts.append(f"--{boundary}\r\n".encode())
    body_parts.append(
        f'Content-Disposition: form-data; name="{file_field}"; filename="{filename}"\r\n'.encode()
    )
    body_parts.append(f"Content-Type: {content_type}\r\n\r\n".encode())
    body_parts.append(file_data)
    body_parts.append(b"\r\n")
    body_parts.append(f"--{boundary}--\r\n".encode())
    body = b"".join(body_parts)

    headers = {
        "Content-Type": f"multipart/form-data; boundary={boundary}",
        "clientid": CLIENT_ID,
    }
    headers.update(auth)
    req = urllib.request.Request(path, data=body, headers=headers, method="POST")
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


def _request(method: str, path: str, body: dict | None = None, headers: dict | None = None) -> tuple[int, dict]:
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


def main() -> int:
    print("1. Login...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as tmp:
        tmp.write(b"\x00\x00\x00\x18ftypmp42\x00\x00\x00\x00mp42isom")
        tmp_path = tmp.name

    upload_path = f"{BASE}/api/v1/projects/{PROJECT_ID}/materials"
    print(f"2. POST multipart {upload_path}")
    status, upload_resp = _multipart_upload(
        upload_path,
        {"type": "VIDEO", "copyrightStatus": "external", "source": "smoke-test"},
        "file",
        tmp_path,
        "smoke-viral.mp4",
        "video/mp4",
        auth,
    )
    os.unlink(tmp_path)
    if status not in (200, 201) or upload_resp.get("code") != 200:
        raise RuntimeError(f"upload failed: HTTP {status} {upload_resp}")
    material_id = int(upload_resp["data"])
    print(f"   materialId={material_id}")

    list_path = f"/api/v1/projects/{PROJECT_ID}/materials?pageNum=1&pageSize=10"
    print(f"3. GET {list_path}")
    status, listed = _request("GET", list_path, headers=auth)
    if status != 200 or listed.get("code") != 200:
        raise RuntimeError(f"list failed: HTTP {status} {listed}")
    rows = listed.get("rows") or []
    match = next((r for r in rows if int(r.get("id", 0)) == material_id), None)
    if not match:
        raise RuntimeError(f"material {material_id} not found in list")
    print(f"   breakdownStatus={match.get('breakdownStatus')!r}")

    breakdown_path = f"/api/v1/projects/{PROJECT_ID}/materials/{material_id}/breakdown"
    print(f"4. POST {breakdown_path} (expect 202)")
    status, trigger = _request("POST", breakdown_path, headers=auth)
    if status != 200 and status != 202:
        raise RuntimeError(f"breakdown trigger failed: HTTP {status} {trigger}")
    if trigger.get("code") != 200:
        raise RuntimeError(f"breakdown trigger failed: {trigger}")
    breakdown_id = int((trigger.get("data") or {}).get("breakdownId"))
    print(f"   breakdownId={breakdown_id}")

    callback_path = "/api/v1/internal/materials/breakdown-callback"
    callback_body = {
        "breakdownId": breakdown_id,
        "status": "SUCCESS",
        "frames": MOCK_FRAMES,
        "dimensions": MOCK_DIMENSIONS,
        "reusableStructure": "Open with visual hook, establish credibility, close with soft CTA.",
        "needsHumanReview": True,
    }
    print(f"5. POST {callback_path} (mock worker callback)")
    status, callback_resp = _request(
        "POST",
        callback_path,
        body=callback_body,
        headers={"Authorization": f"Bearer {INTERNAL_TOKEN}"},
    )
    if status != 200 or callback_resp.get("code") != 200:
        raise RuntimeError(f"callback failed: HTTP {status} {callback_resp}")

    detail_path = f"/api/v1/projects/{PROJECT_ID}/breakdowns/{breakdown_id}"
    print(f"6. GET {detail_path}")
    status, detail = _request("GET", detail_path, headers=auth)
    if status != 200 or detail.get("code") != 200:
        raise RuntimeError(f"detail failed: HTTP {status} {detail}")
    data = detail.get("data") or {}
    if data.get("breakdownStatus") != "SUCCESS":
        raise RuntimeError(f"expected SUCCESS, got {data.get('breakdownStatus')!r}")
    frames = data.get("frames") or []
    dimensions = data.get("dimensions") or {}
    if len(frames) < 2:
        raise RuntimeError(f"expected >=2 frames, got {len(frames)}")
    if "theme" not in dimensions:
        raise RuntimeError(f"dimensions missing theme: {dimensions}")
    if not data.get("needsHumanReview"):
        raise RuntimeError("expected needsHumanReview=true")
    print(f"   frames={len(frames)} theme={dimensions.get('theme')!r}")

    print("E2E passed: material upload + breakdown callback")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as e:
        print(e, file=sys.stderr)
        raise SystemExit(1)
