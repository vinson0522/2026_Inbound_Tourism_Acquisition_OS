# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 Java | 2026-07-11 | FR-807 扩展 · [Sprint #3](2026-07-11-tech-director-sprint3-productization-parallel.md) |

## 上下文

`test_tenant_isolation.py` 已覆盖 **projects** 跨 tenant 403。需扩大至其他 tourgeo 域。

**相关**：`deploy/scripts/test_tenant_isolation.py` · tenant B 用户 `tenantb` / admin123 · projectId=8 属 tenant B

## 交付请求

- [x] 扩展 smoke：tenant A token 访问 tenant B 的
  - `GET /api/v1/diagnostics/{runId}`（B 的 run）
  - `GET /api/v1/projects/{id}/leads`（B 的 project）
  - `GET /api/v1/projects/{id}/diagnostics` 列表
  - 各期望 **403** 或空/404
- [x] 脚本 `--verbose` 输出 code
- [x] 文档注释：依赖 seed tenant B + C25 DDL
- [x] 可选：并入 `run_smoke_regression.ps1` 第 10 项

## Done（由 To 角色填写）

- **完成时间**：2026-07-11
- **结果摘要**：`test_tenant_isolation.py` 4 端点 403（project/diagnostics list/run/leads）· seed run#100 lead#100 · `run_smoke_regression.ps1` 10/10 · 修复 `000001` parseLong→1 回退 bug + 6 位 padding 单测
