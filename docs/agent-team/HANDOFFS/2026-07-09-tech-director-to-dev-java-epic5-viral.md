# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-09 | EPIC-5 M1 · FR-401~403 · ADR-20260709-24 |

## 交付请求

- [ ] `POST /api/v1/projects/{projectId}/materials` — multipart 上传 → MinIO `{tenant}/{project}/materials/` · 写 `material_asset`
- [ ] `GET /api/v1/projects/{projectId}/materials` — 分页列表
- [ ] `POST /api/v1/projects/{projectId}/materials/{materialId}/breakdown` — 创建 `video_breakdown` · MQ `ai.breakdown` · 202 Accepted
- [ ] `GET .../breakdowns/{breakdownId}` — frames_json + dimensions_json + reusable_structure
- [ ] Internal callback：`POST /api/v1/internal/materials/breakdown-callback` — Python 写回结果
- [ ] `tenant.excludes` += `material_asset` · `video_breakdown`
- [ ] smoke：`deploy/scripts/test_material_breakdown.py`

## Prompt

```
角色：开发 Java。必读 material_asset/video_breakdown DDL、KnowledgeAsset 上传、MQ 模式、ADR-24。
任务：material CRUD + breakdown 异步 + smoke。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-09
- **结果摘要**：material upload/list · breakdown trigger 202 · internal callback · `test_material_breakdown.py` E2E ✅ · tenant.excludes · ai.breakdown queue · MinIO `file.getBytes()` 上传 · 本机需 `sys_oss_config` 凭证与 MinIO 一致
- **遗留**：Python worker 待 inbound-ai 窗口；Admin `/materials` 待 #4
