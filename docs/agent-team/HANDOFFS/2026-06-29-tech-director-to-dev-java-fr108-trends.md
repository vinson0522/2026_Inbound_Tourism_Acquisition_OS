# HANDOFF | 技术总监 → 开发（Java）

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-2 M2.2 · **FR-108** · ADR-20260629-10 |

## 上下文

**当前状态**：`ruoyi-diagnostic` 已有 CRUD + 报告导出；`diagnostic_run` 含 `geo_score`、`finished_at`、`status`。

**相关文件**：
- `inbound-core/ruoyi-modules/ruoyi-diagnostic/` — 扩展点
- `DiagnosticController.java` — 现有 `/api/v1/projects/{id}/diagnostics`
- `001_schema.sql` — `diagnostic_run`, `diagnostic_result.score_json`
- `BusinessTenantHelper` — 租户过滤

**约束**：
- Java 管 CRUD/聚合，**不调 LLM**
- 只返回 `SUCCESS` 或 `PARTIAL_FAILED` 且 `geo_score IS NOT NULL` 的 run
- 统一 RuoYi 响应；分页 `limit` 默认 12、最大 52

## 交付请求

**需要什么**：FR-108 诊断趋势查询 API。

**验收标准**：
- [ ] `GET /api/v1/projects/{projectId}/diagnostics/trends`
  - Query：`limit`（默认 12）、可选 `market`
  - 响应：`runs[]` 含 `runId`, `name`, `geoScore`, `finishedAt`, `market`, `status`；按 `finished_at ASC`
  - 可选 `metricsSeries`：从该 run 的 `diagnostic_result.score_json` 聚合 6 分项（与 PRD GEO 公式一致，读权重自 `scoring_rule` 或前端已有估算）
- [ ] 租户隔离；跨 project 403
- [ ] 单元/集成测试：2 条 run 返回 2 点；0 条返回空数组
- [ ] 可选 smoke：`deploy/scripts/test_diagnostic_trends_api.py`

## 质量 / 证据

**必须提供**：curl 或 pytest 输出；`mvn -pl ruoyi-modules/ruoyi-diagnostic test` 通过

**交给下一棒**：[开发 Admin FR-108](2026-06-29-tech-director-to-dev-admin-fr108-trends.md)

## 窗口激活 Prompt 摘要

```
角色：开发。必读 HANDOFF 2026-06-29-tech-director-to-dev-java-fr108-trends.md 与 ruoyi-diagnostic 模块。
任务：实现 GET .../diagnostics/trends；租户隔离；最小 diff。
本机 Docker ADR-09，不开 SSH 隧道。
```

---

## Done（由 To 角色填写）

- **完成时间**：2026-06-29
- **结果摘要**：`GET /api/v1/projects/{projectId}/diagnostics/trends?limit=12`；`runs[]` 含 runId/name/geoScore/finishedAt/market/status/metrics；`finished_at ASC`；仅 SUCCESS/PARTIAL_FAILED + geo_score 非空；`DiagnosticMetricsAggregator` 不调 LLM；`mvn -pl ruoyi-modules/ruoyi-diagnostic test` + `test_diagnostic_trends.py`（2 点 runId=2/3）
- **遗留**：Admin ECharts 趋势页（→ Admin HANDOFF）；P2 可选 `byStage` / 时间范围 filter
