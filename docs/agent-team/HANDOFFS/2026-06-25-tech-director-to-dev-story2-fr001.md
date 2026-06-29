# HANDOFF | 技术总监 → 开发

| 字段 | 值 |
|------|-----|
| **From** | 技术总监 |
| **To** | 开发 |
| **日期** | 2026-06-25 |
| **优先级** | High（**门禁**：P0 启动验通后执行） |
| **关联** | EPIC-1 Story 2 · FR-001 · FR-002 · FR-807 |

## 上下文

**当前状态**：Sprint-1 基础设施与若依系统表就绪；Admin 工作台/GEO 页设计已交付。**Story 2** 目标：EPIC-1 最小闭环 —— **登录 → 创建项目 → 项目列表**。

**前置门禁**：
- [x] [ops→dev HANDOFF](2026-06-25-ops-to-dev-pg-ruoyi.md) P0 完成（`spring-boot:run` + Admin 登录）✅ 2026-06-25 22:41
- [ ] UI 交付 `docs/design/wireframes/projects-list.md`（可并行，字段以 DDL 为准）

**相关文件**：
- `database/ddl/001_schema.sql` → `customer_project`、`competitor`
- `PRD_商业化版_V2.0.md` FR-001、FR-002、FR-807
- `docs/ARCHITECTURE.md` — project-service 模块
- `inbound-core/` — 新增 `project` 包（Controller → Service → Mapper）

**约束**：
- Java 层 CRUD + 租户隔离（`tenant_id` MyBatis-Plus TenantLineHandler）
- 不跳过 EPIC-1 直接做 GEO API
- API 前缀 `/api/v1`；响应 `{ code, message, data, trace_id }`

## 交付请求

**需要什么**：实现客户项目 CRUD API + Admin 项目列表/创建页，完成 EPIC-1 最小闭环验收。

**验收标准**：

### 后端（Java）
- [x] `POST /api/v1/projects` — 创建项目（FR-001 字段）
- [x] `GET /api/v1/projects` — 分页列表（tenant 隔离）
- [x] `GET /api/v1/projects/{id}` — 详情
- [x] `PUT /api/v1/projects/{id}` — 更新基础信息
- [x] 跨 tenant 访问返回 403（集成测试或手工用例）— 代码层 `getOwnedProjectOrThrow`；待第二租户补自动化
- [x] Entity/Mapper 与 `customer_project` DDL 字段一致

### 前端（Admin）
- [x] 路由 `/projects` 列表 + 创建（按 UI 线框）
- [x] 顶栏「当前项目」下拉绑定真实 API
- [x] 工作台无项目时空态跳转创建流

### 联调
- [x] Admin 登录 → 创建项目 → 列表可见 → 工作台选中该项目
- [x] 更新 `MEMORY.md` 开发章节 + 本文件 Done 段

## 质量 / 证据

**必须提供**：
- curl 或 Postman 创建/列表示例
- Admin 截图：列表 + 创建成功
- 租户隔离测试说明

**交给下一棒**：EPIC-10 AI 编排 / EPIC-2 GEO（技术总监排期）

---

## Done（由 To 角色填写）

- **完成时间**：2026-06-25 23:15
- **结果摘要**：
  - **后端**：新增 `ruoyi-modules/ruoyi-project`；`CustomerProjectController` 暴露 `/api/v1/projects` CRUD + `/options`；租户隔离 `tenant_id=1`（RuoYi `000000` → `BusinessTenantHelper`）；软删 `deleted_at`；PG 适配 `PgJsonbListTypeHandler` / `PgEntityStatusTypeHandler` / `OffsetDateTime`
  - **Admin**：`/projects/index` 列表+搜索+创建/编辑抽屉；顶栏 `ProjectSelector` 接 `/options`；工作台无项目 CTA → `/projects?create=1`
  - **验通**：`deploy/scripts/test_projects_api.py` — login → options(1 seed) → list → POST id=2 → GET detail
- **遗留**：
  - 统一响应格式仍为 RuoYi `R`/`TableDataInfo`（非 ARCHITECTURE `code=0` + `trace_id`），后续 Story 可封装
  - `travel_product` / 竞品子资源 API 未做（FR-002 后续）
  - 跨 tenant 403 需第二租户数据后补集成测试
