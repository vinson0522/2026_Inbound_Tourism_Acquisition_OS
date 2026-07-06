# 产品化 Sprint #3 | 并行 backlog（技术总监 · 2026-07-11）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-11 |
| **前置** | C24 `6f4738a` · FR-807 代码 Done 未 commit · B-23 三方挂起 |
| **原则** | **全部不依赖** Perplexity 登录 / Gemini 配额 / 外部 API Key |

## 目标

入库 FR-807 · 补 EPIC-2/6 P2 · 门户 MVP · 文档/ smoke 脚本 · 租户隔离扩大覆盖面。

## 任务表

| # | 角色 | 任务 | HANDOFF | 依赖 | 验收 |
|---|------|------|---------|------|------|
| **1** | **开发 Java** | **C25** FR-807 commit+push | [→C25](2026-07-11-tech-director-to-dev-c25-fr807-commit.md) | — | push · smoke 9/9 |
| **2** | **开发 Admin** | FR-108 P2 趋势时间筛选 | [→Admin trends](2026-07-11-tech-director-to-dev-admin-fr108-trends-filter.md) | — | build:prod |
| **3** | **UI 设计** | 营销门户线框 | [→UI portal](2026-07-11-tech-director-to-ui-marketing-portal.md) | — | wireframe + HANDOFF |
| **4** | **开发 Landing** | 门户首页 MVP | [→Landing](2026-07-11-tech-director-to-dev-landing-portal-home.md) | #3 可并行粗版 | pnpm build |
| **5** | **开发 Java** | 租户隔离 smoke 扩展 | [→tenant smoke](2026-07-11-tech-director-to-dev-java-tenant-smoke-expand.md) | C25 后 | test_tenant_* 多域 |
| **6** | **运维** | LOCAL_DOCKER 文档 | [→ops docs](2026-07-11-tech-director-to-devops-local-docker-smoke-docs.md) | — | 新人可 9/9 |

**挂起（勿开窗口）**：B-23 Perplexity live · Gemini 真 E2E

## 窗口派发（完整复制）

| # | 窗口 | 激活 Prompt |
|---|------|-------------|
| **1** | **开发 Java** | `角色：开发 Java。必读 MEMORY.md、HANDOFF 2026-07-11-tech-director-to-dev-c25-fr807-commit.md。任务：C25 commit+push FR-807 租户隔离（BusinessTenantHelper 统一、006 DDL、test_tenant_isolation.py、*Tenant* 测试）。验收：run_smoke_regression.ps1 9/9 · push origin/main。更新 MEMORY · 关闭 B-24。` |
| **2** | **开发 Admin** | `角色：开发 Admin。必读 diagnostics/trends.vue、wireframes diagnostics-list FR-108 P2、HANDOFF 2026-07-11-tech-director-to-dev-admin-fr108-trends-filter.md。任务：趋势页时间范围筛选（如近 30/90 天或 date range）· 列表 run 链详情 · pnpm build:prod。` |
| **3** | **UI 设计** | `角色：UI 设计。必读 PRD 门户/品牌、inbound-landing index 占位、HANDOFF 2026-07-11-tech-director-to-ui-marketing-portal.md。任务：docs/design/wireframes/marketing-portal-home.md · Hero/功能/CTA/页脚 · UI→Landing HANDOFF。` |
| **4** | **开发 Landing** | `角色：开发 Landing。必读 inbound-landing/src/pages/index.astro、HANDOFF 2026-07-11-tech-director-to-dev-landing-portal-home.md。任务：替换占位首页为营销门户 MVP（可先用 wireframe 粗版）· Turnstile 无 · pnpm build。` |
| **5** | **开发 Java** | `角色：开发 Java。必读 test_tenant_isolation.py、FR-807、HANDOFF 2026-07-11-tech-director-to-dev-java-tenant-smoke-expand.md。任务：扩展跨 tenant smoke 至 diagnostics/leads/reports 各 1 端点 · 403 断言 · 并入 run_smoke 或独立脚本。` |
| **6** | **运维** | `角色：运维。必读 deploy/LOCAL_DOCKER.md、run_smoke_regression.ps1、HANDOFF 2026-07-11-tech-director-to-devops-local-docker-smoke-docs.md。任务：文档增补一键 smoke 9/9 · tenant B 登录说明 · FR-807 验 tenant 步骤。` |

## 完成后

- C25 push · Admin/Landing build · 门户线框 · 文档 · B-24 关闭
- 可选 C26：docs + verify_perplexity_live.py（工具，不要求 live）
