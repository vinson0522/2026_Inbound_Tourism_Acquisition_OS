#!/usr/bin/env python3
"""
Read-only server environment audit (security / purity baseline).
Usage:
  pip install paramiko
  python server_audit.py --host HOST --user root --password '***'

Does NOT modify the remote system.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass, field
from datetime import datetime, timezone

try:
    import paramiko
except ImportError:
    print("Missing dependency: pip install paramiko", file=sys.stderr)
    sys.exit(1)


SUSPICIOUS_PROCESS_PATTERNS = re.compile(
    r"xmrig|kdevtmpfsi|kinsing|miner|cryptonight|\.hidden|/tmp/\.",
    re.I,
)
SUSPICIOUS_WEB_EXTENSIONS = re.compile(
    r"\.(php|jsp|asp|aspx|cgi)$", re.I
)


@dataclass
class Finding:
    severity: str  # CRITICAL, HIGH, MEDIUM, LOW, INFO
    category: str
    message: str
    evidence: str = ""


@dataclass
class AuditReport:
    host: str
    scanned_at: str
    findings: list[Finding] = field(default_factory=list)
    raw_sections: dict[str, str] = field(default_factory=dict)

    def add(self, severity: str, category: str, message: str, evidence: str = "") -> None:
        self.findings.append(Finding(severity, category, message, evidence[:2000]))


def run_cmd(client: paramiko.SSHClient, cmd: str, timeout: int = 60) -> tuple[int, str, str]:
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    code = stdout.channel.recv_exit_status()
    out = stdout.read().decode("utf-8", errors="replace").strip()
    err = stderr.read().decode("utf-8", errors="replace").strip()
    return code, out, err


def collect(client: paramiko.SSHClient, report: AuditReport, name: str, cmd: str) -> str:
    code, out, err = run_cmd(client, cmd)
    text = out if out else err
    report.raw_sections[name] = text
    if code != 0 and not out:
        report.add("LOW", "command", f"Command exited {code}: {name}", err or cmd)
    return text


def analyze(report: AuditReport) -> None:
    os_info = report.raw_sections.get("os", "")
    if os_info:
        report.add("INFO", "system", "OS release", os_info.splitlines()[0] if os_info else "")

    uptime = report.raw_sections.get("uptime", "")
    if uptime:
        days_match = re.search(r"up\s+(\d+)\s+days", uptime)
        if days_match and int(days_match.group(1)) > 180:
            report.add(
                "MEDIUM",
                "patching",
                f"Long uptime ({days_match.group(1)} days) — kernel/security patches may be pending",
                uptime,
            )

    passwd = report.raw_sections.get("passwd_root", "")
    root_lines = [l for l in passwd.splitlines() if l.startswith("root:") or ":0:" in l.split(":")[2:3]]
    if len([l for l in passwd.splitlines() if l.endswith(":0:0:")]) > 1:
        report.add("CRITICAL", "users", "Multiple UID=0 accounts detected", passwd)

    auth_keys = report.raw_sections.get("root_authorized_keys", "")
    if auth_keys.strip():
        key_count = len([l for l in auth_keys.splitlines() if l.strip() and not l.startswith("#")])
        report.add("INFO", "ssh", f"root authorized_keys entries: {key_count}", auth_keys[:500])
    else:
        report.add("INFO", "ssh", "No root authorized_keys file or empty")

    last = report.raw_sections.get("last_logins", "")
    if last:
        report.add("INFO", "auth", "Recent login summary", last[:800])

    ports = report.raw_sections.get("listening_ports", "")
    if ports:
        public_bind = [l for l in ports.splitlines() if re.search(r"0\.0\.0\.0:|:::", l)]
        risky = [l for l in public_bind if re.search(r":(22|3306|5432|6379|5672|9000|27017|9200)\b", l)]
        if risky:
            report.add(
                "HIGH",
                "network",
                "Sensitive ports bound to 0.0.0.0 (public)",
                "\n".join(risky[:20]),
            )
        report.add("INFO", "network", f"Listening sockets: {len(ports.splitlines())} lines", ports[:1200])

    ps = report.raw_sections.get("processes", "")
    for line in ps.splitlines():
        if SUSPICIOUS_PROCESS_PATTERNS.search(line):
            report.add("CRITICAL", "process", "Suspicious process pattern", line)

    cron = report.raw_sections.get("cron", "")
    if cron:
        suspicious_cron = [l for l in cron.splitlines() if re.search(r"curl|wget|/tmp/|base64|python -c|bash -i", l, re.I)]
        if suspicious_cron:
            report.add("HIGH", "persistence", "Suspicious cron entries", "\n".join(suspicious_cron[:15]))
        report.add("INFO", "persistence", "Cron/cron.d summary captured", cron[:800])

    web_recent = report.raw_sections.get("web_recent", "")
    if web_recent:
        script_hits = [l for l in web_recent.splitlines() if SUSPICIOUS_WEB_EXTENSIONS.search(l)]
        if script_hits:
            report.add(
                "MEDIUM",
                "webroot",
                "Recently modified script files under /www/wwwroot",
                "\n".join(script_hits[:20]),
            )

    docker_ps = report.raw_sections.get("docker_ps", "")
    if "CONTAINER" in docker_ps:
        report.add("INFO", "docker", "Docker containers running", docker_ps[:800])
    elif "command not found" in docker_ps.lower() or not docker_ps:
        report.add("INFO", "docker", "Docker not installed or not running")

    yum = report.raw_sections.get("updates", "")
    if yum and "Updated" not in yum and len(yum.splitlines()) > 3:
        report.add("MEDIUM", "patching", "Pending package updates may exist", yum[:600])

    bt_security = report.raw_sections.get("bt_panel", "")
    if bt_security:
        report.add("INFO", "panel", "Baota/panel hints", bt_security[:600])


def severity_rank(s: str) -> int:
    return {"CRITICAL": 0, "HIGH": 1, "MEDIUM": 2, "LOW": 3, "INFO": 4}.get(s, 5)


def print_report(report: AuditReport) -> None:
    print("=" * 72)
    print(f"Server Audit Report — {report.host}")
    print(f"Scanned at: {report.scanned_at} UTC")
    print("=" * 72)

    findings = sorted(report.findings, key=lambda f: (severity_rank(f.severity), f.category))
    counts: dict[str, int] = {}
    for f in findings:
        counts[f.severity] = counts.get(f.severity, 0) + 1

    print("\nSummary:", ", ".join(f"{k}={v}" for k, v in sorted(counts.items(), key=lambda x: severity_rank(x[0]))))

    for f in findings:
        if f.severity == "INFO":
            continue
        print(f"\n[{f.severity}] {f.category}: {f.message}")
        if f.evidence:
            print("  evidence:")
            for line in f.evidence.splitlines()[:8]:
                print(f"    {line}")

    print("\n--- INFO (sample) ---")
    for f in findings:
        if f.severity != "INFO":
            continue
        print(f"  • {f.category}: {f.message}")

    print("\n--- Raw key sections ---")
    for key in ("os", "uptime", "listening_ports", "docker_ps"):
        if key in report.raw_sections and report.raw_sections[key]:
            print(f"\n## {key}\n{report.raw_sections[key][:1500]}")


def audit_host(
    host: str,
    user: str,
    password: str = "",
    port: int = 22,
    key_file: str = "",
    use_sudo: bool = False,
) -> AuditReport:
    report = AuditReport(host=host, scanned_at=datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S"))

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    connect_kwargs: dict = {
        "hostname": host,
        "port": port,
        "username": user,
        "timeout": 25,
        "allow_agent": False,
        "look_for_keys": False,
    }
    if key_file:
        connect_kwargs["pkey"] = paramiko.RSAKey.from_private_key_file(key_file)
    elif password:
        connect_kwargs["password"] = password
    else:
        report.add("CRITICAL", "connect", "Either --password or --key-file is required")
        return report

    try:
        client.connect(**connect_kwargs)
    except Exception as e:
        report.add("CRITICAL", "connect", f"SSH connection failed: {e}")
        return report

    prefix = "sudo " if use_sudo else ""

    try:
        collect(client, report, "whoami", "whoami; id")
        collect(client, report, "os", f"{prefix}cat /etc/os-release 2>/dev/null; uname -a")
        collect(client, report, "uptime", f"{prefix}uptime; {prefix}who -b 2>/dev/null")
        collect(client, report, "passwd_root", f"{prefix}awk -F: '($3==0){{print}}' /etc/passwd")
        collect(client, report, "root_authorized_keys", f"{prefix}cat /root/.ssh/authorized_keys 2>/dev/null")
        collect(client, report, "last_logins", f"{prefix}last -15 2>/dev/null; {prefix}lastb -10 2>/dev/null")
        collect(client, report, "listening_ports", f"{prefix}ss -lntp 2>/dev/null || {prefix}netstat -lntp 2>/dev/null")
        collect(client, report, "processes", f"{prefix}ps aux --sort=-%cpu 2>/dev/null | head -25")
        collect(client, report, "cron", f"{prefix}crontab -l 2>/dev/null; echo '---'; {prefix}ls -la /etc/cron.* 2>/dev/null; echo '---'; {prefix}grep -R . /var/spool/cron/ 2>/dev/null | head -40")
        collect(
            client,
            report,
            "web_recent",
            f"{prefix}find /www/wwwroot -type f -mtime -14 2>/dev/null | head -60",
        )
        collect(client, report, "docker_ps", f"{prefix}docker ps -a 2>/dev/null; which docker 2>/dev/null")
        collect(client, report, "updates", f"{prefix}yum check-update 2>/dev/null | head -25")
        collect(
            client,
            report,
            "bt_panel",
            f"{prefix}ls -la /www/server/panel 2>/dev/null | head -5; {prefix}bt 2>/dev/null | head -5",
        )
        collect(client, report, "disk", f"{prefix}df -hT / /www 2>/dev/null")
        collect(client, report, "memory", f"{prefix}free -h 2>/dev/null")

        analyze(report)
    finally:
        client.close()

    return report


def main() -> None:
    parser = argparse.ArgumentParser(description="Read-only SSH server audit")
    parser.add_argument("--host", required=True)
    parser.add_argument("--user", default="root")
    parser.add_argument("--password", default="")
    parser.add_argument("--key-file", default="", help="Path to PEM private key")
    parser.add_argument("--sudo", action="store_true", help="Prefix privileged commands with sudo")
    parser.add_argument("--port", type=int, default=22)
    parser.add_argument("--json-out", default="", help="Optional path to save JSON report")
    args = parser.parse_args()

    if not args.password and not args.key_file:
        print("Error: --password or --key-file required (do not commit credentials to git)", file=sys.stderr)
        sys.exit(2)

    report = audit_host(
        args.host,
        args.user,
        args.password,
        args.port,
        key_file=args.key_file,
        use_sudo=args.sudo,
    )
    print_report(report)

    if args.json_out:
        payload = {
            "host": report.host,
            "scanned_at": report.scanned_at,
            "findings": [f.__dict__ for f in report.findings],
        }
        with open(args.json_out, "w", encoding="utf-8") as fp:
            json.dump(payload, fp, ensure_ascii=False, indent=2)
        print(f"\nJSON saved: {args.json_out}")

    critical = sum(1 for f in report.findings if f.severity == "CRITICAL")
    high = sum(1 for f in report.findings if f.severity == "HIGH")
    sys.exit(1 if critical or high else 0)


if __name__ == "__main__":
    main()
