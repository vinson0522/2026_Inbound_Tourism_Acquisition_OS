# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-09 | EPIC-11 M2 · FR-115/116 · ADR-20260709-22 |

## 交付请求

- [ ] `GET /api/v1/settings/platform-adapters` — 租户 adapter 列表
- [ ] `GET /api/v1/settings/platform-adapters/{platform}` · `PUT` upsert — `dom_selectors_json` / `api_patterns_json` / `parse_rules_json` / `enabled`
- [ ] `GET /api/v1/projects/{projectId}/diagnostics/{runId}/calibration` — 返回重叠 question 的 api vs extension 对比 · `deviationRate` · pairs[]
- [ ] `createRun`：当 `calibration_ratio`>0 且含 `grounded-api`+`browser-extension` 时，按比例标记重叠 browser-extension 任务（M2 简化：同一批 question 各 1 样本双模式）
- [ ] seed 第二平台 `chatgpt` adapter v1.0
- [ ] smoke：`deploy/scripts/test_probe_calibration.py`

## Prompt

```
角色：开发 Java。必读 PlatformAdapter、DiagnosticRunServiceImpl.createRun、ADR-22。
任务：adapter Admin API + calibration GET + calibration_ratio + smoke。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-09
- **结果摘要**：
  - `PlatformAdapterSettingsController` — `GET/PUT /api/v1/settings/platform-adapters` + `/{platform}`
  - `DiagnosticRunServiceImpl.queryCalibration` — Jaccard 相似度 · brandMentionAgreementRate · pairs[]
  - `createRun` — `calibration_ratio>0` + 双模式时 extension 仅校准 question · 各 1 样本 · `question_scope_json.calibrationQuestionIds`
  - `002_seed_demo.sql` — 追加 `chatgpt` adapter v1.0
  - `deploy/scripts/test_probe_calibration.py` · `mvn compile` ✅ · 技术总监 smoke runId=6 ✅
- **遗留**：`mvn install ruoyi-diagnostic` 后重启 Java；C19 待 commit
