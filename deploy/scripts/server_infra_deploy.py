#!/usr/bin/env python3
"""Upload deploy assets and bootstrap MVP infrastructure on remote server via SSH."""
from __future__ import annotations

import argparse
import os
import secrets
import stat
import sys
import textwrap
import time
from pathlib import Path

import paramiko

ROOT = Path(__file__).resolve().parents[2]
DEPLOY = ROOT / "deploy"
REMOTE_ROOT = "/opt/inbound-growth"

UPLOAD_PATHS = [
    DEPLOY / "docker-compose.prod.yml",
    DEPLOY / "init" / "03_langfuse_db.sql",
    ROOT / "database" / "ddl" / "001_schema.sql",
    ROOT / "database" / "ddl" / "002_seed_demo.sql",
]


def gen_secret(n: int = 32) -> str:
    return secrets.token_urlsafe(n)


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
    code = stdout.channel.recv_exit_status()
    return code, out, err


def sftp_upload(client: paramiko.SSHClient, local: Path, remote: str) -> None:
    sftp = client.open_sftp()
    remote_dir = os.path.dirname(remote)
    parts = []
    p = remote_dir
    while p and p != "/":
        parts.append(p)
        p = os.path.dirname(p)
    for d in reversed(parts):
        try:
            sftp.stat(d)
        except OSError:
            sftp.mkdir(d)
    sftp.put(str(local), remote)
    sftp.close()


def build_env_content(existing: str | None) -> str:
    """Preserve existing secrets if .env already on server."""
    vals: dict[str, str] = {}
    if existing:
        for line in existing.splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            vals[k.strip()] = v.strip()

    def get(key: str) -> str:
        return vals.get(key) or gen_secret(24 if key != "NEXTAUTH_SECRET" and key != "SALT" else 32)

    pg = get("POSTGRES_PASSWORD")
    redis = get("REDIS_PASSWORD")
    rq = get("RABBITMQ_DEFAULT_PASS")
    minio_pass = get("MINIO_ROOT_PASSWORD")
    nextauth = vals.get("NEXTAUTH_SECRET") or gen_secret(32)
    salt = vals.get("SALT") or gen_secret(32)

    return f"""# Inbound Growth Agent — server infra (generated, do NOT commit)
POSTGRES_DB=inbound_growth
POSTGRES_USER=inbound
POSTGRES_PASSWORD={pg}
REDIS_PASSWORD={redis}
RABBITMQ_DEFAULT_USER=inbound
RABBITMQ_DEFAULT_PASS={rq}
MINIO_ROOT_USER=inbound_minio
MINIO_ROOT_PASSWORD={minio_pass}
NEXTAUTH_SECRET={nextauth}
SALT={salt}
LANGFUSE_NEXTAUTH_URL=http://127.0.0.1:3100

# App scaffold 联调参考（本机访问）
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/inbound_growth
SPRING_DATASOURCE_USERNAME=inbound
SPRING_DATASOURCE_PASSWORD={pg}
SPRING_REDIS_HOST=127.0.0.1
SPRING_REDIS_PORT=6380
SPRING_REDIS_PASSWORD={redis}
SPRING_RABBITMQ_HOST=127.0.0.1
MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_ACCESS_KEY=inbound_minio
MINIO_SECRET_KEY={minio_pass}
DATABASE_URL=postgresql+asyncpg://inbound:{pg}@127.0.0.1:5432/inbound_growth
RABBITMQ_URL=amqp://inbound:{rq}@127.0.0.1:5672/
LANGFUSE_HOST=http://127.0.0.1:3100
GOTENBERG_URL=http://127.0.0.1:3002
"""


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

    client = connect(args.host, args.user, str(key_path))
    remote_deploy = f"{args.remote_root}/deploy"
    remote_db = f"{args.remote_root}/database/ddl"

    print(f"==> Prepare directories on {args.host}")
    run(client, f"sudo mkdir -p {remote_deploy}/init {remote_db} && sudo chown -R {args.user}:{args.user} {args.remote_root}")

    print("==> Upload files")
    mapping = {
        DEPLOY / "docker-compose.prod.yml": f"{remote_deploy}/docker-compose.prod.yml",
        DEPLOY / "init" / "03_langfuse_db.sql": f"{remote_deploy}/init/03_langfuse_db.sql",
        ROOT / "database" / "ddl" / "001_schema.sql": f"{remote_db}/001_schema.sql",
        ROOT / "database" / "ddl" / "002_seed_demo.sql": f"{remote_db}/002_seed_demo.sql",
    }
    for local, remote in mapping.items():
        if not local.is_file():
            print(f"Missing local file: {local}", file=sys.stderr)
            return 1
        sftp_upload(client, local, remote)
        print(f"  uploaded {local.name} -> {remote}")

    _, existing_env, _ = run(client, f"cat {remote_deploy}/.env 2>/dev/null || true")
    env_content = build_env_content(existing_env if existing_env.strip() else None)

    code, _, err = run(client, f"cat > {remote_deploy}/.env << 'ENVEOF'\n{env_content}ENVEOF")
    if code != 0:
        print("Failed to write .env:", err, file=sys.stderr)
        return 1
    run(client, f"chmod 600 {remote_deploy}/.env")

    print("==> Check docker")
    code, out, err = run(client, "docker --version 2>&1")
    print(out or err)
    if code != 0:
        print("Docker not available", file=sys.stderr)
        return 1

    _, dc_out, _ = run(client, "test -x /usr/local/bin/docker-compose && /usr/local/bin/docker-compose --version || true")
    if not dc_out.strip():
        print("==> Install docker-compose (not found on server)")
        install_cmd = (
            "sudo curl -fsSL "
            "'https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-x86_64' "
            "-o /usr/local/bin/docker-compose && sudo chmod +x /usr/local/bin/docker-compose"
        )
        code, out, err = run(client, install_cmd, timeout=120)
        if code != 0:
            print("Failed to install docker-compose:", err, file=sys.stderr)
            return 1
        _, dc_out, _ = run(client, "/usr/local/bin/docker-compose --version")
        print(dc_out.strip())

    compose_cmd = "/usr/local/bin/docker-compose"
    docker_prefix = "sudo "

    print("==> Pull images & start stack")
    up_cmd = textwrap.dedent(f"""
        cd {remote_deploy} &&
        {docker_prefix}{compose_cmd} -f docker-compose.prod.yml --env-file .env pull &&
        {docker_prefix}{compose_cmd} -f docker-compose.prod.yml --env-file .env up -d
    """).strip()
    code, out, err = run(client, up_cmd, timeout=900)
    print(out)
    if err:
        print(err)
    if code != 0:
        print("docker compose up failed", file=sys.stderr)
        return 1

    print("==> Wait for healthchecks (up to 120s)")
    for i in range(24):
        code, out, _ = run(
            client,
            f"cd {remote_deploy} && {docker_prefix}{compose_cmd} -f docker-compose.prod.yml ps",
            timeout=30,
        )
        if "healthy" in out.lower() or i == 23:
            print(out)
            break
        time.sleep(5)

    print("==> Create MinIO bucket inbound-growth")
    mc_cmd = textwrap.dedent(f"""
        cd {remote_deploy} && set -a && . ./.env && set +a &&
        {docker_prefix}docker run --rm --network inbound-network --entrypoint /bin/sh minio/mc:latest -c "
          mc alias set local http://minio:9000 $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD &&
          mc mb --ignore-existing local/inbound-growth &&
          mc anonymous set none local/inbound-growth &&
          mc ls local/
        "
    """).strip()
    code, out, err = run(client, mc_cmd, timeout=120)
    print(out or err)

    print("==> Verify PostgreSQL tables")
    pg_pass = [l for l in env_content.splitlines() if l.startswith("POSTGRES_PASSWORD=")][0].split("=", 1)[1]
    verify_cmd = f"{docker_prefix}docker exec inbound-postgres psql -U inbound -d inbound_growth -tAc \"SELECT count(*) FROM information_schema.tables WHERE table_schema='public'\""
    code, out, err = run(client, verify_cmd)
    print(f"  public tables: {out.strip()}")

    print("==> Verify Redis")
    redis_pass = [l for l in env_content.splitlines() if l.startswith("REDIS_PASSWORD=")][0].split("=", 1)[1]
    code, out, _ = run(client, f"{docker_prefix}docker exec inbound-redis redis-cli -a '{redis_pass}' ping 2>/dev/null")
    print(f"  redis ping: {out.strip()}")

    print("==> Internal port probe")
    ports = "5432 6380 9000 9001 5672 15672 3100 3002"
    probe = f'for p in {ports}; do timeout 1 bash -c "exec 3<>/dev/tcp/127.0.0.1/$p" 2>/dev/null && echo "127.0.0.1:$p OK" || echo "127.0.0.1:$p FAIL"; done'
    _, out, _ = run(client, probe)
    print(out)

    print("\n" + "=" * 60)
    print("DEPLOY OK")
    print(f"Remote path: {remote_deploy}")
    print(f"Env file:    {remote_deploy}/.env  (chmod 600, contains secrets)")
    print("\nHost ports (127.0.0.1 only):")
    print("  5432  PostgreSQL (inbound_growth + langfuse DB)")
    print("  6380  Redis (requirepass set; NOT 6379 Baota redis)")
    print("  9000  MinIO API | 9001 console")
    print("  5672  RabbitMQ | 15672 management UI")
    print("  3100  Langfuse | 3002 Gotenberg")
    print("\nSSH tunnel example from your PC:")
    print(f"  ssh -i {args.key_file} -L 5432:127.0.0.1:5432 -L 6380:127.0.0.1:6380 {args.user}@{args.host}")
    print("=" * 60)

    client.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
