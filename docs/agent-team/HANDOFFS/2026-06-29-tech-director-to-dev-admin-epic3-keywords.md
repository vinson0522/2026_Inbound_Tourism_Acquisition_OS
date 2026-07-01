# HANDOFF | 技术总监 → 开发（Admin）

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | EPIC-3 M1 · **FR-201/202** |

## 上下文

**当前状态**：Admin 无关键词页；Java API 由 [Java HANDOFF](2026-06-29-tech-director-to-dev-java-epic3-keywords.md) 提供。

**相关文件**：
- `docs/design/wireframes/keywords-list.md`
- `inbound-admin/src/api/tourgeo/` — 新建 `keyword.ts`
- `inbound-admin/src/router/index.ts`
- `inbound-admin/src/constants/` — 八阶段 label 映射（可新建 `keyword.ts`）

## 交付请求

**需要什么**：关键词机会词列表 + 生成（FR-201/202 MVP UI）。

**验收标准**：
- [x] 路由与线框一致；需当前 `projectId`（Pinia project store）
- [x] API：`listKeywords`, `generateKeywords`, `deleteKeyword`（若 Java 提供）
- [x] 八阶段 Tab 筛选 + 表格 + 分页
- [x] 「AI 生成」按钮 → 确认框 → loading → 刷新列表
- [x] 空态引导；错误 toast
- [x] 侧栏/项目详情增加「关键词」入口

## 质量 / 证据

**必须提供**：生成后列表截图；阶段筛选截图

**交给下一棒**：技术总监 EPIC-3 M1 验收

## 窗口激活 Prompt 摘要

```
角色：开发 Admin。必读 keywords-list 线框与 Java keywords API。
任务：关键词列表页 + 生成按钮；八阶段常量与 PRD 中文标签一致。
```

---

## Done（由 To 角色填写）

- **完成时间**：2026-06-29
- **结果摘要**：
  - **路由**：侧栏 `/keywords/index`；深链 `/projects/:projectId/keywords`（`activeMenu` 高亮关键词）
  - **API**：`src/api/tourgeo/keyword.ts` — list / generate / delete
  - **常量**：`src/constants/keyword.ts` — 八阶段 label + tooltip + 机会分色阶
  - **页面**：`views/tourgeo/keywords/index.vue` — Tab 筛选、表格、分页、AI 生成确认+loading、空态/错误 toast、删除
  - **入口**：项目详情「关键词机会」按钮
  - **生成**：全八阶段 × wordsPerStage=5 · market=项目首市场 · useRag=false（M1 mock 友好）
- **遗留**：Tab badge 各阶段词数（P2）；FR-203 机会分列仍为占位；转内容任务 FR-205 disabled
