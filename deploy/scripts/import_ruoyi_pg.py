#!/usr/bin/env python3
"""Import RuoYi PostgreSQL system tables into inbound_growth on remote server."""
import argparse
import sys
from pathlib import Path

import paramiko

ROOT = Path(__file__).resolve().parents[2]
SQL_FILES = [
    ROOT / "inbound-core/script/sql/postgres/postgres_ry_vue_5.X.sql",
    ROOT / "inbound-core/script/sql/postgres/postgres_ry_workflow.sql",
]


def run(client: paramiko.SSHClient, cmd: str, timeout: int = 300) -> tuple[int, str]:
    _, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = (stdout.read() + stderr.read()).decode("utf-8", errors="replace").strip()
    code = stdout.channel.recv_exit_status()
    return code, out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="18.139.209.10")
    ap.add_argument("--user", default="ec2-user")
    ap.add_argument("--key-file", default=str(ROOT / "cert/im1.pem"))
    ap.add_argument("--check-only", action="store_true")
    args = ap.parse_args()

    key = paramiko.RSAKey.from_private_key_file(args.key_file)
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(args.host, username=args.user, pkey=key, timeout=15, allow_agent=False, look_for_keys=False)

    check_sql = "SELECT to_regclass('public.sys_user'), count(*) FROM pg_tables WHERE schemaname='public';"
    code, out = run(client, f"sudo docker exec inbound-postgres psql -U inbound -d inbound_growth -tAc \"{check_sql}\"")
    print(f"[before] {out}")
    if args.check_only:
        client.close()
        return code

    sftp = client.open_sftp()
    remote_dir = "/tmp/ruoyi_pg_import"
    run(client, f"mkdir -p {remote_dir}")

    for sql_path in SQL_FILES:
        if not sql_path.is_file():
            print(f"SKIP missing: {sql_path}", file=sys.stderr)
            continue
        host_path = f"{remote_dir}/{sql_path.name}"
        container_path = f"/tmp/{sql_path.name}"
        print(f"[upload] {sql_path.name}")
        sftp.put(str(sql_path), host_path)
        run(client, f"sudo docker cp {host_path} inbound-postgres:{container_path}")
        print(f"[import] {sql_path.name}")
        code, out = run(
            client,
            f"sudo docker exec inbound-postgres psql -U inbound -d inbound_growth -v ON_ERROR_STOP=1 -f {container_path}",
            timeout=600,
        )
        if code != 0:
            print(out, file=sys.stderr)
            client.close()
            return code
        print(out[-500:] if len(out) > 500 else out or "(ok)")

    sftp.close()
    code, out = run(client, f"sudo docker exec inbound-postgres psql -U inbound -d inbound_growth -tAc \"{check_sql}\"")
    print(f"[after] {out}")
    client.close()
    return code


if __name__ == "__main__":
    raise SystemExit(main())
