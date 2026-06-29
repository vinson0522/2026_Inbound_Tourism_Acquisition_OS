# 部署与本地开发

> **连接与密码**：见 [../docs/INFRA_ACCESS.md](../docs/INFRA_ACCESS.md)（结构说明）及本机 `../docs/INFRA_ACCESS.local.md`（含服务器凭证，gitignore）。

## 一键启动（MVP 基础设施）

### 本地开发（D 盘隔离，推荐）

**完整安装 / 清理手册** → **[LOCAL_DOCKER.md](LOCAL_DOCKER.md)**（数据在 `D:\Dev\SDKs\Docker\inbound-growth`，不占 C 盘）

```powershell
cd deploy
copy .env.local.example .env   # 填 GEMINI_API_KEY
.\scripts\local_docker_bootstrap.ps1
.\scripts\import_ruoyi_pg_local.ps1
```

### 本地开发（默认 compose，数据在 Docker named volume）

```bash
cd deploy
cp .env.example .env   # 可选，当前 compose 已内联 dev 默认值
docker compose up -d
docker compose ps
```

### 共享服务器（仅本机监听，已避开宝塔 Redis:6379 / Nginx:3000）

```bash
# 从开发机一键部署到服务器
python deploy/scripts/server_infra_deploy.py --host YOUR_IP --user ec2-user --key-file cert/im1.pem

# 验证
python deploy/scripts/server_infra_verify.py --host YOUR_IP --key-file cert/im1.pem

# 部署 / 更新 ai-api（EPIC-10）
python deploy/scripts/server_ai_deploy.py --host YOUR_IP --key-file cert/im1.pem
```

服务器路径：`/opt/inbound-growth/deploy/`  
密钥与连接串：`/opt/inbound-growth/deploy/.env`（chmod 600，勿提交 Git）

| 服务 | 宿主机地址 | 说明 |
|------|------------|------|
| PostgreSQL | `127.0.0.1:5432` | 已初始化 28 表 + demo 种子 |
| Redis | `127.0.0.1:6380` | 独立实例，带密码（非宝塔 6379） |
| MinIO | `127.0.0.1:9000` / `9001` | 桶 `inbound-growth` |
| RabbitMQ | `127.0.0.1:5672` / `15672` | 管理台仅本机 |
| Langfuse | `127.0.0.1:3100` | 可观测 |
| Gotenberg | `127.0.0.1:3002` | PDF 转换 |
| AI 服务 | `127.0.0.1:8090` | inbound-ai `/health` |

本地联调需 SSH 隧道，命令见 [../docs/INFRA_ACCESS.md](../docs/INFRA_ACCESS.md) §6。

同步服务器凭证到本机：

```bash
python deploy/scripts/export_infra_credentials.py --host 18.139.209.10 --key-file cert/im1.pem
```

## 当前 Compose 服务

| Service | 容器名 | 端口 | 状态 |
|---------|--------|------|:----:|
| postgres | inbound-postgres | 5432 | ✅ 启用 |
| redis | inbound-redis | 6379 | ✅ 启用 |
| minio | inbound-minio | 9000, 9001 | ✅ 启用 |
| rabbitmq | inbound-rabbitmq | 5672, 15672 | ✅ 启用 |
| langfuse | inbound-langfuse | 3000 | ✅ 启用 |
| gotenberg | inbound-gotenberg | 3002 | ✅ 启用 |
| core-api | inbound-core | 8080 | ⏳ 待 scaffold 后取消注释 |
| ai-api | inbound-ai | 8090 | ✅ 启用（服务器 `127.0.0.1:8090`） |

## 安全说明（开发环境）

- 默认密码仅用于本地 dev，**禁止**用于生产
- 仅 `nginx`（未来）应对公网；数据库/Redis/MinIO/RabbitMQ 不对公网暴露
- 生产环境使用 `.env` + 密钥管理，勿提交真实 Key

## 常用命令

```bash
# 查看日志
docker compose logs -f postgres

# 进入数据库
docker exec -it inbound-postgres psql -U inbound -d inbound_growth

# 停止并删除卷（重置数据库）
docker compose down -v
```

## 与应用联调（scaffold 后）

1. 取消 `docker-compose.yml` 中 `core-api` / `ai-api` 注释
2. 在 `inbound-core/`、`inbound-ai/` 编写 Dockerfile
3. `docker compose up -d --build`

环境变量完整清单见 [../docs/TECH_STACK_COMPONENTS.md](../docs/TECH_STACK_COMPONENTS.md) §4。

## EPIC-2 GEO 诊断 — ai-api 与 Worker 联调

### 服务器 ai-api（HTTP 探针 API）

```bash
# 推送最新 inbound-ai（含 /ai/diagnose、/ai/score、citation_parser、worker 代码）
python deploy/scripts/server_ai_deploy.py --host 18.139.209.10 --key-file cert/im1.pem
```

部署脚本会验通 `/health` 及 OpenAPI 中的 EPIC-2 路由。服务器 **默认不在容器内启 MQ worker**（`DIAGNOSE_WORKER_ENABLED` 未设 = false）。

### 混合联调（当前推荐，**不必改 compose**）

| 组件 | 跑在哪 | 说明 |
|------|--------|------|
| PG / Redis / RabbitMQ | 服务器（SSH 隧道） | 见 `docs/INFRA_ACCESS.md` §6 |
| `inbound-core` Java | **本机** `:8080` | 发 MQ、收 callback |
| `inbound-ai` HTTP | 服务器 `ai-api` `:8090` 或本机 uvicorn | Java `AI_SERVICE_BASE_URL` 指过去 |
| **diagnose worker** | **本机** uvicorn | `DIAGNOSE_WORKER_ENABLED=true` + `RABBITMQ_URL` 指隧道 |

本机 worker 示例：

```bash
# 隧道 + 凭证见 INFRA_ACCESS.local.md
cd inbound-ai
set DIAGNOSE_WORKER_ENABLED=true
set RABBITMQ_URL=amqp://inbound:<pass>@localhost:5672/
set CORE_CALLBACK_BASE_URL=http://localhost:8080
set GEMINI_API_KEY=<your-key>
uv run uvicorn app.main:app --reload --port 8090
```

E2E 冒烟：`python deploy/scripts/test_diagnostic_e2e.py`（Java :8080 + 本机 worker + MQ 隧道）。

### 生产同机部署（Java 上服务器 **之后**）

当 `docker-compose.prod.yml` 取消注释 `core-api` 且与 `ai-api` 同 `inbound-network` 时，在服务器 `deploy/.env` 为 **容器内 ai-api** 配置：

```bash
DIAGNOSE_WORKER_ENABLED=true
CORE_CALLBACK_BASE_URL=http://core-api:8080
```

- 容器间用 Compose 服务名 `core-api`，**不要**写 `127.0.0.1`（那是容器自身 loopback）
- Java 侧 `AI_SERVICE_BASE_URL=http://ai-api:8090`（同网）
- **混合联调阶段勿在服务器 compose 开 worker**，避免与本机 worker 双消费 `diag.grounded-api` 队列

完整变量见 `deploy/.env.example` 与 `docs/TECH_STACK_COMPONENTS.md` §4.2。
