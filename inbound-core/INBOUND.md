# inbound-core — 底座说明

本目录基于 **[RuoYi-Vue-Plus](https://github.com/dromara/RuoYi-Vue-Plus)** 拉取，作为旅获 AI 项目的 **Java 业务底座**（多租户 IAM / RBAC / 代码生成）。

| 项 | 值 |
|----|-----|
| 上游仓库 | https://github.com/dromara/RuoYi-Vue-Plus |
| 分支 | `5.X` |
| 版本 | **5.6.2**（`pom.xml` revision） |
| 拉取日期 | 2026-06-25 |
| 上游 README | 见同目录 `README.md` |

## 模块结构（若依原生）

```
inbound-core/
├── ruoyi-admin/      # 启动入口
├── ruoyi-common/     # 公共组件
├── ruoyi-modules/    # 业务模块（system、generator 等）
├── ruoyi-extend/     # 扩展（监控、SnailJob 等）
└── script/           # SQL 脚本（MySQL 为主）
```

## 与本项目的关系

- **只借**：多租户、用户/角色/菜单、代码生成器、文件/OSS 集成
- **不采用**：若依内置业务表；业务域以 `database/ddl/001_schema.sql` 为准
- **后续改造**：数据源 MySQL → PostgreSQL（`application-dev.yml` 已有 PG 注释示例）

## 本地启动（若依默认）

```bash
# 1. 导入 script/sql 下 MySQL 脚本到 ry-vue 库（MVP 临时；最终切 PG）
# 2. 修改 ruoyi-admin/src/main/resources/application-dev.yml 数据源
cd inbound-core
mvn clean install -DskipTests
cd ruoyi-admin && mvn spring-boot:run
```

默认端口 **8080**。详见 [plus-doc](https://plus-doc.dromara.org)。
