#!/usr/bin/env python3
"""Create MinIO bucket on remote server (one-off fix)."""
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

    cmd = r"""
set -a
source /opt/inbound-growth/deploy/.env
set +a
sudo docker run --rm --network inbound-network --entrypoint /bin/sh minio/mc:latest -c "
  mc alias set local http://minio:9000 ${MINIO_ROOT_USER} ${MINIO_ROOT_PASSWORD} &&
  mc mb --ignore-existing local/inbound-growth &&
  mc anonymous set none local/inbound-growth &&
  mc ls local/
"
"""
    _, o, e = c.exec_command(cmd, timeout=120)
    print(o.read().decode())
    err = e.read().decode()
    if err:
        print(err)
    print("exit", o.channel.recv_exit_status())
    c.close()


if __name__ == "__main__":
    main()
