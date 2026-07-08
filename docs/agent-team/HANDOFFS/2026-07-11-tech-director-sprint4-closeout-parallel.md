# 产品化 Sprint #4 | 入库 + 体验收尾（技术总监 · 2026-07-11）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-11 |
| **前置** | C25 `c78a157` · Sprint #3 代码 Done · **C26 未 commit** |
| **原则** | 无三方依赖 · B-23 仍挂起 |

## Sprint #3 签核

| # | 任务 | 状态 |
|---|------|:----:|
| 1 | C25 FR-807 | ✅ `c78a157` |
| 2 | FR-108 趋势筛选 | ✅ 工作区 |
| 3 | 门户线框 | ✅ |
| 4 | 门户 Landing MVP | ✅ |
| 5 | tenant smoke 10/10 | ✅ |
| 6 | LOCAL_DOCKER 文档 | ✅（待 C27） |

## Sprint #4 目标

| # | 角色 | 任务 | HANDOFF |
|---|------|------|---------|
| **1** | **开发** | **C26** commit+push Sprint #3 增量 | [→C26](2026-07-11-tech-director-to-dev-c26-sprint3-commit.md) |
| **2** | **运维** | LOCAL_DOCKER + smoke README | [→运维](2026-07-11-tech-director-to-devops-local-docker-smoke-docs.md) |
| **3** | **开发 Admin** | FR-006 工作台链诊断/趋势 | [→Admin](2026-07-11-tech-director-to-dev-admin-dashboard-links.md) |
| **4** | **开发 Landing** | 门户 compose :4321 验通 | [→Landing](2026-07-11-tech-director-to-dev-landing-compose-verify.md) |

## 窗口派发

| # | 窗口 | 激活 Prompt |
|---|------|-------------|
| **1** | **开发** | `角色：开发。必读 HANDOFF 2026-07-11-tech-director-to-dev-c26-sprint3-commit.md、MEMORY B-25。任务：C26 commit+push — FR-108 trends from/to、营销门户 Landing、tenant smoke 10/10、wireframes/HANDOFF。勿提交 .chrome-live-profile/。验收：run_smoke_regression.ps1 10/10 · push。` |
| **2** | **运维** | `角色：运维。必读 deploy/LOCAL_DOCKER.md、run_smoke_regression.ps1、HANDOFF 2026-07-11-tech-director-to-devops-local-docker-smoke-docs.md。任务：文档一键 smoke 10/10 · tenant B 验租户 · scripts README · B-23 opt-in。` |
| **3** | **开发 Admin** | `角色：开发 Admin。必读 dashboard/index.vue、HANDOFF 2026-07-11-tech-director-to-dev-admin-dashboard-links.md。任务：工作台 KPI/最近诊断链到 /diagnostics 与 /diagnostics/trends · pnpm build:prod。` |
| **4** | **开发 Landing** | `角色：开发 Landing。必读 deploy compose inbound-landing、HANDOFF 2026-07-11-tech-director-to-dev-landing-compose-verify.md。任务：docker compose 起 landing :4321 · curl 营销首页 200 · README 启动说明。` |
