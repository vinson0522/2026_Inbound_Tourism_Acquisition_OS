# Agent 团队共享记忆（Single Source of Truth）

> **所有 Cursor Agent 窗口在开始任务前必读，结束任务后必更新。**  
> 各窗口会话互不共享上下文；本文件 + `DECISIONS.md` + `HANDOFFS/` 即「共享记忆」。

| 字段 | 值 |
|------|-----|
| **最后更新** | 2026-06-25 |
| **更新角色** | 技术总监 |
| **当前 EPIC 焦点** | EPIC-1 Sprint-1 收尾 → Story 2（登录→建项目→列表） |

---

## 全局状态

| 项 | 状态 |
|----|------|
| 底座 | RuoYi-Vue-Plus 5.6.2 → `inbound-core`；plus-ui → `inbound-admin` |
| 数据库规划 | PostgreSQL 16 + pgvector（`database/ddl/001_schema.sql`） |
| 若依默认库 | PG 配置 ✅；**服务器已导入若依系统表**（`sys_user` 存在，public 共 64 表） |
| 基础设施 | 共享服务器 **全绿** ✅；本机 Docker **未安装**（可用 SSH 隧道联调） |
| AI 服务 | `inbound-ai` 待 scaffold |
| 落地页/探针/门户 | 目录骨架就绪，待开发 |

---

## 技术总监

- **Sprint-1 进度**（2026-06-25 复核）：

  | 任务 | 角色 | 状态 |
  |------|------|:----:|
  | 服务器 compose 全绿 | 运维 | ✅ |
  | 若依 PG 配置 + 系统表 | 开发 | ✅ 配置 + 服务器已导入 |
  | **后端启动验通** | 开发 | ⏳ **当前瓶颈** |
  | 设计 token + 工作台/GEO 线框 | UI | ✅ |
  | Admin 页实现 | 开发 | ⏳ 可 mock 并行 |
  | 项目列表 + FR-001 线框 | UI | ⏳ |

- **关键路径**：开 SSH 隧道 → 设 `INBOUND_*` 环境变量 → `mvn spring-boot:run` → Admin 登录 → 拆 Story 2
- **本机无 Docker**：不阻塞；隧道连 `18.139.209.10` 即可；Docker Desktop 列为 P2 本地便利项
- **待办**：开发回填 [ops→dev HANDOFF](HANDOFFS/2026-06-25-ops-to-dev-pg-ruoyi.md) Done；验通后发 Story 2 HANDOFF

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
| PostgreSQL | `127.0.0.1:5432` | ✅ | 64 表（28 业务 + 若依）；`sys_user` ✅；`pgvector` 已装 |
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
  - ~~导入若依 PG 系统表~~ ✅ 服务器已验（`import_ruoyi_pg.py --check-only` → `sys_user|64`）
  - **P0** SSH 隧道 + `mvn -pl ruoyi-admin spring-boot:run` 验通（见下「开发联调步骤」）
  - P1 Admin 页按 [UI HANDOFF](HANDOFFS/2026-06-25-ui-to-developer-admin-pages.md)（可 mock API）
  - P2 本机装 Docker 后本地 compose；`ruoyi-generator` anyline 切 postgresql
- **HANDOFF 进度**：[ops→dev PG 联调](HANDOFFS/2026-06-25-ops-to-dev-pg-ruoyi.md) — 配置 ✅ 系统表 ✅；**启动验通 ⏳**

### 开发联调步骤（隧道方案，本机无 Docker）

1. 开隧道：`docs/INFRA_ACCESS.md` §6（PG 5432、Redis **6380**、RabbitMQ 5672）
2. 设环境变量（密码见 `INFRA_ACCESS.local.md` §4）：
   - `INBOUND_PG_PASSWORD=<服务器 PG 密码>`
   - `INBOUND_REDIS_PORT=6380`
   - `INBOUND_REDIS_PASSWORD=<服务器 Redis 密码>`
3. `cd inbound-core && mvn -pl ruoyi-admin spring-boot:run -Dspring-boot.run.profiles=dev`
4. 验通标准：Hikari 连接成功 + 无 Redis 认证失败 + 8080 可访问
5. `cd inbound-admin && pnpm dev` → 登录页可用（默认账号见若依文档 `admin/admin123`）

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
| B-01 | 本机无 Docker（仅影响本地 compose，不阻塞隧道联调） | 运维/用户 | 低优先级 |
| B-02 | 若依 `spring-boot:run` 未验通 | 开发 | **进行中** |

---

## 下一步（跨角色）

1. **开发 P0**（当前瓶颈）：按 MEMORY「开发 → 开发联调步骤」隧道启动后端；验通后更新 HANDOFF Done + 消 B-02
2. **开发 P1**（验通后或 mock 并行）：`inbound-admin` 品牌 token + 工作台/GEO 诊断页（[UI HANDOFF](HANDOFFS/2026-06-25-ui-to-developer-admin-pages.md)）
3. **UI**：客户项目列表 + FR-001 创建项目线框 → `docs/design/wireframes/projects-list.md`
4. **技术总监**：开发启动验通后写 **Story 2 HANDOFF**（FR-001 创建项目 API + Admin 页面 + 租户隔离 FR-807）
5. **运维 P2**：有空时装 Docker Desktop，补本地 compose 验收

---

## 更新日志（最近 5 条）

| 日期 | 角色 | 摘要 |
|------|------|------|
| 2026-06-25 | 技术总监 | 复核 Sprint-1：服务器 sys_user 已存在(64表)；瓶颈改为 spring-boot 验通；更新联调步骤 |
| 2026-06-25 | 开发 | application-dev.yml 切 Docker PG + pom postgresql 驱动；Redis/SnailJob 对齐；待系统表导入与启动验通 |
| 2026-06-25 | 运维 | 服务器 PG/Redis/MinIO/RabbitMQ 全绿；本机无 Docker；HANDOFF 开发 PG 联调 |
| 2026-06-25 | 技术总监 | EPIC-1 Sprint-1 三路拆分；ADR-04 单库 PG；发运维 compose HANDOFF |
| 2026-06-25 | 技术总监 | 初始化共享记忆；建立四角色分工与 HANDOFF 机制 |
