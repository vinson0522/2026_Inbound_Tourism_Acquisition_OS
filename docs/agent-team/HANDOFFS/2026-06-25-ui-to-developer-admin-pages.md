# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-06-25 |
| **优先级** | High |
| **关联** | EPIC-1 / EPIC-2 · FR-006 · FR-103 · PRD §6.1 |

## 上下文

**当前状态**：UI 已输出 Admin 设计 token、工作台与 GEO 诊断任务列表线框，均位于 `docs/design/`。底座仍为 plus-ui，**不重写** `layout/`，仅新增业务视图与主题色。

**相关文件**：
- `docs/design/README.md` — 设计系统索引与导航映射
- `docs/design/tokens.md` — 色/字/间距 token（含 `--tg-*` CSS 变量片段）
- `docs/design/wireframes/dashboard.md` — 工作台线框
- `docs/design/wireframes/diagnostics-list.md` — GEO 诊断列表 + 新建抽屉
- `inbound-admin/src/settings.ts` — 主题色当前 `#409EFF`，需改为 `#1677A0`
- `inbound-admin/src/assets/styles/variables.module.scss` — 追加 token
- `inbound-admin/src/views/system/user/index.vue` — 列表页实现模板

**约束**：
- Element Plus 组件优先；列表页遵循 plus-ui 现有模式（搜索卡片、`right-toolbar`、`el-table border`）
- WCAG AA：状态 Tag 带中文文案，不仅靠颜色
- GEO 列表/新建须含合规提示（probe_mode、采样性质，见线框）
- API 未就绪时允许 mock，但路由与组件结构须到位
- 不修改 Java/Python 后端（本 HANDOFF 仅前端）

## 交付请求

**需要什么**：在 `inbound-admin` 落地品牌 token、注册菜单路由，实现「工作台」与「GEO 诊断 · 诊断任务列表」两页（含空/加载/无项目态），并可切换当前项目上下文。

**验收标准**：
- [ ] `settings.ts` 主色 `#1677A0`；`:root` 已追加 `docs/design/tokens.md` §6 中 `--tg-*` 变量
- [ ] 路由 `/dashboard` 可访问，替换原 RuoYi 欢迎页为工作台布局（KPI 四卡 + 今日任务 + 预警 + 本周建议 + 最近诊断表）
- [ ] 路由 `/diagnostics/runs`（或 `/projects/:id/diagnostics`）可访问，含搜索、表格、分页、新建抽屉表单
- [ ] 顶栏或页内「当前项目」`el-select` 可切换（mock 2 个项目即可）
- [ ] 诊断状态 Tag 与 `diagnostic_run_status` 枚举一致（PENDING/RUNNING/SUCCESS/PARTIAL_FAILED/FAILED/CANCELLED）
- [ ] 新建诊断默认探针模式仅 `grounded-api`；抽屉含合规 `el-alert`
- [ ] 无项目 / 空列表 / loading 三态可用
- [ ] 浏览器内无明显对比度问题（正文 #4B5563 on #FFF）

## 实现清单（建议顺序）

1. **Token**：`variables.module.scss` + `settings.ts` + 启动时 `handleThemeStyle`
2. **目录**：`src/views/tourgeo/dashboard/index.vue`、`src/views/tourgeo/diagnostics/index.vue`
3. **API 封装**：`src/api/tourgeo/project.ts`、`diagnostic.ts`（可先 mock）
4. **Store（可选）**：`useProjectStore` 存 `currentProjectId`
5. **路由/菜单**：若依菜单 SQL 或后台配置（见各线框「菜单配置」节）
6. **替换首页**：`/index` redirect → `/dashboard`

## 质量 / 证据

**必须提供**：
- `pnpm dev` 截图：工作台 + 诊断列表 + 新建抽屉
- 浏览器 Network 或说明 mock 数据结构

**交给下一棒**：
- 后端 API 就绪后，开发将 mock 换为真实接口并更新 `MEMORY.md` 开发章节
- UI 下一 Sprint：客户项目列表 + FR-001 创建项目表单线框（待产品确认后补 HANDOFF）

---

## Done（由 To 角色填写）

- **完成时间**：2026-06-25
- **结果摘要**：
  - Token：`settings.ts` `#1677A0`；`variables.module.scss` 追加 `--tg-*`
  - 路由：`/dashboard` 工作台、`/diagnostics/runs` 诊断列表（constantRoutes 侧栏可见）
  - 视图：`src/views/tourgeo/dashboard/index.vue`、`diagnostics/index.vue`
  - Mock API：`src/api/tourgeo/*` + `useProjectStore` 项目切换
  - 组件：ProjectSelector、DiagnosticStatusTag、GeoScoreDisplay
  - 新建抽屉：默认 grounded-api + 合规 el-alert；状态 Tag 对齐枚举
- **遗留**：
  - [ ] 后端 API 就绪后替换 mock（`VITE_TOURGEO_MOCK=false`）
  - [ ] 若依后台菜单 SQL 与权限（当前用前端 constantRoutes）
  - [ ] 截图归档（开发本地验通）
