#!/usr/bin/env python3
"""Quick SSH audit — avoids slow commands (yum, bt)."""
import argparse
import re
import sys

import paramiko


def run(client, cmd: str, timeout: int = 25) -> str:
    _, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    return (stdout.read() + stderr.read()).decode("utf-8", errors="replace").strip()


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--host", required=True)
    p.add_argument("--user", default="ec2-user")
    p.add_argument("--key-file", required=True)
    args = p.parse_args()

    key = paramiko.RSAKey.from_private_key_file(args.key_file)
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(
        args.host,
        username=args.user,
        pkey=key,
        timeout=20,
        allow_agent=False,
        look_for_keys=False,
    )

    cmds = {
        "meta": "curl -s --max-time 2 http://169.254.169.254/latest/meta-data/public-ipv4; echo; hostname",
        "whoami": "whoami; id; sudo whoami 2>/dev/null",
        "os": "cat /etc/os-release | head -5; uname -a",
        "uptime": "uptime",
        "uid0": "sudo awk -F: '($3==0){print}' /etc/passwd",
        "ports": "sudo ss -lntp 2>/dev/null | head -50",
        "ps": "ps aux --sort=-%cpu | head -15",
        "docker": "docker ps -a 2>/dev/null; which docker 2>/dev/null || true",
        "webroot": "sudo ls -la /www/wwwroot 2>/dev/null | head -25",
        "web_recent": "sudo find /www/wwwroot -type f -mtime -14 2>/dev/null | head -35",
        "cron": "sudo crontab -l 2>/dev/null; echo '---'; sudo ls /etc/cron.d 2>/dev/null",
        "disk": "df -hT /",
        "mem": "free -h",
        "baota": "sudo test -d /www/server/panel && echo BAOTA=YES || echo BAOTA=NO",
        "last": "sudo last -10 2>/dev/null | head -12",
    }

    findings: list[str] = []
    print("=" * 60)
    print(f"Quick Audit — {args.host} as {args.user} key={args.key_file}")
    print("=" * 60)

    for name, cmd in cmds.items():
        print(f"\n=== {name} ===")
        try:
            out = run(client, cmd)
            print(out[:3000] if out else "(empty)")
        except Exception as ex:
            print(f"ERROR: {ex}")
            continue

        if name == "uid0" and out:
            lines = [l for l in out.splitlines() if l.strip()]
            if len(lines) > 1:
                findings.append("[CRITICAL] Multiple UID=0 accounts")
        if name == "ports" and re.search(r"0\.0\.0\.0:(5432|6379|3306|5672|9000|27017)", out):
            findings.append("[HIGH] Database/cache port bound to 0.0.0.0")
        if name == "ps" and re.search(r"xmrig|miner|kinsing|kdevtmpfsi", out, re.I):
            findings.append("[CRITICAL] Suspicious process name")
        if name == "uptime":
            m = re.search(r"up\s+(\d+)\s+days", out)
            if m and int(m.group(1)) > 180:
                findings.append(f"[MEDIUM] Long uptime ({m.group(1)} days) — patch/reboot recommended")

    client.close()

    print("\n" + "=" * 60)
    print("AUTO FINDINGS:")
    if findings:
        for f in findings:
            print(" ", f)
    else:
        print("  No critical auto-rules triggered — manual review still required")
    print("=" * 60)


if __name__ == "__main__":
    main()
