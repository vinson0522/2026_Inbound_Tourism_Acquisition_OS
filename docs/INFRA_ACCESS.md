# 基础设施访问与凭证手册

> **文档定位**：中间件怎么连、用什么账号、本机/服务器有何差异。  
> **配套文档**：[TECH_STACK_COMPONENTS.md](./TECH_STACK_COMPONENTS.md)（组件版本与 BOM）、[../deploy/README.md](../deploy/README.md)（部署命令）。  
> **私密凭证**：服务器生产密码见同目录 **[INFRA_ACCESS.local.md](./INFRA_ACCESS.local.md)**（已 `.gitignore`，仅本机保留，含完整密码表）。

---

## 1. 环境总览

| 环境 | 用途 | Compose 文件 | 凭证来源 |
|------|------|--------------|----------|
| **本地开发** | 本机 `docker compose up` | `deploy/docker-compose.yml` | 本文 §3（固定 dev 默认值） |
| **共享服务器** | EC2 `18.139.209.10` | `deploy/docker-compose.prod.yml` | `INFRA_ACCESS.local.md` 或服务器 `/opt/inbound-growth/deploy/.env` |

**安全原则**

- 中间件端口在服务器上均绑定 **`127.0.0.1`**，外网不可直连。
- 本机访问远程中间件：**SSH 隧道**（见 §6），或应用部署在同一台服务器上连 `127.0.0.1`。
- 服务器 `.env` 权限 `600`，勿提交 Git。

---

## 2. 中间件清单与端口

### 2.1 本地开发（`deploy/docker-compose.yml`）

| 服务 | 容器名 | 宿主机地址 | Web / 管理台 |
|------|--------|------------|--------------|
| PostgreSQL 16 + pgvector | `inbound-postgres` | `localhost:5432` | — |
| Redis 7 | `inbound-redis` | `localhost:6379` | — |
| MinIO | `inbound-minio` | API `localhost:9000` | Console http://localhost:9001 |
| RabbitMQ | `inbound-rabbitmq` | AMQP `localhost:5672` | http://localhost:15672 |
| Langfuse 2 | `inbound-langfuse` | http://localhost:3000 | 首次打开注册管理员 |
| Gotenberg 8 | `inbound-gotenberg` | http://localhost:3002 | `/health` |

### 2.2 共享服务器（`deploy/docker-compose.prod.yml`）

| 服务 | 容器名 | 服务器本机地址 | 说明 |
|------|--------|----------------|------|
| PostgreSQL | `inbound-postgres` | `127.0.0.1:5432` | 库 `inbound_growth` + `langfuse` |
| Redis | `inbound-redis` | `127.0.0.1:6380` | **6380**（避开宝塔 Redis 6379） |
| MinIO | `inbound-minio` | `127.0.0.1:9000` / `9001` | 桶名 `inbound-growth` |
| RabbitMQ | `inbound-rabbitmq` | `127.0.0.1:5672` / `15672` | — |
| Langfuse | `inbound-langfuse` | http://127.0.0.1:3100 | **3100**（避开 Nginx 3000） |
| Gotenberg | `inbound-gotenberg` | http://127.0.0.1:3002 | — |

**服务器运维路径**

```text
/opt/inbound-growth/deploy/          # compose + .env
/opt/inbound-growth/database/ddl/    # DDL 脚本
```

---

## 3. 本地开发凭证与连接串

> 以下密码仅用于本地 dev，定义于 `deploy/docker-compose.yml`，可公开在文档中。

### 3.1 账号密码

| 组件 | 用户名 | 密码 | 数据库 / 备注 |
|------|--------|------|---------------|
| PostgreSQL | `inbound` | `inbound_dev_pass` | DB: `inbound_growth` |
| Redis | — | 无密码 | — |
| MinIO | `minioadmin` | `minioadmin_dev` | 桶：`inbound-growth`（需 mc 或控制台创建） |
| RabbitMQ | `inbound` | `inbound_dev_pass` | vhost: `/` |
| Langfuse | — | 首次访问 UI 注册 | `NEXTAUTH_SECRET` 见 compose 内联 dev 值 |

### 3.2 应用连接串（本地）

```properties
# Java inbound-core
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/inbound_growth
SPRING_DATASOURCE_USERNAME=inbound
SPRING_DATASOURCE_PASSWORD=inbound_dev_pass
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_RABBITMQ_HOST=localhost
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin_dev
```

```bash
# Python inbound-ai
DATABASE_URL=postgresql+asyncpg://inbound:inbound_dev_pass@localhost:5432/inbound_growth
REDIS_URL=redis://localhost:6379/1
RABBITMQ_URL=amqp://inbound:inbound_dev_pass@localhost:5672/
LANGFUSE_HOST=http://localhost:3000
GOTENBERG_URL=http://localhost:3002
```

### 3.3 命令行快速验证（本地）

```bash
# 启动
cd deploy && docker compose up -d

# PostgreSQL
docker exec -it inbound-postgres psql -U inbound -d inbound_growth -c "\dt"

# Redis
docker exec -it inbound-redis redis-cli ping

# RabbitMQ
curl -s -o /dev/null -w "%{http_code}" http://localhost:15672

# MinIO 健康
curl -s http://localhost:9000/minio/health/live

# Gotenberg
curl -s http://localhost:3002/health
```

---

## 4. 共享服务器凭证

> **具体密码不在此仓库提交。** 请打开本机文件：  
> **`docs/INFRA_ACCESS.local.md`**（与本文同目录，已 gitignore）  
> 或在服务器执行：`cat /opt/inbound-growth/deploy/.env`

`INFRA_ACCESS.local.md` 中包含：

- PostgreSQL / Redis / MinIO / RabbitMQ / Langfuse 全部账号密码
- Java / Python 即用连接串
- SSH 隧道后的本机等价地址

**重新从服务器拉取凭证**（部署或改密后）：

```bash
python deploy/scripts/export_infra_credentials.py --host 18.139.209.10 --key-file cert/im1.pem
```

---

## 5. 客户端与 GUI 连接

### 5.1 PostgreSQL（DBeaver / DataGrip / psql）

| 字段 | 本地 | 远程（需先开 SSH 隧道 §6） |
|------|------|---------------------------|
| Host | `localhost` | `localhost` |
| Port | `5432` | `5432`（隧道映射） |
| Database | `inbound_growth` | `inbound_growth` |
| User | `inbound` | `inbound` |
| Password | 见 §3.1 | 见 `INFRA_ACCESS.local.md` |

```bash
# psql via tunnel
psql "postgresql://inbound:<密码>@localhost:5432/inbound_growth"
```

### 5.2 Redis（Another Redis Desktop Manager / redis-cli）

| 字段 | 本地 | 远程（隧道后） |
|------|------|----------------|
| Host | `localhost` | `localhost` |
| Port | `6379` | **6380**（隧道映射 6380→服务器 6380） |
| Password | 无 | 见 `INFRA_ACCESS.local.md` |

```bash
redis-cli -h localhost -p 6380 -a '<密码>' ping
```

### 5.3 MinIO

| 项 | 本地 | 远程（隧道后） |
|----|------|----------------|
| API | http://localhost:9000 | http://localhost:9000 |
| Console | http://localhost:9001 | http://localhost:9001 |
| Access Key / Secret | §3.1 | `INFRA_ACCESS.local.md` |

### 5.4 RabbitMQ 管理台

| 项 | 本地 | 远程（隧道后） |
|----|------|----------------|
| URL | http://localhost:15672 | http://localhost:15672 |
| User / Pass | §3.1 | `INFRA_ACCESS.local.md` |

### 5.5 Langfuse

| 项 | 本地 | 远程（隧道后） |
|----|------|----------------|
| URL | http://localhost:3000 | http://localhost:3100 |
| 初始化 | 浏览器首次注册项目 | 同上 |

### 5.6 Gotenberg

```bash
curl http://localhost:3002/health
```

---

## 6. SSH 隧道（本机访问远程中间件）

**前提**：密钥 `cert/im1.pem`，用户 `ec2-user`，主机 `18.139.209.10`。

### 6.1 一次性映射（Windows PowerShell / Git Bash / macOS）

```bash
ssh -i cert/im1.pem -N ^
  -L 5432:127.0.0.1:5432 ^
  -L 6380:127.0.0.1:6380 ^
  -L 9000:127.0.0.1:9000 ^
  -L 9001:127.0.0.1:9001 ^
  -L 5672:127.0.0.1:5672 ^
  -L 15672:127.0.0.1:15672 ^
  -L 3100:127.0.0.1:3100 ^
  -L 3002:127.0.0.1:3002 ^
  -L 8090:127.0.0.1:8090 ^
  ec2-user@18.139.209.10
```

> Linux/macOS 将 `^` 换为 `\`。保持终端窗口不关，隧道即有效。

### 6.2 推荐 `~/.ssh/config` 片段

```ssh-config
Host inbound-dev
    HostName 18.139.209.10
    User ec2-user
    IdentityFile cert/im1.pem
    ServerAliveInterval 30
    ServerAliveCountMax 6
    LocalForward 5432 127.0.0.1:5432
    LocalForward 6380 127.0.0.1:6380
    LocalForward 9000 127.0.0.1:9000
    LocalForward 9001 127.0.0.1:9001
    LocalForward 5672 127.0.0.1:5672
    LocalForward 15672 127.0.0.1:15672
    LocalForward 3100 127.0.0.1:3100
    LocalForward 3002 127.0.0.1:3002
    LocalForward 8090 127.0.0.1:8090
```

连接：`ssh -N inbound-dev`

### 6.3 隧道建立后的等价地址

隧道开启后，本机应用配置与 §5 中「远程（隧道后）」列相同，密码用 `INFRA_ACCESS.local.md` 中的服务器值。

**EPIC-2 混合联调**（Java 本机、ai-api 可隧道或直连服务器）：

| 变量 | 混合联调（本机 worker） | 生产同机 compose |
|------|-------------------------|------------------|
| `AI_SERVICE_BASE_URL`（Java） | `http://127.0.0.1:8090`（隧道） | `http://ai-api:8090` |
| `CORE_CALLBACK_BASE_URL`（Python worker） | `http://localhost:8080` | `http://core-api:8080` |
| `DIAGNOSE_WORKER_ENABLED` | 本机 `true`；**服务器 ai-api 保持 false** | 服务器 ai-api `true`（Java 上机后） |

详见 [deploy/README.md](../deploy/README.md) §EPIC-2。

---

## 7. 服务器运维命令

```bash
ssh -i cert/im1.pem ec2-user@18.139.209.10

cd /opt/inbound-growth/deploy

# 状态
sudo /usr/local/bin/docker-compose -f docker-compose.prod.yml ps

# 日志
sudo /usr/local/bin/docker-compose -f docker-compose.prod.yml logs -f postgres

# 重启单个服务
sudo /usr/local/bin/docker-compose -f docker-compose.prod.yml restart redis

# 重新部署（从开发机）
python deploy/scripts/server_infra_deploy.py --host 18.139.209.10 --key-file cert/im1.pem

# 健康检查
python deploy/scripts/server_infra_verify.py --host 18.139.209.10 --key-file cert/im1.pem

# 更新 ai-api（EPIC-2 代码）
python deploy/scripts/server_ai_deploy.py --host 18.139.209.10 --key-file cert/im1.pem
```

---

## 8. 与旧宝塔服务的关系

| 端口 | 服务 | 归属 | 本项目是否使用 |
|------|------|------|----------------|
| 6379 | Redis | 宝塔（旧业务） | **否** — 用 Docker Redis **6380** |
| 3306 | MySQL 5.7 | 宝塔（旧业务） | **否** — MVP 用 PostgreSQL **5432** |
| 8848 | Nacos | 旧 Docker | **否** |
| 38456 | 宝塔面板 | 系统 | 无关 |

---

## 9. 变更同步规则

| 变更类型 | 需同步 |
|----------|--------|
| 改 compose 端口/镜像 | `deploy/docker-compose*.yml` + 本文 §2 + `TECH_STACK_COMPONENTS.md` §5 |
| 改本地 dev 密码 | `deploy/docker-compose.yml` + 本文 §3 |
| 改服务器密码 | 服务器 `.env` + 运行 `export_infra_credentials.py` 更新 `INFRA_ACCESS.local.md` |
| 新增中间件 | compose + 本文 + `TECH_STACK_COMPONENTS.md` |

---

*Last updated: 2026-06-26*
