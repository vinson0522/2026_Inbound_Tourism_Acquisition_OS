#!/usr/bin/env python3
"""Smoke test FR-001 projects API (login + list + create). Local Docker Redis :6379; tunnel :6380."""
from __future__ import annotations

import base64
import json
import os
import secrets
import string
import sys
import urllib.error
import urllib.request

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import padding as sym_padding
from cryptography.hazmat.primitives.asymmetric import padding as asym_padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.serialization import load_der_public_key

BASE = os.environ.get("INBOUND_API_BASE", "http://localhost:8080")
CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e"
RSA_PUBLIC_B64 = (
    "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKoR8mX0rGKLqzcWmOzbfj64K8ZIgOdHnzkXSOVOZbFu/TJhZ7rFAN+eaGkl3C4buccQd/EjEsj9ir7ijT7h96MCAwEAAQ=="
)
REDIS_HOST = os.environ.get("INBOUND_REDIS_HOST", "127.0.0.1")
REDIS_PORT = int(os.environ.get("INBOUND_REDIS_PORT", "6379"))
REDIS_PASSWORD = os.environ.get("INBOUND_REDIS_PASSWORD", "")


def _http(method: str, path: str, body: dict | None = None, headers: dict | None = None) -> dict:
    url = f"{BASE}{path}"
    data = None
    hdrs = {"Content-Type": "application/json", "clientid": CLIENT_ID}
    if headers:
        hdrs.update(headers)
    if body is not None:
        data = json.dumps(body).encode()
    req = urllib.request.Request(url, data=data, headers=hdrs, method=method)
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode())


def _encrypt_post(payload: dict) -> tuple[str, dict]:
    aes_key = "".join(secrets.choice(string.ascii_letters + string.digits) for _ in range(32)).encode()
    padder = sym_padding.PKCS7(128).padder()
    plain = json.dumps(payload).encode()
    padded = padder.update(plain) + padder.finalize()
    cipher = Cipher(algorithms.AES(aes_key), modes.ECB(), backend=default_backend())
    encryptor = cipher.encryptor()
    encrypted_body = base64.b64encode(encryptor.update(padded) + encryptor.finalize()).decode()

    pub = load_der_public_key(base64.b64decode(RSA_PUBLIC_B64), backend=default_backend())
    enc_key = pub.encrypt(base64.b64encode(aes_key), asym_padding.PKCS1v15())
    return encrypted_body, {"encrypt-key": base64.b64encode(enc_key).decode()}


def _redis_captcha(uuid: str) -> str:
    try:
        import redis
    except ImportError:
        print("pip install redis", file=sys.stderr)
        raise
    r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, password=REDIS_PASSWORD or None, decode_responses=True)
    key = f"global:captcha_codes:{uuid}"
    code = r.get(key)
    if not code:
        raise RuntimeError(f"captcha not in redis: {key}")
    if code.startswith('"') and code.endswith('"'):
        code = json.loads(code)
    return code


def login() -> str:
    code_resp = _http("GET", "/auth/code")
    uuid = code_resp["data"]["uuid"]
    captcha = _redis_captcha(uuid)
    payload = {
        "tenantId": "000000",
        "username": "admin",
        "password": "admin123",
        "code": captcha,
        "uuid": uuid,
        "clientId": CLIENT_ID,
        "grantType": "password",
    }
    enc_body, extra_hdrs = _encrypt_post(payload)
    req = urllib.request.Request(
        f"{BASE}/auth/login",
        data=enc_body.encode(),
        headers={"Content-Type": "application/json", "clientid": CLIENT_ID, **extra_hdrs},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = json.loads(resp.read().decode())
    if data.get("code") != 200:
        raise RuntimeError(f"login failed: {data}")
    return data["data"]["access_token"]


def main() -> int:
    print("1. Login...")
    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    print("   OK")

    print("2. GET /api/v1/projects/options")
    opts = _http("GET", "/api/v1/projects/options", headers=auth)
    print(f"   count={len(opts.get('data') or [])}")

    print("3. GET /api/v1/projects?pageNum=1&pageSize=10")
    lst = _http("GET", "/api/v1/projects?pageNum=1&pageSize=10", headers=auth)
    rows = lst.get("rows") or []
    print(f"   total={lst.get('total')}, rows={len(rows)}")

    print("4. POST /api/v1/projects (encrypted)")
    create_payload = {
        "name": "API Smoke Test Project",
        "brandName": "SmokeBrand",
        "website": "https://example.com",
        "industry": "inbound_tourism",
        "targetMarkets": ["US", "UK"],
        "languages": ["en"],
    }
    enc_body, extra_hdrs = _encrypt_post(create_payload)
    req = urllib.request.Request(
        f"{BASE}/api/v1/projects",
        data=enc_body.encode(),
        headers={"Content-Type": "application/json", "clientid": CLIENT_ID, "Authorization": f"Bearer {token}", **extra_hdrs},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        created = json.loads(resp.read().decode())
    project_id = created.get("data")
    print(f"   id={project_id}")

    print("5. GET /api/v1/projects/{id}")
    detail = _http("GET", f"/api/v1/projects/{project_id}", headers=auth)
    print(f"   name={detail['data']['name']}")

    print("All smoke checks passed.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except urllib.error.HTTPError as e:
        print(e.read().decode(), file=sys.stderr)
        raise SystemExit(1)
