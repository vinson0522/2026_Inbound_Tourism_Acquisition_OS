#!/usr/bin/env python3
"""Probe TCP port reachability from local machine to target host."""
import argparse
import socket
import time


def probe(host: str, port: int, timeout: float = 4.0) -> tuple[str, str]:
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(timeout)
    t0 = time.time()
    try:
        rc = sock.connect_ex((host, port))
        ms = f"{int((time.time() - t0) * 1000)}ms"
        if rc == 0:
            return "OPEN", ms
        return "CLOSED/FILTERED", "-"
    except socket.timeout:
        return "TIMEOUT", f">{int(timeout * 1000)}ms"
    except OSError as e:
        return "ERROR", str(e)[:40]
    finally:
        sock.close()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", required=True)
    parser.add_argument("--timeout", type=float, default=4.0)
    args = parser.parse_args()

    ports = [
        (22, "SSH"),
        (80, "HTTP / nginx"),
        (443, "HTTPS / nginx"),
        (888, "nginx / 宝塔"),
        (3000, "nginx / 服务"),
        (6379, "Redis"),
        (3306, "MySQL"),
        (8848, "Nacos HTTP"),
        (9848, "Nacos gRPC"),
        (9849, "Nacos"),
        (38456, "宝塔面板 BT-Panel"),
        (111, "rpcbind"),
        (5672, "RabbitMQ"),
        (5432, "PostgreSQL"),
        (9000, "MinIO"),
        (15672, "RabbitMQ 管理"),
        (8080, "Java API(预留)"),
        (8090, "AI API(预留)"),
    ]

    print(f"Target: {args.host}  (probe from this machine)\n")
    print(f"{'PORT':<8} {'SERVICE':<22} {'STATUS':<18} LATENCY")
    print("-" * 62)

    open_ports = []
    for port, name in ports:
        status, latency = probe(args.host, port, args.timeout)
        print(f"{port:<8} {name:<22} {status:<18} {latency}")
        if status == "OPEN":
            open_ports.append((port, name))

    print("\n" + "=" * 62)
    print(f"Summary: {len(open_ports)} port(s) reachable from outside")
    for port, name in open_ports:
        print(f"  • {port} — {name}")


if __name__ == "__main__":
    main()
