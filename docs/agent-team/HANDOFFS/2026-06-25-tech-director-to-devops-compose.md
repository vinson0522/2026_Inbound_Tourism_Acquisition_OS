# HANDOFF | 技术总监 → 运维

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 运维 |
| **日期** | 2026-06-25 |
| **优先级** | High → **已关闭（服务器路径）** |
| **关联** | EPIC-1 / ADR-20260623-02 / ADR-20260625-04 |

## 上下文

**当前状态**：`deploy/docker-compose.yml` 定义 6 个 MVP 基础设施服务。共享服务器 `18.139.209.10` 已通过 `docker-compose.prod.yml` 验收全绿；本机 Windows 开发环境 **未安装 Docker**，本地 compose 验收降级为 P2。

**相关文件**：
- `deploy/docker-compose.yml` — 本地 compose 主文件
- `deploy/docker-compose.prod.yml` — 生产/共享服务器
- `deploy/scripts/server_infra_verify.py` — 远程验收脚本
- `database/ddl/001_schema.sql` — 28 张业务表
- `database/ddl/002_seed_demo.sql` — demo 种子
- `docs/INFRA_ACCESS.local.md` — 凭证（勿提交 Git）

**约束**：
- 默认密码仅 dev 环境；改 compose 密码须同步 `INFRA_ACCESS.local.md`
- 不取消注释 `core-api` / `ai-api`
- Langfuse 与业务表同库策略见 ADR-20260625-04（服务器 Langfuse 用独立库 `langfuse`）

---

## EPIC-1 任务 #1（运维）

| 项 | 内容 |
|----|------|
| **目标** | 基础设施 compose 全绿，PG 可连、28 业务表 + demo 就绪 |
| **验收环境** | **优先**共享服务器；本机 Docker 为加分项 |
| **阻塞下游** | 开发 PG 联调、若依系统表导入 |

## 交付请求

**需要什么**：确认全部启用服务 **healthy / running**，附可复现验证证据。

**验收标准（服务器路径 — 已满足 ✅）**：
- [x] 6 服务 `running`；postgres / redis / minio / rabbitmq `healthy`
- [x] PG：`inbound_growth` 含 28 业务表 + `vector` 扩展；demo 种子存在
- [x] PG：若依系统表已导入（`sys_user` 存在，public 共 **64** 表）
- [x] Redis ping OK（6380，需密码）
- [x] RabbitMQ diagnostics ping OK
- [x] Langfuse HTTP 200（3100）；Gotenberg HTTP 200（3002）
- [x] 公网 5432 CLOSED

**验收标准（本机路径 — 待 P2 ⏳）**：
- [ ] 安装 Docker Desktop 后 `cd deploy && docker compose up -d`
- [ ] 本地 6 服务 healthy（端口见 `deploy/README.md`）

## 质量 / 证据

**已提供（2026-06-25）**：
- `python deploy/scripts/server_infra_verify.py --host 18.139.209.10 --key-file cert/im1.pem`
- `python deploy/scripts/import_ruoyi_pg.py --check-only` → `sys_user|64`

**交给下一棒**：
- 开发接手 EPIC-1 任务 #2（[ops→dev PG 联调](2026-06-25-ops-to-dev-pg-ruoyi.md)）
- 本机 Docker 不阻塞；开发用 SSH 隧道（ADR-20260625-05）

---

## Done（运维填写 + 技术总监复核 2026-06-25）

- **完成时间**：2026-06-25 21:56 CST（服务器）；若依表 2026-06-25 晚复核
- **结果摘要**：
  - **共享服务器**：6 容器 Up；核心 healthcheck 全绿；PG 64 表（28 业务 + 若依）；`sys_user` ✅
  - **本机**：Docker 未安装，本地 compose 未跑（B-01 低优先级）
  - 已发 [开发 PG HANDOFF](2026-06-25-ops-to-dev-pg-ruoyi.md)
- **遗留**：
  - 本机 Docker Desktop 安装后补跑本地 compose（P2）
  - `server_infra_verify.py` MinIO bucket 检查脚本 mc 兼容性（非阻塞）
