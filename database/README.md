# 数据库

PostgreSQL 16 + pgvector，业务与向量统一单库（MVP）。

## 文件

| 文件 | 说明 |
|------|------|
| `ddl/001_schema.sql` | 全量 DDL：28 表、枚举、索引、触发器 |
| `ddl/002_seed_demo.sql` | 演示数据：租户、项目、竞品、路线、问题库样本（对应 PRD §20.5） |

## 使用方式

### 方式 A：Docker Compose 自动初始化（推荐）

```bash
cd deploy && docker compose up -d postgres
```

首次启动时，`001_schema.sql` 与 `002_seed_demo.sql` 会挂载到 `docker-entrypoint-initdb.d/` 自动执行。

> 若卷已存在且需重建：`docker compose down -v` 后重新 `up`（**会清空数据**）。

### 方式 B：手动执行

```bash
psql -U inbound -d inbound_growth -f database/ddl/001_schema.sql
psql -U inbound -d inbound_growth -f database/ddl/002_seed_demo.sql
```

## 核心域表分组

| 域 | 主要表 |
|----|--------|
| 租户与权限 | `tenant`, `user_account`, `subscription` |
| 客户项目 | `customer_project`, `travel_product`, `competitor`, `knowledge_asset`, `knowledge_chunk` |
| GEO 诊断 | `question_bank`, `diagnostic_run`, `diagnostic_result`, `probe_node`, `probe_task`, `platform_adapter`, `scoring_rule` |
| 关键词与内容 | `keyword_opportunity`, `content_task`, `generated_content`, `content_plan` |
| 落地页与线索 | `landing_page`, `lead`, `lead_followup` |
| 报告与配置 | `report`, `template`, `model_config`, `audit_log`, `material_asset`, `video_breakdown` |

## 迁移策略（应用 scaffold 后）

- Java 侧使用 **Flyway**，版本脚本放在 `inbound-core/.../db/migration/`
- 基线版本 `V1__baseline.sql` 应与 `001_schema.sql` 一致
- 后续增量：`V2__xxx.sql`，禁止无备份的破坏性 DDL
