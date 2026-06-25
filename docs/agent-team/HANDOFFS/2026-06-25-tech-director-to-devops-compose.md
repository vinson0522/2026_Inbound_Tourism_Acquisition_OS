# HANDOFF | 技术总监 → 运维

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 运维 |
| **日期** | 2026-06-25 |
| **优先级** | High |
| **关联** | EPIC-1 / ADR-20260623-02 / ADR-20260625-04 |

## 上下文

**当前状态**：`deploy/docker-compose.yml` 已定义 6 个 MVP 基础设施服务（PostgreSQL、Redis、MinIO、RabbitMQ、Langfuse、Gotenberg）；`core-api` / `ai-api` 仍为注释占位。开发侧若依切 PG（EPIC-1 Sprint 任务 #2）**依赖本任务产出**——须先确认 PG 可连、28 张业务表与 demo 种子已初始化。

**相关文件**：
- `deploy/docker-compose.yml` — 本地 compose 主文件
- `deploy/docker-compose.prod.yml` — 生产变体（本任务以本地 dev 为主，prod 仅 spot-check 配置一致性）
- `deploy/README.md` — 启动命令与服务端口表
- `database/ddl/001_schema.sql` — PG init 挂载（28 表）
- `database/ddl/002_seed_demo.sql` — demo 种子
- `docs/TECH_STACK_COMPONENTS.md` §4–§5 — 端口与环境变量基线
- `docs/INFRA_ACCESS.local.md` — 本机/服务器凭证（勿提交 Git）

**约束**：
- 默认密码仅 dev 环境；**不要**改 compose 内联密码除非发现冲突并同步 `INFRA_ACCESS.local.md`
- 不取消注释 `core-api` / `ai-api`（属开发后续任务）
- Windows 开发机注意 5432 / 6379 / 3000 端口占用；若冲突，记录实际映射而非静默改 compose（改端口需 HANDOFF 回技术总监）
- Langfuse 与业务表共用 `inbound_growth` 库（见 ADR-20260625-04）；验证时注意 Langfuse 自建表与 `001_schema.sql` 共存

## 交付请求

**需要什么**：在本机执行 `docker compose up -d`，确认全部启用服务 **healthy / running**，并附可复现的验证证据（命令输出或脚本结果）。

**验收标准**：
- [ ] `cd deploy && docker compose up -d` 无报错退出
- [ ] `docker compose ps` 显示 6 服务均为 `running`；带 healthcheck 的 5 个服务状态为 `healthy`（postgres、redis、minio、rabbitmq；langfuse 依赖 postgres healthy 后启动）
- [ ] **PostgreSQL**：`docker exec inbound-postgres psql -U inbound -d inbound_growth -c "\dt"` 可见 **28** 张业务表；`\dx` 含 `vector` 扩展；抽样查询 demo 种子（如 `tenant` / `project` 表有记录，以 `002_seed_demo.sql` 为准）
- [ ] **Redis**：`docker exec inbound-redis redis-cli ping` 返回 `PONG`
- [ ] **MinIO**：API `http://127.0.0.1:9000/minio/health/live` 返回 200；Console `9001` 可访问（浏览器或 curl）
- [ ] **RabbitMQ**：`docker exec inbound-rabbitmq rabbitmq-diagnostics -q ping` 成功；管理台 `http://127.0.0.1:15672` 可登录（默认 `inbound` / `inbound_dev_pass`）
- [ ] **Langfuse**：`http://127.0.0.1:3000` 可访问（HTTP 200 或登录页）
- [ ] **Gotenberg**：`http://127.0.0.1:3002/health` 返回 healthy
- [ ] 更新 `deploy/README.md`「当前 Compose 服务」表（若实测状态与文档不一致）
- [ ] 更新 `docs/agent-team/MEMORY.md` → **运维** 章节 + **全局状态 → 基础设施** 一行结论

## 质量 / 证据

**必须提供**：
- `docker compose ps` 完整输出（贴 HANDOFF Done 段或 gist）
- PG 表数量验证命令与输出（`\dt` 行数或 `SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'`）
- 至少 1 条 demo 种子抽样查询结果
- Redis ping、RabbitMQ ping、Gotenberg health 各 1 行输出
- 若失败：失败服务名、`docker compose logs --tail=50 <service>` 摘要、是否端口冲突

**交给下一棒**：
- 完成后 **开发** 接手 EPIC-1 任务 #2（若依 `application-dev.yml` 切 PG，`jdbc:postgresql://127.0.0.1:5432/inbound_growth`）
- 在 `MEMORY.md` **阻塞项** 若 PG/端口不可用则标 Critical 并 @ 技术总监

---

> 完成后：运维在本文件末尾追加 **Done** 段，并更新 `MEMORY.md`。

## Done（由 To 角色填写）

- **完成时间**：2026-06-25 21:56 CST
- **结果摘要**：
  - **本机**：`docker` / WSL 均未安装，无法在 Windows 开发机执行本地 `docker compose up -d`（见 MEMORY「运维 → 本地」）
  - **共享服务器 `18.139.209.10`**：6 容器 Up；postgres / redis / minio / rabbitmq healthcheck **healthy**；Langfuse HTTP 200；Gotenberg HTTP 200；PG 28 业务表 + `vector` 扩展；公网 5432 CLOSED
  - 验证命令：`python deploy/scripts/server_infra_verify.py --host 18.139.209.10 --key-file cert/im1.pem`
  - 已更新 `MEMORY.md` 运维章节；已发 [开发 PG HANDOFF](2026-06-25-ops-to-dev-pg-ruoyi.md)
- **遗留**：
  - 本机需安装 Docker Desktop 后补跑本地 compose 验收
  - PG 中无 `sys_user`（若依系统表待开发导入）
  - `server_infra_verify.py` 中 MinIO bucket 检查脚本需修复（mc 子命令兼容性）
