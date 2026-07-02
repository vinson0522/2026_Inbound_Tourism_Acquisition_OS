# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-10 | EPIC-2 M3 · FR-109 · ADR-20260710-26 |

## 交付请求

- [x] DDL：`diagnostic_schedule`（tenant_id · project_id · frequency enum · enabled · probe_modes_json · models_json · sample_count · question_scope_json · calibration_ratio · next_run_at · last_run_id · last_triggered_at）
- [x] `GET/PUT /api/v1/projects/{projectId}/diagnostics/schedule` — 单条 upsert（M3 每项目 1 条）
- [x] `DiagnosticScheduleJob` — `@Scheduled(cron = "0 0 * * * ?")` 每小时 · due 且 enabled → 调 `createRun` · 更新 next_run_at/last_run_id
- [x] 触发前 quota 校验 · 超额 skip + log
- [x] `tenant.excludes` += `diagnostic_schedule`
- [x] smoke：`deploy/scripts/test_diagnostic_schedule.py` — PUT schedule · mock 触发（internal 或调 job 方法）· 断言 run 创建

## Prompt

```
角色：开发 Java。必读 DiagnosticRunServiceImpl、BillingSchedulingConfig、ADR-26。
任务：diagnostic_schedule + Job + smoke。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-02
- **结果摘要**：
  - `001_schema.sql` + `003_diagnostic_schedule.sql` · enum `diagnostic_schedule_frequency` · 含 market/locale/region
  - `DiagnosticSchedule*` entity/mapper/service · `GET/PUT .../schedule`
  - `DiagnosticScheduleJob` hourly · `createRunForSchedule` · `IQuotaService.hasRemainingQuota`
  - `POST /api/v1/internal/diagnostics/schedule-trigger?projectId=&force=true`
  - smoke ✅ runId=8 · lastRunId/nextRunAt 更新
- **遗留**：Admin 定时计划 Tab（独立 HANDOFF）· C23 未 commit
