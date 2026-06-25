# HANDOFF | 运维 → 开发

| 字段 | 值 |
|------|-----|
| **From** | 运维 |
| **To** | 开发 |
| **日期** | 2026-06-25 |
| **优先级** | High |
| **关联** | EPIC-1 / ADR-20260623-02 / ADR-20260625-04 |

## 上下文

**当前状态**：共享服务器 `18.139.209.10` 上 `docker-compose.prod.yml` 六服务已 Up，PG/Redis/MinIO/RabbitMQ 四项 healthcheck 全绿。`inbound_growth` 库已有 **28 张业务表**（`001_schema.sql` init）及 demo 种子，**尚未导入若依系统表**（`sys_user` 不存在）。

`application-dev.yml` 已将 master 数据源改为 PostgreSQL driver，但 Redis 仍为若依默认 `ruoyi123`，与 compose 不一致。

**相关文件**：
- `deploy/docker-compose.yml` — 本地 dev compose（账号见下表）
- `deploy/docker-compose.prod.yml` — 服务器 prod compose
- `docs/INFRA_ACCESS.local.md` — 完整凭证（§3 本地 / §4 服务器 / §5 连接串）
- `inbound-core/ruoyi-admin/src/main/resources/application-dev.yml` — 数据源已指 PG
- `inbound-core/script/sql/postgres/postgres_ry_vue_5.X.sql` — 若依 PG 系统表脚本
- `database/ddl/001_schema.sql` — 业务表（已 init，勿重复 DROP）

**约束**（ADR-20260625-04）：
- **单库** `inbound_growth`，`public` schema；业务表 + 若依系统表共存
- 禁止 MySQL 双库过渡
- Langfuse 使用独立库 `langfuse`（服务器已 init）
- 开发机连远程须先开 SSH 隧道（`INFRA_ACCESS.local.md` §6）

## 交付请求

**需要什么**：完成若依 PostgreSQL 联调，使 `./mvnw spring-boot:run`（profile=dev）能连库并启动至登录页无 DB 报错。

**验收标准**：
- [ ] 若依系统表已导入 `inbound_growth`（`sys_user` 等存在；与 28 业务表无命名冲突）
- [ ] `application-dev.yml` Redis/RabbitMQ/MinIO 与目标环境一致（见下表）
- [ ] 本地或隧道连 PG：`SELECT count(*) FROM pg_tables WHERE schemaname='public'` ≥ 28 + 若依表数
- [ ] `./mvnw -pl ruoyi-admin spring-boot:run` 启动成功，日志无 datasource 连接失败
- [ ] 更新 `MEMORY.md` 开发章节 + 本文件 Done 段

## 连接配置（复制即用）

### 方案 A — 本地 Docker（需先安装 Docker Desktop）

```properties
# application-dev.yml → spring.datasource.dynamic.datasource.master
url=jdbc:postgresql://localhost:5432/inbound_growth?useUnicode=true&characterEncoding=utf8&useSSL=false&reWriteBatchedInserts=true
username=inbound
password=inbound_dev_pass

# spring.data.redis（本地 compose 无密码）
host=localhost
port=6379
password=          # 留空或删除 requirepass

# RabbitMQ（若启用）
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=inbound
spring.rabbitmq.password=inbound_dev_pass
```

### 方案 B — SSH 隧道连共享服务器（当前可用）

先开隧道（`docs/INFRA_ACCESS.local.md` §6），Host 一律 `localhost`：

```properties
url=jdbc:postgresql://localhost:5432/inbound_growth?useUnicode=true&characterEncoding=utf8&useSSL=false&reWriteBatchedInserts=true
username=inbound
password=<见 INFRA_ACCESS.local.md §4>

spring.data.redis.host=localhost
spring.data.redis.port=6380          # 注意：服务器映射 6380，非 6379
spring.data.redis.password=<见 §4>

spring.rabbitmq.host=localhost
spring.rabbitmq.username=inbound
spring.rabbitmq.password=<见 §4>
```

Python 异步连接串（`inbound-ai` 参考）：

```bash
DATABASE_URL=postgresql+asyncpg://inbound:<password>@localhost:5432/inbound_growth
REDIS_URL=redis://:<password>@localhost:6380/1
RABBITMQ_URL=amqp://inbound:<password>@localhost:5672/
```

## 若依改 PG 注意事项

1. **系统表导入**：在已有 28 业务表库上执行 `inbound-core/script/sql/postgres/postgres_ry_vue_5.X.sql`（及 `postgres_ry_job.sql` 若启用 SnailJob）。建议先 `grep -i` 检查表名是否与业务表冲突；RuoYi 表前缀多为 `sys_` / `gen_`，业务表为 `tenant`、`customer_project` 等，当前无冲突迹象。

2. **Driver 已就绪**：`ruoyi-admin/pom.xml` 已启用 `postgresql` 依赖，MySQL connector 已注释。

3. **Redis 密码对齐**：`application-dev.yml` 现写 `password: ruoyi123`，本地 compose Redis **无密码**；连服务器 Redis 须改 **6380 + 生产密码**。Redisson 段需同步。

4. **代码生成器**：`ruoyi-generator/pom.xml` 仍依赖 `anyline-data-jdbc-mysql`；生成器若报错需切 `anyline-data-jdbc-postgresql`（非启动阻塞项，但 EPIC-1 后需处理）。

5. **SnailJob**：`ruoyi-snailjob-server/application-dev.yml` 仍为 MySQL；MVP 可先 `snail-job.enabled: false` 或单独改 PG。

6. **Flyway**：确认 RuoYi Flyway 脚本与 `001_schema.sql` 不互相 DROP；优先用手动 SQL init 若依表 + 关闭重复 migration，或 Flyway baseline。

7. **p6spy**：dev 环境 `p6spy: true` 会打印 SQL，联调期可保留。

## 质量 / 证据

**必须提供**：
- `sys_user` 存在性查询：`SELECT to_regclass('public.sys_user')`
- 启动日志片段（datasource + Hikari 连接成功）
- 若用隧道：注明隧道命令与 profile

**交给下一棒**：启动验通后 UI 可联调 Admin 登录页；技术总监拆 Story 2。

---

> 完成后：开发在本文件末尾追加 **Done** 段，并更新 `MEMORY.md`。

## Done（由 To 角色填写）

- **完成时间**：2026-06-25
- **结果摘要**：
  - `application-dev.yml`：master → `jdbc:postgresql://localhost:5432/inbound_growth`（inbound/inbound_dev_pass）；Redis 无密码；`snail-job.enabled: false`
  - `ruoyi-admin/pom.xml`：启用 postgresql 依赖，注释 mysql-connector-j
  - 本机未安装 Docker，未能执行系统表 SQL 与 `spring-boot:run` 验通
- **遗留**：
  - [ ] 执行 `postgres_ry_vue_5.X.sql`（及 workflow 若启用工作流模块）
  - [ ] Docker compose up 后启动验通并提供 Hikari 连接成功日志
  - [ ] 连服务器方案 B 时改 Redis 6380 + 生产密码
  - [ ] `ruoyi-generator` anyline 切 postgresql（非启动阻塞）
