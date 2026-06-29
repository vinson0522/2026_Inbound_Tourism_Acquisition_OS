# 本地 Docker 开发（D 盘隔离）— 安装与清理手册

> **目标**：基础设施全在 Docker；**镜像与数据尽量不占 C 盘**；一键可删干净。  
> **适用**：Windows 10/11 + Docker Desktop (WSL2)。  
> **项目代码**仍在 `D:\Dev\Workspace\...`；**Docker 运行时与数据**在 `D:\Dev\SDKs\Docker\`。

---

## 1. 目录规划（全部在 D 盘）

```
D:\Dev\SDKs\Docker\
├── DockerDesktop\              # 可选：Docker Desktop 安装目录（安装器指定）
├── wsl-data\                   # Docker Desktop「磁盘镜像」位置（Settings 里改）
└── inbound-growth\             # 本项目容器数据（bind mount，删文件夹 = 删库）
    ├── postgres\
    ├── redis\
    └── rabbitmq\
```

| 内容 | 位置 | 怎么删 |
|------|------|--------|
| PG / Redis / MQ 数据 | `D:\Dev\SDKs\Docker\inbound-growth\` | 删整个文件夹 |
| Docker 镜像/层 | `D:\Dev\SDKs\Docker\wsl-data\` | Docker Desktop 清盘 / 卸载 |
| 项目 compose / 脚本 | 仓库 `deploy/` | `git` 管理，不删 |

---

## 2. 安装（最干净流程）

### 2.1 前置：确认端口与 Windows PG

你的 **PostgreSQL-16 服务已停止且不自动重启** — 5432 可给 Docker 用，无需再改。

安装前关闭 **SSH 隧道**（否则 5672/6380 被占用）：

```powershell
# 查看占用
Get-NetTCPConnection -LocalPort 5432,6379,5672,8090 -State Listen -ErrorAction SilentlyContinue
```

### 2.2 安装 WSL2（一次性，需管理员 + 重启）

**管理员 PowerShell**：

```powershell
wsl --install
```

重启后确认：

```powershell
wsl --status
wsl -l -v
```

### 2.3 安装 Docker Desktop 到 D 盘

1. 下载 [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/)
2. 安装时若可选手动路径，选：`D:\Dev\SDKs\Docker\DockerDesktop`
3. 若安装器不支持改路径：装完后在 **Settings → Resources → Advanced → Disk image location** 设为：
   ```
   D:\Dev\SDKs\Docker\wsl-data
   ```
   > **重要**：最好在第一次 `docker pull` 之前就改，避免 C 盘先占几 GB。

4. Settings → General → 勾选 **Use the WSL 2 based engine**

验证：

```powershell
docker --version
docker compose version
docker run --rm hello-world
```

### 2.4 创建数据目录

```powershell
New-Item -ItemType Directory -Force -Path @(
  "D:\Dev\SDKs\Docker\wsl-data",
  "D:\Dev\SDKs\Docker\inbound-growth\postgres",
  "D:\Dev\SDKs\Docker\inbound-growth\redis",
  "D:\Dev\SDKs\Docker\inbound-growth\rabbitmq"
)
```

### 2.5 配置项目环境变量

```powershell
cd D:\Dev\Workspace\Commercial_Projects\2026_Inbound_Tourism_Acquisition_OS\deploy
copy .env.local.example .env
# 编辑 .env：填入 GEMINI_API_KEY 等
```

### 2.6 启动最小基础设施（EPIC-2）

```powershell
cd deploy
docker compose -f docker-compose.yml -f docker-compose.local-d.yml up -d postgres redis rabbitmq ai-api
docker compose -f docker-compose.yml -f docker-compose.local-d.yml ps
```

或使用一键脚本：

```powershell
.\scripts\local_docker_bootstrap.ps1
```

等待 `healthy` 后验通：

```powershell
curl.exe http://localhost:8090/health
docker exec inbound-postgres pg_isready -U inbound -d inbound_growth
```

### 2.7 导入若依系统表（仅首次）

```powershell
.\scripts\import_ruoyi_pg_local.ps1
```

### 2.8 本机起 Java + Admin（不进 Docker）

**PowerShell**（本地 compose 默认值，无需隧道变量）：

```powershell
$env:INBOUND_PG_PASSWORD = "inbound_dev_pass"
$env:INBOUND_REDIS_PASSWORD = ""
$env:INBOUND_RABBITMQ_PASSWORD = "inbound_dev_pass"
$env:AI_SERVICE_BASE_URL = "http://localhost:8090"
$env:AI_SERVICE_INTERNAL_TOKEN = "dev_internal_token_change_me"
```

```powershell
# 终端 1
cd inbound-core
mvn -pl ruoyi-admin spring-boot:run -Dspring-boot.run.profiles=dev

# 终端 2
cd inbound-admin
pnpm dev
```

冒烟：

```powershell
python deploy/scripts/test_projects_api.py
python deploy/scripts/test_diagnostic_e2e.py
```

---

## 3. 日常命令

```powershell
cd deploy

# 启动
docker compose -f docker-compose.yml -f docker-compose.local-d.yml up -d postgres redis rabbitmq ai-api

# 停止（保留 D 盘数据）
docker compose -f docker-compose.yml -f docker-compose.local-d.yml stop

# 看日志
docker compose -f docker-compose.yml -f docker-compose.local-d.yml logs -f ai-api

# 重建 ai-api（改代码后）
docker compose -f docker-compose.yml -f docker-compose.local-d.yml up -d --build ai-api
```

---

## 4. 连接信息（本地 dev）

| 服务 | 地址 | 账号 / 密码 |
|------|------|-------------|
| PostgreSQL | `localhost:5432` | `inbound` / `inbound_dev_pass` |
| Redis | `localhost:6379` | 无密码 |
| RabbitMQ | `localhost:5672` | `inbound` / `inbound_dev_pass` |
| RabbitMQ UI | http://localhost:15672 | 同上 |
| ai-api | http://localhost:8090 | token: `dev_internal_token_change_me` |

### FR-106 PDF 导出（Gotenberg，可选）

M2 默认 **DOCX 不依赖 Gotenberg**。PDF 需启用 Gotenberg：

```powershell
cd deploy
docker compose -f docker-compose.yml -f docker-compose.local-d.yml --profile full up -d gotenberg
```

Java 环境变量：

```powershell
$env:GOTENBERG_BASE_URL = "http://localhost:3002"
```

验通：`python deploy/scripts/test_diagnostic_report_export.py`（DOCX）；PDF 在 Admin 详情页点「导出 PDF」。

---

## 5. 清理手册

按「污染程度」从浅到深，选一级执行即可。

### 级别 A — 仅停容器（数据全保留）

```powershell
cd deploy
docker compose -f docker-compose.yml -f docker-compose.local-d.yml stop
```

**效果**：端口释放；`D:\Dev\SDKs\Docker\inbound-growth\` 数据仍在。

---

### 级别 B — 重置本项目数据库（推荐日常「洗干净重来」）

```powershell
cd deploy
docker compose -f docker-compose.yml -f docker-compose.local-d.yml down

Remove-Item -Recurse -Force "D:\Dev\SDKs\Docker\inbound-growth"
```

然后重新 bootstrap：

```powershell
.\scripts\local_docker_bootstrap.ps1
.\scripts\import_ruoyi_pg_local.ps1
```

**效果**：PG/Redis/MQ 数据全删；Docker 镜像仍在 D 盘 wsl-data；C 盘几乎无影响。

或使用脚本：

```powershell
.\scripts\local_docker_cleanup.ps1 -Level B
```

---

### 级别 C — 删除本项目相关镜像

在级别 B 之后：

```powershell
docker image ls "inbound*" 
docker image ls "deploy-*"
docker rmi $(docker images -q "deploy-ai-api") 2>$null
docker rmi pgvector/pgvector:pg16 redis:7-alpine rabbitmq:3-management-alpine 2>$null
docker image prune -f
```

或：

```powershell
.\scripts\local_docker_cleanup.ps1 -Level C
```

**效果**：释放 D 盘 wsl-data 里的镜像空间；下次 `up` 会重新 pull/build。

---

### 级别 D — 清空 Docker Desktop 全部数据（所有项目镜像/容器）

1. 退出 Docker Desktop  
2. **Docker Desktop → Troubleshoot → Clean / Purge data**  
3. 或 PowerShell（需 Docker 已停）：

```powershell
wsl --shutdown
# 若曾把 disk image 设在 D:\Dev\SDKs\Docker\wsl-data，可手动删该目录
Remove-Item -Recurse -Force "D:\Dev\SDKs\Docker\wsl-data" -ErrorAction SilentlyContinue
```

**效果**：Docker 恢复「刚安装」状态。

---

### 级别 E — 完全卸载 Docker（零 Docker 残留）

1. 执行 **级别 D**  
2. **设置 → 应用 → Docker Desktop → 卸载**  
3. （可选）删除安装目录：

```powershell
Remove-Item -Recurse -Force "D:\Dev\SDKs\Docker\DockerDesktop" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "D:\Dev\SDKs\Docker" -ErrorAction SilentlyContinue
```

4. （可选）注销 WSL 发行版：

```powershell
wsl --unregister docker-desktop
wsl --unregister docker-desktop-data
```

**效果**：本机无 Docker；Windows 自带 PostgreSQL-16 不受影响（你已是手动启动）。

---

## 6. 不会动到的东西

| 组件 | 说明 |
|------|------|
| Windows PostgreSQL-16 | 独立安装，与 Docker PG 无关；保持 Disabled/手动即可 |
| 远程服务器 `18.139.209.10` | 本地 Docker 与其完全隔离 |
| 项目源码 | 在 Workspace，清理 Docker 不删代码 |
| `deploy/.env` | 含 Key，已在 gitignore；清理时自行保留或删 |

---

## 7. 常见问题

**Q：C 盘还是涨了？**  
A：检查 Docker Desktop → Settings → Disk image location 是否在 `D:\Dev\SDKs\Docker\wsl-data`；改完后执行级别 C/D 清旧 VHDX。

**Q：5432 被占用？**  
A：`Get-Service PostgreSQL*` 确认已 Stop；或临时改 compose 端口映射为 `5433:5432` 并设 `INBOUND_PG_PORT=5433`。

**Q：ai-api worker callback 失败？**  
A：确认 Java 在宿主机 `:8080`，且 `.env` 中 `CORE_CALLBACK_BASE_URL=http://host.docker.internal:8080`。

**Q：和 SSH 隧道混用？**  
A：不要混用。本地 Docker 模式下关掉所有隧道，Redis 用 **6379**（不是 6380）。

---

## 8. 相关文件

| 文件 | 用途 |
|------|------|
| `docker-compose.local-d.yml` | D 盘 bind mount + 最小服务集 |
| `.env.local.example` | 本地 `.env` 模板 |
| `scripts/local_docker_bootstrap.ps1` | 一键启动 |
| `scripts/local_docker_cleanup.ps1` | 分级清理 |
| `scripts/import_ruoyi_pg_local.ps1` | 本机导入若依表 |

---

*Last updated: 2026-06-27*
