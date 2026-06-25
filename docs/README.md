# 技术文档索引

| 文档 | 内容 | 何时读 |
|------|------|--------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 七层架构、服务模块、MQ 契约、Monorepo 结构、EPIC 映射、部署拓扑 | 做架构决策、拆服务、写集成代码前 |
| [TECH_STACK_COMPONENTS.md](./TECH_STACK_COMPONENTS.md) | 组件版本、端口矩阵、环境变量、Maven/pip BOM、替换矩阵 | 装依赖、配 Docker、选型变更时 |
| [INFRA_ACCESS.md](./INFRA_ACCESS.md) | 中间件端口、本地/远程连接方式、SSH 隧道、GUI 配置 | 连库、配 Redis/MinIO、本机访问远程 infra 时 |
| [INFRA_ACCESS.local.md](./INFRA_ACCESS.local.md) | **本机私密**：含服务器生产密码与即用连接串（gitignore） | 日常开发查密码；部署后运行 `export_infra_credentials.py` 同步 |

**上游产品文档**（仓库根目录）：

| 文档 | 内容 |
|------|------|
| [../PRD_商业化版_V2.0.md](../PRD_商业化版_V2.0.md) | FR 全集、数据模型、API、GEO 探针、验收标准 |
| [../CLAUDE.md](../CLAUDE.md) | AI 助手项目上下文 |
| [../AGENT.md](../AGENT.md) | AI 编码 Agent 工作指南 |

**同步规则**：改架构 → 同步 ARCHITECTURE + PRD 相关章节；改组件版本 → 同步 TECH_STACK + `deploy/docker-compose.yml`；改表结构 → 同步 `database/ddl/001_schema.sql` + PRD §11；改中间件端口/凭证 → 同步 INFRA_ACCESS + 服务器 `.env`。
