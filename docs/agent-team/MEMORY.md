# Agent 团队共享记忆（Single Source of Truth）

> **所有 Cursor Agent 窗口在开始任务前必读，结束任务后必更新。**  
> 各窗口会话互不共享上下文；本文件 + `DECISIONS.md` + `HANDOFFS/` 即「共享记忆」。

| 字段 | 值 |
|------|-----|
| **最后更新** | 2026-06-25 |
| **更新角色** | 开发 |
| **当前 EPIC 焦点** | EPIC-1 Sprint-1（基础设施验证 → 若依 PG → Admin 线框） |

---

## 全局状态

| 项 | 状态 |
|----|------|
| 底座 | RuoYi-Vue-Plus 5.6.2 → `inbound-core`；plus-ui → `inbound-admin` |
| 数据库规划 | PostgreSQL 16 + pgvector（`database/ddl/001_schema.sql`） |
| 若依默认库 | `application-dev.yml` 已切 PG（localhost:5432/inbound_growth）；**若依系统表未导入**（无 `sys_user`） |
| 基础设施 | 共享服务器 **全绿** ✅；本机 Docker **未安装**（本地 compose 待验证） |
| AI 服务 | `inbound-ai` 待 scaffold |
| 落地页/探针/门户 | 目录骨架就绪，待开发 |

---

## 技术总监

- **排期**：EPIC-1 → EPIC-10 → EPIC-2 …（见 `AGENTS.md` §17）
- **EPIC-1 Sprint-1 拆分**（3 条，依赖顺序 ①→②→③ 部分并行）：
  1. **运维** — 验证 `docker compose up` 全绿 + PG 28 表/种子 → [HANDOFF](HANDOFFS/2026-06-25-tech-director-to-devops-compose.md)
  2. **开发** — 若依切 PG（ADR-20260625-04）+ 本地启动无报错；Flyway/系统表与 `001_schema.sql` 共存验证
  3. **UI** — ~~`docs/design/` 品牌 token + 工作台/GEO 诊断列表线框~~ ✅；待补：项目列表 + FR-001 创建流
- **已定案**：单库 PG（ADR-20260625-04）；RuoYi 系统表 + 业务表同库，不做 MySQL 过渡
- **待办**：~~运维 compose 验收~~ ✅ 服务器已验；接收 [HANDOFF PG 联调](HANDOFFS/2026-06-25-ops-to-dev-pg-ruoyi.md)
- **风险**：多 Agent 并行改同一模块 → 必须写 HANDOFF；Langfuse 与业务表同库需开发启动时再验迁移

---

## 运维

- **负责目录**：`deploy/`、`cert/`、`docs/INFRA_ACCESS.local.md`
- **验证时间**：2026-06-25 21:56 CST

### 本地（本机 Cursor 环境）

| 项 | 结果 |
|----|------|
| `docker compose up -d` | ❌ **未执行** — 本机未安装 Docker Desktop / WSL |
| 待办 | 安装 [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/) 后于 `deploy/` 执行 `docker compose up -d` |

**本地 compose 预期端口与账号**（`deploy/docker-compose.yml`）：

| 服务 | 宿主机端口 | 账号 / 密码 | DB/备注 |
|------|------------|-------------|---------|
| PostgreSQL | `5432` | `inbound` / `inbound_dev_pass` | `inbound_growth`（init 含 28 业务表 + demo） |
| Redis | `6379` | 无密码 | AOF 持久化 |
| MinIO | `9000` / `9001` | `minioadmin` / `minioadmin_dev` | API / Console |
| RabbitMQ | `5672` / `15672` | `inbound` / `inbound_dev_pass` | 管理台 15672 |
| Langfuse | `3000` | — | 依赖 PG |
| Gotenberg | `3002` | — | PDF |

### 共享服务器 `18.139.209.10`（已验证 ✅）

`docker-compose.prod.yml` 全 6 容器 Up；核心 4 项 healthcheck **全绿**：

| 服务 | 绑定 | 健康 | 备注 |
|------|------|:----:|------|
| PostgreSQL | `127.0.0.1:5432` | ✅ | 28 表、`pgvector` 已装；**无 `sys_user`（若依表未导入）** |
| Redis | `127.0.0.1:6380` | ✅ | 需密码（见 `INFRA_ACCESS.local.md` §4） |
| MinIO | `127.0.0.1:9000/9001` | ✅ | health/live → 200 |
| RabbitMQ | `127.0.0.1:5672/15672` | ✅ | diagnostics ping OK |
| Langfuse | `127.0.0.1:3100` | ✅ | HTTP 200 |
| Gotenberg | `127.0.0.1:3002` | ✅ | HTTP 200 |

- 公网 5432：**CLOSED**（符合安全要求）
- 凭证与连接串：`docs/INFRA_ACCESS.local.md` §3–§5
- 远程验证命令：`python deploy/scripts/server_infra_verify.py --host 18.139.209.10 --key-file cert/im1.pem`

- **HANDOFF**：→ 开发 `docs/agent-team/HANDOFFS/2026-06-25-ops-to-dev-pg-ruoyi.md`

---

## 开发

- **负责目录**：`inbound-core/`、`inbound-ai/`、`database/`、`inbound-probe-extension/`
- **分层铁律**：Java 管事务，Python 管 AI（`CLAUDE.md` §5）
- **已完成（2026-06-25）**：
  - `application-dev.yml` master 数据源 → Docker PG `jdbc:postgresql://localhost:5432/inbound_growth`（user `inbound`）
  - Redis 对齐本地 compose（无密码）；`snail-job.enabled: false`（SnailJob 仍 MySQL，MVP 跳过）
  - `ruoyi-admin/pom.xml` 启用 `postgresql` JDBC，注释 `mysql-connector-j`
- **待办**：
  - 本机安装 Docker 后 `cd deploy && docker compose up -d`
  - 导入若依 PG 系统表：`inbound-core/script/sql/postgres/postgres_ry_vue_5.X.sql`（+ workflow/job 按需）
  - 验收 `./mvnw -pl ruoyi-admin spring-boot:run` 连库成功
  - 连共享服务器时用 SSH 隧道 + `INFRA_ACCESS.local.md` §6 改 Redis 6380/密码
- **HANDOFF 进度**：[ops→dev PG 联调](HANDOFFS/2026-06-25-ops-to-dev-pg-ruoyi.md) — 配置项 ✅；系统表导入与启动验通 ⏳

---

## UI 设计

- **负责目录**：`inbound-admin/src/`、`inbound-landing/`、`inbound-portal/`、`docs/design/`
- **底座 UI**：plus-ui / Element Plus，**不重造 Admin 壳**
- **已完成（2026-06-25）**：
  - `docs/design/README.md` — 设计系统索引、§6.1 路由映射、实现入口
  - `docs/design/tokens.md` — 品牌色 `#1677A0`、语义色、字体、间距、GEO 分数色阶
  - `docs/design/wireframes/dashboard.md` — 工作台（KPI/任务/预警/建议/漏斗/最近诊断）
  - `docs/design/wireframes/diagnostics-list.md` — GEO 诊断任务列表 + 新建抽屉（FR-103）
  - HANDOFF → 开发：[2026-06-25-ui-to-developer-admin-pages.md](HANDOFFS/2026-06-25-ui-to-developer-admin-pages.md)
- **待办**：客户项目列表 + FR-001 创建项目线框；诊断详情/报告页（EPIC-2）；落地页视觉（EPIC-6）

---

## 阻塞项

| ID | 描述 | 负责人 | 状态 |
|----|------|--------|------|
| — | （暂无） | — | — |

---

## 下一步（跨角色）

1. ~~**运维**：compose 验收~~ ✅ 服务器全绿；本机待装 Docker
2. **开发**（进行中）：若依 PG 配置已切 ✅；待 Docker/系统表导入 + `./mvnw spring-boot:run` 验通（[HANDOFF](HANDOFFS/2026-06-25-ops-to-dev-pg-ruoyi.md)）
3. ~~**UI**：建 `docs/design/` + 工作台/GEO 诊断列表线框~~ ✅ → 开发按 [UI HANDOFF](HANDOFFS/2026-06-25-ui-to-developer-admin-pages.md) 实现 Admin 页
4. **UI**（下一棒）：客户项目列表 + FR-001 创建项目线框
5. **技术总监**：开发启动验通后拆 EPIC-1 Story 2（登录→建项目→列表）

---

## 更新日志（最近 5 条）

| 日期 | 角色 | 摘要 |
|------|------|------|
| 2026-06-25 | 开发 | application-dev.yml 切 Docker PG + pom postgresql 驱动；Redis/SnailJob 对齐；待系统表导入与启动验通 |
| 2026-06-25 | 运维 | 服务器 PG/Redis/MinIO/RabbitMQ 全绿；本机无 Docker；HANDOFF 开发 PG 联调 |
| 2026-06-25 | 技术总监 | EPIC-1 Sprint-1 三路拆分；ADR-04 单库 PG；发运维 compose HANDOFF |
| 2026-06-25 | 技术总监 | 初始化共享记忆；建立四角色分工与 HANDOFF 机制 |
