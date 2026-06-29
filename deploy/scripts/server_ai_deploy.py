#!/usr/bin/env python3
"""Upload inbound-ai sources and start ai-api on remote server."""
from __future__ import annotations

import argparse
import os
import secrets
import sys
import time
from pathlib import Path

import paramiko

ROOT = Path(__file__).resolve().parents[2]
DEPLOY = ROOT / "deploy"
AI_ROOT = ROOT / "inbound-ai"
REMOTE_ROOT = "/opt/inbound-growth"
AI_UPLOAD_FILES = ("Dockerfile", "pyproject.toml", "README.md")


def connect(host: str, user: str, key_file: str) -> paramiko.SSHClient:
    key = paramiko.RSAKey.from_private_key_file(key_file)
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(host, username=user, pkey=key, timeout=20, allow_agent=False, look_for_keys=False)
    return client


def run(client: paramiko.SSHClient, cmd: str, timeout: int = 900) -> tuple[int, str, str]:
    _, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    return stdout.channel.recv_exit_status(), out, err


def sftp_mkdir_p(sftp: paramiko.SFTPClient, remote_dir: str) -> None:
    parts: list[str] = []
    p = remote_dir
    while p and p != "/":
        parts.append(p)
        p = os.path.dirname(p)
    for d in reversed(parts):
        try:
            sftp.stat(d)
        except OSError:
            sftp.mkdir(d)


def sftp_upload_file(sftp: paramiko.SFTPClient, local: Path, remote: str) -> None:
    sftp_mkdir_p(sftp, os.path.dirname(remote))
    sftp.put(str(local), remote)


def sftp_upload_tree(sftp: paramiko.SFTPClient, local_dir: Path, remote_dir: str) -> None:
    for path in sorted(local_dir.rglob("*")):
        rel = path.relative_to(local_dir)
        remote = f"{remote_dir}/{rel.as_posix()}"
        if path.is_dir():
            sftp_mkdir_p(sftp, remote)
        else:
            sftp_upload_file(sftp, path, remote)


def ensure_ai_env(existing: str) -> str:
    lines = existing.splitlines()
    keys = {}
    for line in lines:
        s = line.strip()
        if not s or s.startswith("#") or "=" not in s:
            continue
        k, v = s.split("=", 1)
        keys[k.strip()] = v.strip()

    if not keys.get("AI_SERVICE_INTERNAL_TOKEN"):
        token = secrets.token_urlsafe(32)
        lines.append(f"AI_SERVICE_INTERNAL_TOKEN={token}")
        print(f"==> Added AI_SERVICE_INTERNAL_TOKEN to server .env")

    for key in (
        "LANGFUSE_PUBLIC_KEY",
        "LANGFUSE_SECRET_KEY",
        "OPENAI_API_KEY",
        "GEMINI_API_KEY",
        "PERPLEXITY_API_KEY",
    ):
        if key not in keys:
            lines.append(f"{key}=")

    return "\n".join(lines).rstrip() + "\n"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", required=True)
    ap.add_argument("--user", default="ec2-user")
    ap.add_argument("--key-file", default="cert/im1.pem")
    ap.add_argument("--remote-root", default=REMOTE_ROOT)
    args = ap.parse_args()

    key_path = ROOT / args.key_file
    if not key_path.is_file():
        print(f"Key not found: {key_path}", file=sys.stderr)
        return 1

    for name in AI_UPLOAD_FILES:
        if not (AI_ROOT / name).is_file():
            print(f"Missing {AI_ROOT / name}", file=sys.stderr)
            return 1
    if not (AI_ROOT / "app").is_dir():
        print("Missing inbound-ai/app", file=sys.stderr)
        return 1

    client = connect(args.host, args.user, str(key_path))
    remote_deploy = f"{args.remote_root}/deploy"
    remote_ai = f"{args.remote_root}/inbound-ai"

    print("==> Upload compose + inbound-ai sources")
    sftp = client.open_sftp()
    sftp_upload_file(sftp, DEPLOY / "docker-compose.prod.yml", f"{remote_deploy}/docker-compose.prod.yml")
    for name in AI_UPLOAD_FILES:
        sftp_upload_file(sftp, AI_ROOT / name, f"{remote_ai}/{name}")
    sftp_upload_tree(sftp, AI_ROOT / "app", f"{remote_ai}/app")
    sftp.close()
    print(f"  uploaded inbound-ai -> {remote_ai}")

    _, existing_env, _ = run(client, f"cat {remote_deploy}/.env 2>/dev/null || true")
    env_content = ensure_ai_env(existing_env)
    code, _, err = run(client, f"cat > {remote_deploy}/.env << 'ENVEOF'\n{env_content}ENVEOF")
    if code != 0:
        print("Failed to write .env:", err, file=sys.stderr)
        return 1
    run(client, f"chmod 600 {remote_deploy}/.env")

    compose_cmd = "/usr/local/bin/docker-compose"
    print("==> Build and start ai-api")
    up_cmd = (
        f"cd {remote_deploy} && sudo {compose_cmd} -f docker-compose.prod.yml --env-file .env "
        f"up -d --build ai-api"
    )
    code, out, err = run(client, up_cmd, timeout=1200)
    print(out)
    if err:
        print(err, file=sys.stderr)
    if code != 0:
        print("ai-api deploy failed", file=sys.stderr)
        _, logs, _ = run(client, f"sudo docker logs inbound-ai --tail 80 2>&1")
        print(logs)
        return 1

    print("==> Verify /health")
    http_code = ""
    body = ""
    for attempt in range(12):
        code, out, _ = run(client, "curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8090/health")
        http_code = out.strip()
        if http_code == "200":
            _, body, _ = run(client, "curl -s http://127.0.0.1:8090/health")
            break
        time.sleep(5)
    print(f"  HTTP {http_code}")
    if http_code != "200":
        _, body, _ = run(client, "curl -s http://127.0.0.1:8090/health || true")
        print(body)
        _, logs, _ = run(client, "sudo docker logs inbound-ai --tail 80 2>&1")
        print(logs)
        return 1

    print(f"  body: {body.strip()}")

    print("==> Verify EPIC-2 routes (openapi)")
    _, openapi_raw, _ = run(client, "curl -s http://127.0.0.1:8090/openapi.json")
    try:
        import json

        paths = sorted(json.loads(openapi_raw).get("paths", {}).keys())
        required = {"/ai/diagnose", "/ai/parse-citations", "/ai/score", "/ai/health"}
        missing = required - set(paths)
        print(f"  routes: {', '.join(paths)}")
        if missing:
            print(f"  MISSING EPIC-2 routes: {missing}", file=sys.stderr)
            return 1
        print("  EPIC-2 routes OK")
    except Exception as exc:
        print(f"  openapi parse failed: {exc}", file=sys.stderr)
        return 1

    _, ps_out, _ = run(client, f"cd {remote_deploy} && sudo {compose_cmd} -f docker-compose.prod.yml ps ai-api")
    print(ps_out)

    client.close()
    print("\nAI-API DEPLOY OK — http://127.0.0.1:8090/health → 200")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
