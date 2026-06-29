# HANDOFF | 技术总监 → 运维

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 运维 |
| **日期** | 2026-06-27 |
| **优先级** | High |
| **关联** | ADR-20260627-09 · EPIC-2 M1 · `deploy/LOCAL_DOCKER.md` |

## 上下文

**当前状态**：

- 项目资源提供人已在 **Windows 本机** 完成：WSL2 Ubuntu、Docker Desktop（磁盘镜像 `D:\Dev\SDKs\Docker\wsl-data`）、`local_docker_bootstrap.ps1` **四容器全 healthy**（postgres/redis/rabbitmq/ai-api）。
- 仓库已提供 D 盘隔离方案：`docker-compose.local-d.yml`、bootstrap/cleanup/import 脚本、`LOCAL_DOCKER.md`。
- **尚未完成**：本机若依系统表导入、smoke 验通、`.env` Key 配置、关闭 SSH 隧道混用确认。
- 共享服务器 `18.139.209.10` **保留为 staging**；本机开发 **不再依赖隧道**（ADR-09）。

**相关文件**：

- `deploy/LOCAL_DOCKER.md` — 安装 / 日常 / 清理手册（A→E 级）
- `deploy/docker-compose.local-d.yml` — D 盘 bind mount + EPIC-2 最小 4 服务
- `deploy/.env.local.example` → 复制为 `deploy/.env`
- `deploy/scripts/local_docker_bootstrap.ps1`
- `deploy/scripts/import_ruoyi_pg_local.ps1`
- `deploy/scripts/local_docker_cleanup.ps1`
- `deploy/scripts/test_projects_api.py` / `test_ai_health.py` / `test_diagnostic_e2e.py`

**约束**：

- **勿**与 SSH 隧道混用（本地 Redis **6379 无密码**，不是远程 6380）。
- Windows PostgreSQL-16 服务保持 **停止/不自动启动**（5432 给 Docker）。
- Key 只写 `deploy/.env`，**勿提交 Git**。
- 只改 `deploy/`、`docs/agent-team/`；不改业务 Java/Vue 代码。

---

## 交付请求

**需要什么**：在本机 Docker 基础设施已 bootstrap 的前提下，**完成首次初始化 + smoke 验通 + 文档/记忆更新**，并输出「开发窗口启动说明」供资源提供人派活。

---

## 运维窗口 — 激活 Prompt（复制到新 Cursor 窗口首行）

```
角色：运维。必读 docs/agent-team/MEMORY.md 与 HANDOFFS/2026-06-27-tech-director-to-devops-local-docker-finish.md。
任务：完成本机 D 盘 Docker 首次初始化与 smoke；只改 deploy/ 与 docs/agent-team/。
```

---

## 操作步骤（按顺序）

### Step 0 — 前置检查

```powershell
# 确认无 SSH 隧道占用（5672/6380 不应是 ssh 进程）
Get-NetTCPConnection -LocalPort 5432,6379,5672,8090 -State Listen -ErrorAction SilentlyContinue

cd D:\Dev\Workspace\Commercial_Projects\2026_Inbound_Tourism_Acquisition_OS\deploy
docker compose -f docker-compose.yml -f docker-compose.local-d.yml ps
```

**期望**：4 容器均为 `healthy`。

若未运行：

```powershell
.\scripts\local_docker_bootstrap.ps1
```

---

### Step 1 — 配置 deploy/.env

```powershell
cd deploy
# 若尚无 .env：
copy .env.local.example .env
```

编辑 `deploy/.env`，至少确认：

```env
INBOUND_DOCKER_DATA=D:/Dev/SDKs/Docker/inbound-growth
GEMINI_API_KEY=<向资源提供人索取，勿写入 Git>
DIAGNOSE_MOCK_LLM=false
DIAGNOSE_WORKER_ENABLED=true
CORE_CALLBACK_BASE_URL=http://host.docker.internal:8080
```

若改了 Key 或 worker 相关项：

```powershell
docker compose -f docker-compose.yml -f docker-compose.local-d.yml up -d --build ai-api
```

---

### Step 2 — 导入若依系统表（仅首次）

```powershell
.\scripts\import_ruoyi_pg_local.ps1
```

**期望输出**：`sys_user` 存在；public 表 60+。

---

### Step 3 — 基础设施 smoke

```powershell
curl http://localhost:8090/health
docker exec inbound-postgres pg_isready -U inbound -d inbound_growth
docker exec inbound-postgres psql -U inbound -d inbound_growth -tAc "SELECT count(*) FROM pg_tables WHERE schemaname='public';"
```

**期望**：health 200；pg_isready OK；表数量 ≥ 60。

---

### Step 4 — 应用层 smoke（需 Java 已启动时）

资源提供人或开发窗口先起 Java（见下方「开发窗口说明」），然后：

```powershell
cd D:\Dev\Workspace\Commercial_Projects\2026_Inbound_Tourism_Acquisition_OS
python deploy/scripts/test_ai_health.py
python deploy/scripts/test_projects_api.py
python deploy/scripts/test_diagnostic_e2e.py
```

**期望**：projects API 通过；E2E 创建诊断 → SUCCESS + geo_score（若 Key 无效可暂设 `DIAGNOSE_MOCK_LLM=true` 并注明遗留）。

---

### Step 5 — 更新共享记忆

- [ ] 更新 `docs/agent-team/MEMORY.md` 运维章节：本机 Docker ✅、D 盘路径、隧道方案降为备选
- [ ] 本 HANDOFF 填 **Done**
- [ ] 确认 `deploy/LOCAL_DOCKER.md` 与现状一致（无需大改则跳过）

---

## 验收标准

- [ ] `docker compose ps` — postgres / redis / rabbitmq / ai-api 全 **healthy**
- [ ] `import_ruoyi_pg_local.ps1` — `sys_user` 存在
- [ ] `curl localhost:8090/health` — 200
- [ ] `deploy/.env` 存在且含 `INBOUND_DOCKER_DATA`（Key 不入 Git）
- [ ] 确认 **无 SSH 隧道** 与本地 Docker 同时联调
- [ ] MEMORY.md 已更新

---

## 开发窗口启动说明（运维完成后交给资源提供人）

资源提供人可开 **2 个开发 Cursor 窗口**（或 1 开发 + 1 自测）：

### 窗口 A — Java 后端

**激活 Prompt**：

```
角色：开发。必读 docs/agent-team/MEMORY.md。本机 Docker 已就绪（见 ADR-09），不要开 SSH 隧道。
任务：启动 inbound-core 并联调本地 PG/Redis/MQ/ai-api。
```

**终端命令**：

```powershell
$env:INBOUND_PG_PASSWORD = "inbound_dev_pass"
$env:INBOUND_REDIS_PASSWORD = ""
$env:INBOUND_RABBITMQ_PASSWORD = "inbound_dev_pass"
$env:AI_SERVICE_BASE_URL = "http://localhost:8090"
$env:AI_SERVICE_INTERNAL_TOKEN = "dev_internal_token_change_me"

cd D:\Dev\Workspace\Commercial_Projects\2026_Inbound_Tourism_Acquisition_OS\inbound-core
mvn -pl ruoyi-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

**验通**：日志 `Started DromaraApplication`；8080 可访问。

---

### 窗口 B — Admin 前端

**激活 Prompt**：

```
角色：开发。必读 MEMORY.md。Java 已在 :8080；Admin 联调本地 API。
任务：pnpm dev + 浏览器走查 登录/项目/GEO诊断列表→详情。
```

**终端命令**：

```powershell
cd D:\Dev\Workspace\Commercial_Projects\2026_Inbound_Tourism_Acquisition_OS\inbound-admin
pnpm dev
```

**验通**：http://localhost:5173 登录 `admin/admin123`；项目列表、GEO 诊断列表→详情可打开。

---

### 窗口 C（可选）— 技术总监 / 验收

跑 smoke：

```powershell
python deploy/scripts/test_projects_api.py
python deploy/scripts/test_diagnostic_e2e.py
```

---

## 清理（需要重置时）

```powershell
cd deploy
.\scripts\local_docker_cleanup.ps1 -Level B
.\scripts\local_docker_bootstrap.ps1
.\scripts\import_ruoyi_pg_local.ps1
```

完整分级见 `deploy/LOCAL_DOCKER.md` §5。

---

## 质量 / 证据

**必须提供**：

- `docker compose ps` 截图或文本（4 healthy）
- `import_ruoyi_pg_local.ps1` 末尾 check 输出
- `curl localhost:8090/health` 一行输出
- （若跑 E2E）`test_diagnostic_e2e.py` 成功日志

**交给下一棒**：开发窗口 A/B 启动 Java + Admin；技术总监 M1 本地 E2E 签核。

---

## Done（由 To 角色填写）

- **完成时间**：2026-06-27 16:00 CST
- **结果摘要**：
  - 4 容器 **healthy**：postgres / redis / rabbitmq / ai-api
  - `import_ruoyi_pg_local.ps1` → `sys_user` ✅，public **64 表**
  - `deploy/.env` 已配置（`INBOUND_DOCKER_DATA`、worker、`CORE_CALLBACK_BASE_URL`、GEMINI Key）
  - Smoke：`curl.exe localhost:8090/health` → 200；`test_ai_health.py` → litellm=ready
  - 端口监听均为 Docker（`com.docker.backend` / `wslrelay`），**无 SSH 隧道混用**
  - 修复 `import_ruoyi_pg_local.ps1`（Join-Path 数组语法 + 编码）
- **遗留**：
  - `test_projects_api.py` / `test_diagnostic_e2e.py` 待 **开发窗口 A** 起 Java `:8080` 后执行
  - PowerShell 下建议用 `curl.exe` 而非 `curl`（避免 Invoke-WebRequest 挂起）
