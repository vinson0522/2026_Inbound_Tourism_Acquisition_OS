# HANDOFF | 技术总监 → 运维

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 运维 | 2026-07-11 | 文档 · [Sprint #3](2026-07-11-tech-director-sprint3-productization-parallel.md) |

## 交付请求

- [x] 更新 `deploy/LOCAL_DOCKER.md`：
  - 一键 `run_smoke_regression.ps1` 前提（compose 服务列表）
  - `DIAGNOSE_MOCK_LLM=true` / `EMBED_MOCK=true` 说明
  - FR-807 验证：`test_tenant_isolation.py` · tenant B 账号
- [x] 可选：`deploy/scripts/README.md` smoke 脚本索引表（9/9 + tenant + probe API）
- [x] 注明 B-23 三方任务为 opt-in

## Done（由 To 角色填写）

- **完成时间**：2026-07-11
- **结果摘要**：`LOCAL_DOCKER.md` §2.9 一键 smoke 10/10（compose 前提 · mock 说明 · FR-807 tenant B · B-23 opt-in）· `deploy/scripts/README.md` 索引 · `run_smoke_regression.ps1` 注释对齐
- **遗留**：无；C26 commit 由开发入库
