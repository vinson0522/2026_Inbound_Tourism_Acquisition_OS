# 维护轨 Sprint #2 | FR-807 优先（技术总监 · 重排 2026-07-11）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-11 |
| **重排** | [暂不等三方](2026-07-11-tech-director-sprint2-reprioritize-no-third-party.md) |
| **前置** | C24 `6f4738a` · smoke 9/9 ✅ |

## 当前目标（P0）

**FR-807** 业务 `tenant_id` 从登录解析 · 跨 tenant 403 · 单测绿。

## 任务状态

| # | 角色 | 任务 | HANDOFF | 状态 |
|---|------|------|---------|:----:|
| **1** | **开发 Java** | FR-807 租户 | [→P0](2026-07-11-tech-director-to-dev-java-fr807-tenant-P0.md) | ✅ |
| 2 | 开发 Extension | Perplexity live | [→live](2026-07-11-tech-director-to-dev-extension-perplexity-live.md) | ⏸ B-23 |
| 3 | 开发 Python | Gemini 真 E2E | [→AI](2026-07-11-tech-director-to-dev-ai-gemini-grounded-e2e.md) | ⏸ B-23 |

## 窗口派发（2026-07-11 重排 · 复制到 Cursor）

| # | 窗口 | 激活 Prompt（Custom Instructions 首行） |
|---|------|----------------------------------------|
| **1** | **开发 Java**（原卡在 Perplexity 的窗口 **改派到此**） | `角色：开发 Java。必读 docs/agent-team/MEMORY.md、HANDOFF 2026-07-11-tech-director-to-dev-java-fr807-tenant-P0.md、重排决策 2026-07-11-tech-director-sprint2-reprioritize-no-third-party.md。Perplexity live / Gemini 真 E2E 已挂起 B-23，勿做。任务：BusinessTenantHelper 真实 tenant 映射 · 跨 tenant 403 集成测试 · smoke 9/9 回归 · HANDOFF Done + MEMORY。` |

**挂起任务勿开新窗口**，有三方资源再恢复 #2/#3。

## 完成后

- FR-807 测试绿 · MEMORY EPIC-1 验收表更新
- 可选 C25：`feat(core): FR-807 business tenant isolation`（用户要求 commit 时）
- B-23 仍挂起直至 Perplexity/Gemini 资源就绪
