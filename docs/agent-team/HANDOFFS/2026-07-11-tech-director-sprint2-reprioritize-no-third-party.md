# 技术总监决策 | 维护轨 #2 重排（暂不等三方）

| 日期 | 2026-07-11 |
|------|------------|
| **决策** | Perplexity live · Gemini 真 E2E **挂起**；**FR-807 租户** 升为 P0 |
| **原因** | 开发窗口卡在 Perplexity 登录/Chrome live；Gemini 配额 B-03b 已知阻塞；mock/fixture smoke 9/9 已绿，不挡 MVP |
| **关联** | [Sprint #2 索引](2026-07-11-tech-director-post-maintenance-b03b-sprint.md) · B-03b · B-23 |

## 挂起（Backlog · 有三方资源再开）

| 原 # | 任务 | HANDOFF | 恢复条件 |
|:----:|------|---------|----------|
| 1 | Perplexity live Chrome | [→Extension live](2026-07-11-tech-director-to-dev-extension-perplexity-live.md) | perplexity.ai 可登录 · 愿意跑 `verify_perplexity_live.py` |
| 2 | Gemini grounded 真 E2E | [→AI Gemini](2026-07-11-tech-director-to-dev-ai-gemini-grounded-e2e.md) | `GEMINI_API_KEY` 配额恢复 |

**已完成、不重复做**：fixture hook · `test:perplexity-hook` · `test_probe_extension_e2e` · `verify_perplexity_live.py` scaffold（可选入库，**不要求跑 live**）

## 当前 P0

| # | 角色 | 任务 | HANDOFF |
|:--:|------|------|---------|
| **1** | **开发 Java** | FR-807 租户隔离 | [→Java FR-807](2026-07-11-tech-director-to-dev-java-fr807-tenant.md) |

## 给卡在 Perplexity 的开发窗口

**立即停止** live Perplexity 验收。该任务已挂起（B-23）。  
**切换任务** → FR-807 `BusinessTenantHelper` · 跨 tenant 403 测试。
