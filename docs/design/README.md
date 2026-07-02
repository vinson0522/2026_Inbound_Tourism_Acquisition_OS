# 旅获 AI — Admin 设计系统

> **产品**：Inbound AI Growth Agent / 旅获 AI / TourGEO Agent  
> **底座**：plus-ui（RuoYi-Vue-Plus 5.6.2）+ Element Plus  
> **原则**：扩展壳层、不重造布局；B2B 清晰密度；WCAG AA；组件优先 Element Plus

---

## 文档索引

| 文件 | 内容 |
|------|------|
| [tokens.md](./tokens.md) | 品牌色、字体、间距、圆角、阴影、语义色 |
| [wireframes/dashboard.md](./wireframes/dashboard.md) | 工作台（项目概览）线框 |
| [wireframes/diagnostics-list.md](./wireframes/diagnostics-list.md) | GEO 诊断 · 诊断任务列表线框 |
| [wireframes/diagnostic-detail.md](./wireframes/diagnostic-detail.md) | GEO 诊断 · 诊断详情/结果（EPIC-2） |
| [wireframes/diagnostic-trends.md](./wireframes/diagnostic-trends.md) | GEO 诊断 · 趋势对比 FR-108 |
| [wireframes/keywords-list.md](./wireframes/keywords-list.md) | 关键词洞察 · 机会词列表 FR-201/202 |
| [wireframes/content-task-list.md](./wireframes/content-task-list.md) | 内容 Agent · 内容任务列表 EPIC-4 M1（预研） |
| [wireframes/landing-page-list.md](./wireframes/landing-page-list.md) | 落地页 Agent · 页面列表 EPIC-6 M1 |
| [wireframes/landing-page-publish.md](./wireframes/landing-page-publish.md) | 落地页 Agent · 发布与公网预览 EPIC-6 M2 |
| [wireframes/leads-list.md](./wireframes/leads-list.md) | 线索与转化 · 询盘列表 EPIC-7 M1 + **CRM M2** |
| [wireframes/reports-list.md](./wireframes/reports-list.md) | 报告中心 · 报告列表 EPIC-8 M1 |
| [wireframes/billing-settings.md](./wireframes/billing-settings.md) | 系统设置 · 套餐与额度 EPIC-9 M1 |
| [wireframes/probe-nodes.md](./wireframes/probe-nodes.md) | 系统设置 · 探针节点 EPIC-11 M1 |
| [wireframes/projects-list.md](./wireframes/projects-list.md) | 客户项目列表 + FR-001 创建线框 |
| [wireframes/project-detail.md](./wireframes/project-detail.md) | 客户项目详情 · Story 3 Tab（品牌/竞品/知识库） |
| [../agent-team/HANDOFFS/2026-06-25-ui-to-developer-admin-pages.md](../agent-team/HANDOFFS/2026-06-25-ui-to-developer-admin-pages.md) | UI → 开发（工作台/GEO） |
| [../agent-team/HANDOFFS/2026-06-25-ui-to-developer-projects-list.md](../agent-team/HANDOFFS/2026-06-25-ui-to-developer-projects-list.md) | UI → 开发（项目列表） |
| [../agent-team/HANDOFFS/2026-06-29-ui-to-developer-project-detail.md](../agent-team/HANDOFFS/2026-06-29-ui-to-developer-project-detail.md) | UI → 开发（项目详情 Story 3） |
| [../agent-team/HANDOFFS/2026-06-29-ui-to-developer-diagnostic-trends.md](../agent-team/HANDOFFS/2026-06-29-ui-to-developer-diagnostic-trends.md) | UI → 开发（趋势对比 FR-108） |
| [../agent-team/HANDOFFS/2026-06-29-ui-to-developer-keywords-list.md](../agent-team/HANDOFFS/2026-06-29-ui-to-developer-keywords-list.md) | UI → 开发（关键词列表 FR-201/202） |
| [../agent-team/HANDOFFS/2026-06-29-ui-to-developer-content-task-list.md](../agent-team/HANDOFFS/2026-06-29-ui-to-developer-content-task-list.md) | UI → 开发（内容任务 EPIC-4 M1 预研） |
| [../agent-team/HANDOFFS/2026-07-03-ui-to-developer-landing-publish.md](../agent-team/HANDOFFS/2026-07-03-ui-to-developer-landing-publish.md) | UI → 开发（落地页发布 EPIC-6 M2） |
| [../agent-team/HANDOFFS/2026-07-01-ui-to-developer-landing-page-list.md](../agent-team/HANDOFFS/2026-07-01-ui-to-developer-landing-page-list.md) | UI → 开发（落地页 EPIC-6 M1） |
| [../agent-team/HANDOFFS/2026-07-07-ui-to-developer-leads-crm.md](../agent-team/HANDOFFS/2026-07-07-ui-to-developer-leads-crm.md) | UI → 开发（线索 CRM EPIC-7 M2） |
| [../agent-team/HANDOFFS/2026-07-01-ui-to-developer-leads-list.md](../agent-team/HANDOFFS/2026-07-01-ui-to-developer-leads-list.md) | UI → 开发（线索 EPIC-7 M1） |
| [../agent-team/HANDOFFS/2026-07-05-ui-to-developer-probe-nodes.md](../agent-team/HANDOFFS/2026-07-05-ui-to-developer-probe-nodes.md) | UI → 开发（探针节点 EPIC-11 M1） |
| [../agent-team/HANDOFFS/2026-07-04-ui-to-developer-billing-settings.md](../agent-team/HANDOFFS/2026-07-04-ui-to-developer-billing-settings.md) | UI → 开发（计费 EPIC-9 M1） |
| [../agent-team/HANDOFFS/2026-07-02-ui-to-developer-reports-list.md](../agent-team/HANDOFFS/2026-07-02-ui-to-developer-reports-list.md) | UI → 开发（报告 EPIC-8 M1） |

---

## 与 PRD §6.1 导航映射

| 一级模块 | 路由建议（开发实现） | 本阶段设计范围 |
|----------|---------------------|----------------|
| 工作台 | `/dashboard` | ✅ [dashboard.md](./wireframes/dashboard.md) |
| 客户项目 | `/projects` | ✅ [列表](wireframes/projects-list.md) + [详情 Story 3](wireframes/project-detail.md) |
| GEO 诊断 | `/diagnostics` | ✅ [列表](wireframes/diagnostics-list.md) + [详情](wireframes/diagnostic-detail.md) + [趋势 FR-108](wireframes/diagnostic-trends.md) |
| 关键词洞察 | `/projects/:projectId/keywords`（侧栏 `/keywords` 重定向） | ✅ [keywords-list.md](wireframes/keywords-list.md) FR-201/202 M1 |
| 内容 Agent | `/content` | 🔶 [content-task-list.md](wireframes/content-task-list.md) EPIC-4 M1 预研 |
| 落地页 Agent | `/landing` | ✅ [列表 M1](wireframes/landing-page-list.md) · ✅ [发布 M2](wireframes/landing-page-publish.md) |
| 线索与转化 | `/leads` | ✅ [leads-list.md](wireframes/leads-list.md) FR-601 M1 + **FR-605 M2 CRM** |
| 报告中心 | `/reports` | ✅ [reports-list.md](wireframes/reports-list.md) EPIC-8 M1 |
| 系统设置 | `/settings/*` | ✅ [billing-settings.md](wireframes/billing-settings.md) FR-804 · ✅ [probe-nodes.md](wireframes/probe-nodes.md) FR-113 · 其余沿用若依 |

二级页「问题库 / 探针节点」待后续 Sprint；诊断详情已覆盖「诊断结果」；趋势监控见 [diagnostic-trends.md](wireframes/diagnostic-trends.md)。

---

## 布局约束（继承 plus-ui）

- **导航**：左侧栏 + 顶栏 + TagsView（`settings.ts` 默认 `NavTypeEnum.LEFT`）
- **内容区**：`.p-2` 或 `.app-container`，卡片 `el-card shadow="hover"`
- **列表页模式**：搜索区（可折叠）→ 工具栏 → `el-table border` → 分页（与 `system/user/index.vue` 一致）
- **项目上下文**：顶栏右侧增加 **当前项目** 下拉（占位，绑定 `project_id`；无 API 时用 mock）
- **租户**：沿用若依多租户；不在业务页重复展示 tenant_id

---

## 实现入口（开发参考）

| 项 | 路径 |
|----|------|
| 主题色注入 | `inbound-admin/src/utils/theme.ts` + `settings.ts` `theme` |
| 全局变量 | `inbound-admin/src/assets/styles/variables.module.scss` |
| 替换首页 | `inbound-admin/src/views/index.vue` → 路由指向 `/dashboard` |
| 新视图目录建议 | `inbound-admin/src/views/tourgeo/dashboard/`、`.../diagnostics/` |

Token 落地时优先在 `variables.module.scss` 的 `:root` 追加 `--tg-*` 变量，再通过 `handleThemeStyle('#1677A0')` 同步 Element Plus 主色。

---

## 无障碍（WCAG AA）

- 正文与背景对比度 ≥ **4.5:1**；大号标题（≥18px 或 14px bold）≥ **3:1**
- 状态不仅靠颜色：`el-tag` + 文案（如「执行中」）；进度用 `el-progress` + 百分比
- 表格操作：`el-tooltip` + `aria-label`（plus-ui 惯例）
- 焦点环：不覆盖 Element Plus 默认 focus 样式

---

## 版本

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-07 | UI 设计 | EPIC-7 M2 线索 CRM 增量线框 FR-605 + HANDOFF |
| 2026-07-05 | UI 设计 | EPIC-11 M1 探针节点列表线框 FR-113 + HANDOFF |
| 2026-07-04 | UI 设计 | EPIC-9 M1 套餐与额度设置线框 + 6 quota 进度条 + HANDOFF |
| 2026-07-03 | UI 设计 | EPIC-6 M2 落地页发布/公网预览/Astro 八模块 + Turnstile 线框 |
| 2026-07-02 | UI 设计 | EPIC-8 M1 报告中心列表 + 周报生成 + 下载线框 + HANDOFF |
| 2026-07-01 | UI 设计 | EPIC-7 M1 询盘线索列表 + 详情 drawer 线框 + HANDOFF |
| 2026-07-01 | UI 设计 | EPIC-6 M1 落地页列表 + 创建/预览线框 + HANDOFF |
| 2026-06-29 | UI 设计 | EPIC-4 M1 内容任务列表预研线框 + HANDOFF |
| 2026-06-29 | UI 设计 | EPIC-3 M1 关键词机会词列表线框 FR-201/202 + HANDOFF 开发 |
| 2026-06-29 | UI 设计 | FR-108 诊断趋势对比线框 + HANDOFF |
| 2026-06-29 | UI 设计 | Story 3 项目详情 Tab 线框 + HANDOFF |
| 2026-06-26 | UI 设计 | 初版 token + 工作台 + GEO 列表；EPIC-2 诊断详情线框 |
