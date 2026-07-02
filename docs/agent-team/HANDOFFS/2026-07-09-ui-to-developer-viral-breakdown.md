# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-07-09 |
| **优先级** | High |
| **关联** | EPIC-5 M1 · FR-401~403 · FR-405 提示 · [技术总监 → UI](2026-07-09-tech-director-to-ui-epic5-viral-breakdown.md) · ADR-20260709-24 |

## 上下文

**当前状态**：EPIC-4 内容任务列表 ✅；无素材上传/拆解 UI。DDL `material_asset` · `video_breakdown` 已就绪。

**相关文件**：
- `docs/design/wireframes/viral-breakdown-list.md` — 线框 + DDL/API + drawer + 版权 footnote
- `docs/design/wireframes/content-task-list.md` — 列表/项目选择器/菜单模式
- `database/ddl/001_schema.sql` — `material_asset` · `video_breakdown`
- Java HANDOFF：`2026-07-09-tech-director-to-dev-java-epic5-viral.md`
- Python HANDOFF：`2026-07-09-tech-director-to-dev-ai-epic5-breakdown.md`
- Admin HANDOFF：`2026-07-09-tech-director-to-dev-admin-epic5-viral.md`

**约束**：
- M1 mock 拆帧可测（`BREAKDOWN_MOCK_LLM`）
- FR-405 仅 footnote/alert · **不拦截**上传或查看
- 无 FR-404 标签库 · FR-406 推荐 · 删除 · 导出 confirm
- 七维键固定：theme/hook/shot/subtitle/emotion/psychology/reusable

## 交付请求

**需要什么**：Admin 爆款拆解列表 + 上传 + 拆解触发 + 详情 drawer。

**验收标准**：
- [ ] 路由 `/projects/:projectId/materials`；菜单「内容 Agent → 爆款拆解」
- [ ] 项目选择器 · 筛选 type/breakdownStatus/copyright/createdAt
- [ ] 表格：缩略图、素材名、类型、版权、拆解状态 Tag、帧数、操作
- [ ] 「上传素材」dialog · multipart · 类型/版权/来源 · 格式限制
- [ ] 行「开始拆解」→ `POST .../breakdown` · PROCESSING + 5s 轮询
- [ ] 行「查看拆解」→ drawer 720px：七维 descriptions · reusableStructure · frames 网格
- [ ] `needsHumanReview` Tag · PROCESSING skeleton · FAILED 态
- [ ] 页底 + 上传 dialog 版权 warning footnote（FR-405）
- [ ] `api/tourgeo/material.ts` · 路由注册
- [ ] `pnpm build:prod` ✅

## 后端依赖

- [ ] `POST/GET .../materials` · `POST .../materials/{id}/breakdown` · `GET .../breakdowns/{id}`
- [ ] List join `breakdownStatus` · `frameCount` · `needsHumanReview`
- [ ] Python extract-frames + analyze callback
- [ ] smoke `test_material_breakdown.py`

## API 封装建议（`material.ts`）

```typescript
uploadMaterial(projectId, FormData)
listMaterials(projectId, query)
triggerBreakdown(projectId, materialId)
getBreakdown(projectId, breakdownId)
```

## 质量 / 证据

**必须提供**：
- 上传成功列表新行截图
- 拆解中 → 完成状态切换截图
- drawer 七维表 + 6 帧网格截图
- 版权 footnote 可见截图

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
