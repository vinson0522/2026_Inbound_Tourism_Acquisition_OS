#!/usr/bin/env python3
"""Merge LLM keys from environment into remote deploy/.env and restart ai-api.

Usage (keys never stored in repo):
  set GEMINI_API_KEY=...
  set OPENAI_API_KEY=...
  set OPENAI_API_BASE=https://...
  python deploy/scripts/configure_llm_keys.py --host 18.139.209.10 --key-file cert/im1.pem
"""
from __future__ import annotations

import argparse
import os
import sys
import time
from pathlib import Path

import paramiko

ROOT = Path(__file__).resolve().parents[2]

ENV_KEYS = (
    "GEMINI_API_KEY",
    "OPENAI_API_KEY",
    "OPENAI_API_BASE",
    "PERPLEXITY_API_KEY",
    "LANGFUSE_PUBLIC_KEY",
    "LANGFUSE_SECRET_KEY",
)


def connect(host: str, user: str, key_file: str) -> paramiko.SSHClient:
    key = paramiko.RSAKey.from_private_key_file(key_file)
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(host, username=user, pkey=key, timeout=20, allow_agent=False, look_for_keys=False)
    return client


def run(client: paramiko.SSHClient, cmd: str, timeout: int = 600) -> tuple[int, str, str]:
    _, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    return stdout.channel.recv_exit_status(), out, err


def merge_env(existing: str, updates: dict[str, str]) -> str:
    lines = existing.splitlines()
    present = set()
    out: list[str] = []
    for line in lines:
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            out.append(line)
            continue
        key, _ = stripped.split("=", 1)
        key = key.strip()
        if key in updates:
            out.append(f"{key}={updates[key]}")
            present.add(key)
        else:
            out.append(line)
    for key, value in updates.items():
        if key not in present:
            out.append(f"{key}={value}")
    return "\n".join(out).rstrip() + "\n"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", required=True)
    ap.add_argument("--user", default="ec2-user")
    ap.add_argument("--key-file", default="cert/im1.pem")
    ap.add_argument("--remote-deploy", default="/opt/inbound-growth/deploy")
    args = ap.parse_args()

    updates = {k: os.environ[k].strip() for k in ENV_KEYS if os.environ.get(k, "").strip()}
    if not updates:
        print("No LLM keys in environment. Set GEMINI_API_KEY / OPENAI_API_KEY etc.", file=sys.stderr)
        return 1

    key_path = ROOT / args.key_file
    client = connect(args.host, args.user, str(key_path))

    _, existing, _ = run(client, f"cat {args.remote_deploy}/.env 2>/dev/null || true")
    merged = merge_env(existing, updates)

    code, _, err = run(client, f"cat > {args.remote_deploy}/.env << 'ENVEOF'\n{merged}ENVEOF")
    if code != 0:
        print("Failed to write .env:", err, file=sys.stderr)
        return 1
    run(client, f"chmod 600 {args.remote_deploy}/.env")

    print("Updated keys:", ", ".join(updates.keys()))

    compose = "/usr/local/bin/docker-compose"
    code, out, err = run(
        client,
        f"cd {args.remote_deploy} && sudo {compose} -f docker-compose.prod.yml --env-file .env up -d ai-api",
    )
    print(out or err)
    if code != 0:
        return 1

    for attempt in range(12):
        _, code_out, _ = run(client, "curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8090/health")
        if code_out.strip() == "200":
            _, body, _ = run(client, "curl -s http://127.0.0.1:8090/ai/health -H 'Authorization: Bearer $(grep AI_SERVICE_INTERNAL_TOKEN /opt/inbound-growth/deploy/.env | cut -d= -f2)'")
            print("ai-api health OK")
            if body.strip():
                print("ai/health:", body.strip()[:200])
            client.close()
            return 0
        time.sleep(5)

    print("ai-api did not become healthy in time", file=sys.stderr)
    client.close()
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
