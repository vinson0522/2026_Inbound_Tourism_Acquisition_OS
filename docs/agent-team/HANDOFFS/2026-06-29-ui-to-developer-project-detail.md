# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-06-29 |
| **优先级** | High |
| **关联** | Story 3 · FR-002/003/004 · EPIC-1 扩展 |

## 上下文

**当前状态**：M1 已有 `/projects/index` 列表（FR-001）。Story 3 详情页线框已输出，三 Tab 字段对齐 DDL。

**相关文件**：
- `docs/design/wireframes/project-detail.md` — 线框 + DDL/API 对照
- `docs/design/wireframes/projects-list.md` — 列表入口与创建流
- `docs/design/tokens.md` — Tag / 表单密度
- `database/ddl/001_schema.sql` — `travel_product`、`competitor`、`knowledge_asset`
- `inbound-admin/src/views/tourgeo/projects/index.vue` — 列表改跳转

**约束**：
- 不阻塞 EPIC-10：知识库「检索预览」disabled + tooltip
- 竞品 FR-002：UI 提示 ≥5 个，不硬拦提交
- 上传走 Java → MinIO → MQ embed（后端实现）；前端仅 202 + 状态轮询
- 租户隔离 FR-807；字段 camelCase 与 Story 2 项目 API 一致

## 交付请求

**需要什么**：实现 `/projects/:projectId` 详情页（Tab：品牌信息 / 竞品 / 知识库），列表「进入/编辑」打通。

**验收标准**：
- [ ] 隐藏路由 + `activeMenu`；页头摘要 + 返回列表
- [ ] Tab 品牌：编辑 `customer_project` + 路线 `travel_product` CRUD 表格/抽屉
- [ ] Tab 竞品：`competitor` CRUD + 社媒 JSON 表单 + ≥5 提示
- [ ] Tab 知识库：上传 dialog + 列表 + `vector_status` Tag；FR-005 预览 disabled
- [ ] URL `?tab=brand|competitors|knowledge` 同步
- [ ] 「设为当前项目」「进入工作台」接 `projectStore`
- [ ] 空/加载/404 态

## 实现顺序建议

1. 路由 + 页壳 + GET project 回填 Tab1
2. Java API：products / competitors / knowledge（可分期 PR）
3. Admin 三 Tab 逐个接 API
4. 列表 `进入` → `ProjectDetail`

## 质量 / 证据

**必须提供**：三 Tab 截图 + 添加竞品/路线/上传各 1 条成功路径

---

## Done（由 To 角色填写）

- **完成时间**：2026-06-29
- **结果摘要**：`/projects/:projectId` 三 Tab；Java `products`/`competitors` CRUD + knowledge 更新/删除/筛选；列表「进入」→ ProjectDetail
- **遗留**：FR-005 检索预览 disabled；PDF 上传走 OSS + embed MQ（需本机 OSS 配置）
- **Admin 走查（2026-06-29）**：四路径 OK；dashboard「查看」已修 `goRunDetail`

**走查步骤**（前提：`pnpm dev` + Java :8080 + 登录 admin/admin123）：
1. `/dashboard` — 选项目 → 见 GEO KPI + 最近 5 条诊断 → 点「查看」进详情
2. `/projects/index` — 点「进入」→ `/projects/1?tab=brand` → 切换竞品/知识库 Tab
3. `/diagnostics/runs` — 「新建诊断」→ 提交 → 详情四 Tab
4. 详情页 SUCCESS 任务 → 「导出 DOCX」→ 浏览器下载 `.docx`
