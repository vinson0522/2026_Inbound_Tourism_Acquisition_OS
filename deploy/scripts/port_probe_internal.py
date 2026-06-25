#!/usr/bin/env python3
"""Probe ports from inside the server via SSH (localhost + public IP)."""
import argparse
import paramiko


def run(host: str, user: str, key_file: str, ports: list[int]) -> None:
    key = paramiko.RSAKey.from_private_key_file(key_file)
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(host, username=user, pkey=key, timeout=15, allow_agent=False, look_for_keys=False)

    port_list = " ".join(str(p) for p in ports)
    script = f"""
ports="{port_list}"
for target in 127.0.0.1 {host}; do
  echo "=== target $target ==="
  for p in $ports; do
    if timeout 1 bash -c "exec 3<>/dev/tcp/$target/$p" 2>/dev/null; then
      echo "$target:$p OPEN"
      exec 3>&- 2>/dev/null || true
    else
      echo "$target:$p CLOSED"
    fi
  done
done
"""
    _, stdout, stderr = client.exec_command(f"bash -s <<'EOF'\n{script}\nEOF", timeout=60)
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    print(out)
    if err.strip():
        print("STDERR:", err)
    client.close()


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--host", required=True)
    p.add_argument("--user", default="ec2-user")
    p.add_argument("--key-file", default="cert/im1.pem")
    p.add_argument(
        "--ports",
        default="22,80,443,888,3000,6379,3306,8848,9848,9849,38456,5432,5672,9000,15672,8080,8090,111",
    )
    args = p.parse_args()
    ports = [int(x.strip()) for x in args.ports.split(",") if x.strip()]
    run(args.host, args.user, args.key_file, ports)


if __name__ == "__main__":
    main()
