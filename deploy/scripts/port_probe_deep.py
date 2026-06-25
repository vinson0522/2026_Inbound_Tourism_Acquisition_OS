#!/usr/bin/env python3
"""Deep external port probe with banner / protocol check."""
import argparse
import socket
import ssl


def probe(host: str, port: int, send: bytes | None = None, use_tls: bool = False) -> dict:
    raw = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    raw.settimeout(5)
    result = {"port": port, "tcp": "UNKNOWN", "banner": "", "note": ""}
    try:
        raw.connect((host, port))
        result["tcp"] = "OPEN"
        sock: socket.socket = raw
        if use_tls:
            ctx = ssl.create_default_context()
            ctx.check_hostname = False
            ctx.verify_mode = ssl.CERT_NONE
            sock = ctx.wrap_socket(raw, server_hostname=host)
        if send:
            sock.sendall(send)
        sock.settimeout(3)
        try:
            data = sock.recv(512)
            result["banner"] = data.decode("utf-8", errors="replace").strip()[:200]
        except socket.timeout:
            result["banner"] = ""
            result["note"] = "no banner"
    except ConnectionRefusedError:
        result["tcp"] = "REFUSED"
    except socket.timeout:
        result["tcp"] = "TIMEOUT"
    except OSError as e:
        result["tcp"] = f"ERR({getattr(e, 'errno', '?')})"
        result["note"] = str(e)[:80]
    finally:
        try:
            raw.close()
        except OSError:
            pass
    return result


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--host", required=True)
    args = p.parse_args()
    host = args.host

    checks = [
        (22, "SSH", None, False),
        (80, "HTTP-nginx", b"GET / HTTP/1.0\r\nHost: test\r\n\r\n", False),
        (443, "HTTPS-nginx", b"GET / HTTP/1.0\r\nHost: test\r\n\r\n", True),
        (888, "nginx-baota", b"GET / HTTP/1.0\r\nHost: test\r\n\r\n", False),
        (3000, "nginx-service", b"GET / HTTP/1.0\r\nHost: test\r\n\r\n", False),
        (6379, "Redis", b"*1\r\n$4\r\nPING\r\n", False),
        (3306, "MySQL", None, False),
        (8848, "Nacos-HTTP", b"GET /nacos HTTP/1.0\r\nHost: test\r\n\r\n", False),
        (9848, "Nacos-gRPC", None, False),
        (9849, "Nacos", None, False),
        (38456, "Baota-Panel", b"GET / HTTP/1.0\r\nHost: test\r\n\r\n", False),
        (111, "rpcbind", None, False),
        (5672, "RabbitMQ", None, False),
        (5432, "PostgreSQL", None, False),
        (9000, "MinIO", b"GET /minio/health/live HTTP/1.0\r\nHost: test\r\n\r\n", False),
        (15672, "RabbitMQ-Mgmt", b"GET / HTTP/1.0\r\nHost: test\r\n\r\n", False),
        (8080, "Java-reserved", b"GET / HTTP/1.0\r\nHost: test\r\n\r\n", False),
        (8090, "AI-reserved", b"GET / HTTP/1.0\r\nHost: test\r\n\r\n", False),
    ]

    print(f"Deep probe target: {host} (from this machine)\n")
    print(f"{'PORT':<6} {'NAME':<16} {'TCP':<10} {'VERIFIED':<10} BANNER/NOTE")
    print("-" * 90)

    verified_open = []
    for port, name, payload, tls in checks:
        r = probe(host, port, payload, tls)
        tcp = r["tcp"]
        banner = r["banner"].replace("\r", " ").replace("\n", " | ")[:70]
        verified = "yes" if tcp == "OPEN" and (banner or r["note"]) else ("tcp-only" if tcp == "OPEN" else "no")
        if tcp == "OPEN" and banner:
            verified = "YES"
            verified_open.append((port, name, banner))
        elif tcp == "OPEN":
            verified = "OPEN?"
            verified_open.append((port, name, r["note"] or "connected, no banner"))
        else:
            verified = "NO"

        line = f"{port:<6} {name:<16} {tcp:<10} {verified:<10} {banner or r['note']}"
        print(line.encode("ascii", errors="replace").decode("ascii"))

    print("\n=== EXTERNALLY REACHABLE (confirmed or TCP open) ===")
    for port, name, info in verified_open:
        print(f"  {port} {name}: {info[:80]}")


if __name__ == "__main__":
    main()
