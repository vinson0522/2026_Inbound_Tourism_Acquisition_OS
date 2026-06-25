#!/usr/bin/env python3
"""Pull server /opt/inbound-growth/deploy/.env and refresh docs/INFRA_ACCESS.local.md."""
from __future__ import annotations

import argparse
from datetime import date
from pathlib import Path

import paramiko

ROOT = Path(__file__).resolve().parents[2]
LOCAL_DOC = ROOT / "docs" / "INFRA_ACCESS.local.md"


def parse_env(text: str) -> dict[str, str]:
    out: dict[str, str] = {}
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        out[k.strip()] = v.strip()
    return out


def build_local_md(env: dict[str, str], host: str) -> str:
    pg_user = env.get("POSTGRES_USER", "inbound")
    pg_pass = env["POSTGRES_PASSWORD"]
    pg_db = env.get("POSTGRES_DB", "inbound_growth")
    redis_pass = env["REDIS_PASSWORD"]
    rq_user = env.get("RABBITMQ_DEFAULT_USER", "inbound")
    rq_pass = env["RABBITMQ_DEFAULT_PASS"]
    minio_user = env.get("MINIO_ROOT_USER", "inbound_minio")
    minio_pass = env["MINIO_ROOT_PASSWORD"]
    nextauth = env.get("NEXTAUTH_SECRET", "")

    today = date.today().isoformat()
    return f"""# 基础设施访问与凭证手册（本机私密副本）

> **⚠️ 含服务器生产密码，已加入 `.gitignore`，勿提交 Git、勿外传。**  
> 结构与说明见仓库内 [INFRA_ACCESS.md](./INFRA_ACCESS.md)；**本文档 = 完整版 + 服务器凭证**。  
> 生成方式：`python deploy/scripts/export_infra_credentials.py --host {host} --key-file cert/im1.pem`

---

## 1. 环境总览

| 环境 | 主机 | 凭证文件（服务器） |
|------|------|-------------------|
| 本地开发 | `localhost` | `deploy/docker-compose.yml` 内联 dev 值 |
| 共享服务器 | `{host}` | `/opt/inbound-growth/deploy/.env` |

访问远程中间件：先开 SSH 隧道（§6），再连 `localhost` 对应端口。

---

## 2. 端口速查

### 本地

| 服务 | 地址 |
|------|------|
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |
| MinIO API / Console | `9000` / `9001` |
| RabbitMQ / 管理台 | `5672` / `15672` |
| Langfuse | http://localhost:3000 |
| Gotenberg | http://localhost:3002 |

### 服务器（仅 127.0.0.1；隧道后在本机用左列端口）

| 服务 | 服务器地址 | 隧道后本机 |
|------|------------|------------|
| PostgreSQL | `127.0.0.1:5432` | `localhost:5432` |
| Redis | `127.0.0.1:6380` | `localhost:6380` |
| MinIO | `9000` / `9001` | 同左 |
| RabbitMQ | `5672` / `15672` | 同左 |
| Langfuse | http://127.0.0.1:3100 | http://localhost:3100 |
| Gotenberg | http://127.0.0.1:3002 | http://localhost:3002 |

---

## 3. 本地开发凭证

| 组件 | 用户名 | 密码 | 备注 |
|------|--------|------|------|
| PostgreSQL | `inbound` | `inbound_dev_pass` | DB: `inbound_growth` |
| Redis | — | 无 | — |
| MinIO | `minioadmin` | `minioadmin_dev` | — |
| RabbitMQ | `inbound` | `inbound_dev_pass` | — |

---

## 4. 共享服务器凭证（生产）

> 同步自 `/opt/inbound-growth/deploy/.env`，更新时间：{today}

| 组件 | 用户名 / Key | 密码 / Secret | 备注 |
|------|--------------|---------------|------|
| PostgreSQL | `{pg_user}` | `{pg_pass}` | DB: `{pg_db}`；Langfuse 库: `langfuse` |
| Redis | — | `{redis_pass}` | 端口 **6380** |
| MinIO | `{minio_user}` | `{minio_pass}` | 桶: `inbound-growth` |
| RabbitMQ | `{rq_user}` | `{rq_pass}` | — |
| Langfuse | — | `NEXTAUTH_SECRET={nextauth}` | UI: http://127.0.0.1:3100 |

---

## 5. 连接串（复制即用）

### 5.1 服务器 — Java（`inbound-core`，应用跑在 EC2 上）

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/{pg_db}
SPRING_DATASOURCE_USERNAME={pg_user}
SPRING_DATASOURCE_PASSWORD={pg_pass}
SPRING_REDIS_HOST=127.0.0.1
SPRING_REDIS_PORT=6380
SPRING_REDIS_PASSWORD={redis_pass}
SPRING_RABBITMQ_HOST=127.0.0.1
SPRING_RABBITMQ_USERNAME={rq_user}
SPRING_RABBITMQ_PASSWORD={rq_pass}
MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_ACCESS_KEY={minio_user}
MINIO_SECRET_KEY={minio_pass}
LANGFUSE_HOST=http://127.0.0.1:3100
GOTENBERG_URL=http://127.0.0.1:3002
```

### 5.2 服务器 — Python（`inbound-ai`，应用跑在 EC2 上）

```bash
DATABASE_URL=postgresql+asyncpg://{pg_user}:{pg_pass}@127.0.0.1:5432/{pg_db}
REDIS_URL=redis://:{redis_pass}@127.0.0.1:6380/1
RABBITMQ_URL=amqp://{rq_user}:{rq_pass}@127.0.0.1:5672/
MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_ACCESS_KEY={minio_user}
MINIO_SECRET_KEY={minio_pass}
LANGFUSE_HOST=http://127.0.0.1:3100
GOTENBERG_URL=http://127.0.0.1:3002
```

### 5.3 本机 + SSH 隧道（开发机连远程）

隧道开启后，Host 一律填 **`localhost`**，端口与密码同 §4 / §5.2（Redis 用 **6380**）。

```bash
# PostgreSQL
psql "postgresql://{pg_user}:{pg_pass}@localhost:5432/{pg_db}"

# Redis
redis-cli -h localhost -p 6380 -a '{redis_pass}' ping

# MinIO Console → http://localhost:9001
# RabbitMQ     → http://localhost:15672
```

---

## 6. SSH 隧道

```bash
ssh -i cert/im1.pem -N \\
  -L 5432:127.0.0.1:5432 \\
  -L 6380:127.0.0.1:6380 \\
  -L 9000:127.0.0.1:9000 \\
  -L 9001:127.0.0.1:9001 \\
  -L 5672:127.0.0.1:5672 \\
  -L 15672:127.0.0.1:15672 \\
  -L 3100:127.0.0.1:3100 \\
  -L 3002:127.0.0.1:3002 \\
  ec2-user@{host}
```

---

## 7. 服务器 SSH

```bash
ssh -i cert/im1.pem ec2-user@{host}
cat /opt/inbound-growth/deploy/.env
```

---

*本文件由 export_infra_credentials.py 自动生成，更新时间 {today}。*
"""


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", required=True)
    ap.add_argument("--user", default="ec2-user")
    ap.add_argument("--key-file", default="cert/im1.pem")
    ap.add_argument("--output", default=str(LOCAL_DOC))
    args = ap.parse_args()

    key_path = ROOT / args.key_file
    key = paramiko.RSAKey.from_private_key_file(str(key_path))
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(args.host, username=args.user, pkey=key, timeout=20, allow_agent=False, look_for_keys=False)

    _, stdout, stderr = client.exec_command("cat /opt/inbound-growth/deploy/.env", timeout=30)
    text = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    client.close()

    if not text.strip():
        print(err or "empty .env")
        return 1

    env = parse_env(text)
    required = ["POSTGRES_PASSWORD", "REDIS_PASSWORD", "RABBITMQ_DEFAULT_PASS", "MINIO_ROOT_PASSWORD"]
    missing = [k for k in required if k not in env]
    if missing:
        print("Missing keys in server .env:", missing)
        return 1

    out_path = Path(args.output)
    out_path.write_text(build_local_md(env, args.host), encoding="utf-8")
    print(f"Wrote {out_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
