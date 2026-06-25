# 入境游海外获客增长 Agent（Inbound AI Growth Agent）

> GEO 诊断 · 海外关键词洞察 · 社媒内容 Agent · 落地页生成 · 询盘转化

面向中国入境游企业的 AI 海外获客增长系统。当前处于 **文档 + 基础设施就绪、RuoYi 底座已拉取** 阶段。

---

## 快速开始

```bash
# 1. 启动基础设施（PostgreSQL / Redis / MinIO / RabbitMQ / Langfuse / Gotenberg）
cd deploy
docker compose up -d

# 2. 验证数据库（首次启动会自动执行 DDL + 种子数据）
docker exec -it inbound-postgres psql -U inbound -d inbound_growth -c "\dt"
```

| 服务 | 地址 | 默认账号 |
|------|------|----------|
| PostgreSQL | `localhost:5432` | 见 [docs/INFRA_ACCESS.md](docs/INFRA_ACCESS.md) §3 |
| Redis | `localhost:6379` | — |
| MinIO Console | http://localhost:9001 | 见 INFRA_ACCESS |
| RabbitMQ UI | http://localhost:15672 | 见 INFRA_ACCESS |
| Langfuse | http://localhost:3000 | 见 INFRA_ACCESS |
| Gotenberg | http://localhost:3002 | — |

> 完整端口、密码、SSH 隧道、远程服务器连接：[docs/INFRA_ACCESS.md](docs/INFRA_ACCESS.md)

---

## 仓库结构

```
2026_Inbound_Tourism_Acquisition_OS/
│
├── README.md                          ← 本文件（项目入口）
├── CLAUDE.md                          ← AI 助手项目上下文
├── AGENT.md                           ← AI 编码工作流
├── PRD_商业化版_V2.0.md                ← 产品需求（工程基线）
│
├── docs/                              ← 技术文档
│   ├── README.md
│   ├── ARCHITECTURE.md                ← 七层架构、服务职责、数据流
│   ├── TECH_STACK_COMPONENTS.md       ← 组件版本、端口、环境变量、BOM
│   ├── INFRA_ACCESS.md                ← 中间件连接与访问（仓库内）
│   └── INFRA_ACCESS.local.md          ← 含服务器密码（本机 gitignore）
│
├── database/                          ← 数据库
│   ├── README.md
│   └── ddl/
│       ├── 001_schema.sql             ← 全量 DDL（28 表 + pgvector）
│       └── 002_seed_demo.sql          ← 演示种子（PRD §20.5 演示脚本）
│
├── deploy/                            ← 部署与本地开发
│   ├── README.md
│   ├── docker-compose.yml             ← MVP 基础设施
│   └── .env.example
│
├── inbound-core/                      ← ✅ RuoYi-Vue-Plus 5.6.2（Java 底座）
├── inbound-ai/                        ← [骨架就绪] Python FastAPI AI 服务
├── inbound-admin/                     ← ✅ plus-ui 5.6.2（Vue3 管理后台）
├── inbound-portal/                    ← [骨架就绪] 客户只读门户
├── inbound-landing/                   ← [骨架就绪] Astro 落地页
├── inbound-probe-extension/         ← [骨架就绪] Plasmo Chrome 探针扩展
│
├── .cursor/                           ← Cursor IDE 配置
│   ├── mcp.json
│   └── rules/
└── .vscode/                           ← VS Code 工作区配置
```

### 状态说明

| 目录/文件 | 状态 | 说明 |
|-----------|:----:|------|
| `docs/`、`database/`、`deploy/` | ✅ 就绪 | 可直接使用 |
| `PRD_*.md`、`CLAUDE.md`、`AGENT.md` | ✅ 就绪 | 规划与 AI Coding 基线 |
| `inbound-core` / `inbound-admin` | ✅ 已拉取 | RuoYi-Vue-Plus + plus-ui，见各目录 `INBOUND.md` |
| 其余 `inbound-*` | 📁 骨架就绪 | 按 EPIC 顺序 scaffold |
| `deploy/docker-compose` 中 core-api / ai-api | ⏳ 注释 | 待 Dockerfile 就绪后取消注释 |

---

## 文档阅读顺序

| 角色 | 建议路径 |
|------|----------|
| 产品 / 运营 | `PRD_商业化版_V2.0.md` |
| 架构 / 后端 | `docs/ARCHITECTURE.md` → `database/ddl/` |
| AI Coding | `CLAUDE.md` → `AGENT.md` → 对应 EPIC 的 FR |
| 运维 / 本地环境 | `deploy/README.md` → `docs/INFRA_ACCESS.md` → `docs/TECH_STACK_COMPONENTS.md` |

---

## EPIC 实施顺序

1. EPIC-1 基础平台 → 2. EPIC-10 AI 编排 → 3. EPIC-2 GEO 诊断 → 4. EPIC-3 关键词 → 5. EPIC-4 内容 → 6. EPIC-6 落地页 → 7. EPIC-7 线索 → 8. EPIC-8 报告 → 9. EPIC-9 计费 → 10. EPIC-11 浏览器探针 → 11. EPIC-5 爆款素材

详见 `AGENT.md` §17 依赖图。

---

## 技术栈摘要

- **业务**：Java 21 + Spring Boot 3 + MyBatis-Plus + PostgreSQL
- **AI**：Python 3.11 + FastAPI + LiteLLM + LangGraph + LlamaIndex
- **前端**：Vue 3 + Element Plus；落地页 Astro
- **探针**：Plasmo (Chrome MV3) + Grounded API
- **基础设施**：pgvector、Redis、MinIO、RabbitMQ、Langfuse、Gotenberg

---

## License

待定（商业项目）。
