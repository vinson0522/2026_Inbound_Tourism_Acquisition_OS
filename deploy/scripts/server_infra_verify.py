#!/usr/bin/env python3
"""Verify inbound infrastructure on remote server."""
import argparse
import paramiko


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", required=True)
    ap.add_argument("--user", default="ec2-user")
    ap.add_argument("--key-file", default="cert/im1.pem")
    args = ap.parse_args()

    key = paramiko.RSAKey.from_private_key_file(args.key_file)
    c = paramiko.SSHClient()
    c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    c.connect(args.host, username=args.user, pkey=key, timeout=15, allow_agent=False, look_for_keys=False)

    cmds = [
        ("compose ps", "cd /opt/inbound-growth/deploy && sudo /usr/local/bin/docker-compose -f docker-compose.prod.yml ps"),
        ("postgres tables", "sudo docker exec inbound-postgres psql -U inbound -d inbound_growth -tAc \"SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY 1 LIMIT 5\""),
        ("langfuse db", "sudo docker exec inbound-postgres psql -U inbound -d langfuse -tAc 'SELECT 1' 2>&1"),
        ("minio buckets", "sudo docker run --rm --network inbound-network minio/mc:latest sh -c 'mc alias set l http://minio:9000 inbound_minio $(grep MINIO_ROOT_PASSWORD /opt/inbound-growth/deploy/.env | cut -d= -f2) && mc ls l/' 2>&1"),
        ("langfuse http", "curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:3100/ || true"),
        ("gotenberg", "curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:3002/health || true"),
        ("public 5432", "timeout 1 bash -c 'exec 3<>/dev/tcp/18.139.209.10/5432' 2>/dev/null && echo OPEN || echo CLOSED"),
    ]
    for name, cmd in cmds:
        _, o, e = c.exec_command(cmd, timeout=60)
        out = (o.read() + e.read()).decode("utf-8", errors="replace").strip()
        print(f"[{name}]")
        print(out or "(empty)")
        print()

    c.close()


if __name__ == "__main__":
    main()
