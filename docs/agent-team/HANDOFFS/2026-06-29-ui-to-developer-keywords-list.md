# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-3 M1 · **FR-201/202** |

## 上下文

**当前状态**：项目详情 Tab 已上线；关键词 Admin 页未实现。本线框为 EPIC-3 M1 列表 MVP。

**相关文件**：
- `docs/design/wireframes/keywords-list.md` — 线框 + 八阶段枚举 + DDL 字段表
- `docs/design/tokens.md` — Tag、机会分色阶、空态
- `database/ddl/001_schema.sql` — `keyword_opportunity`
- `inbound-admin/src/constants/diagnostic.ts` — `LIFECYCLE_STAGE_LABELS`（可抽到 `keyword.ts`）
- [Java EPIC-3 HANDOFF](2026-06-29-tech-director-to-dev-java-epic3-keywords.md) — list / generate API
- [Admin EPIC-3 HANDOFF](2026-06-29-tech-director-to-dev-admin-epic3-keywords.md) — 验收清单

**约束**：
- 路由 **`/projects/:projectId/keywords`**（与线框一致）
- 八阶段中文标签与 PRD / `LIFECYCLE_STAGE_LABELS` 一致
- M1 无详情抽屉；「转任务」disabled
- 机会分列 M1 占位（FR-203 后续）

## 交付请求

**需要什么**：实现关键词机会词列表页 + 侧栏菜单 + 生成流程 UI。

**验收标准**：
- [ ] 路由 `/projects/:projectId/keywords`；侧栏「关键词洞察 → 机会词列表」
- [ ] 项目选择器（`:projectId` 与顶栏当前项目同步）
- [ ] 八阶段 Tab（全部 + 8 阶段）+ badge 可选
- [ ] 表格列：keyword / keyword_en / keyword_cn / stage / market / score(占位) / status / created_at
- [ ] 筛选：市场、关键词模糊搜索
- [ ] 「AI 生成机会词」→ 确认框 → loading → 刷新列表
- [ ] 空态（全空 / 阶段空 / 搜索无结果）与错误 toast
- [ ] API 封装：`listKeywords`、`generateKeywords`、`deleteKeyword`（若 Java 提供）
- [ ] 项目详情或侧栏增加「关键词」入口（链到本路由）

## 质量 / 证据

**必须提供**：生成后列表截图；切换「灵感」Tab 截图；空态截图

**交给下一棒**：[开发 Admin EPIC-3](2026-06-29-tech-director-to-dev-admin-epic3-keywords.md) 与本 HANDOFF 合并验收

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
