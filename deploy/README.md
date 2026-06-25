# 部署与本地开发

> **连接与密码**：见 [../docs/INFRA_ACCESS.md](../docs/INFRA_ACCESS.md)（结构说明）及本机 `../docs/INFRA_ACCESS.local.md`（含服务器凭证，gitignore）。

## 一键启动（MVP 基础设施）

### 本地开发

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
| ai-api | inbound-ai | 8090 | ⏳ 待 scaffold 后取消注释 |

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
