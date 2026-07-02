# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-07-10 |
| **优先级** | High |
| **关联** | EPIC-2 M3 · FR-109 · [技术总监 → UI](2026-07-10-tech-director-to-ui-epic2-schedule-m3.md) · ADR-20260710-26 |

## 上下文

**当前状态**：M1 `/diagnostics` 列表 + 新建 drawer ✅。M3 增加页级「定时计划」Tab，每项目单条 schedule upsert。

**相关文件**：
- `docs/design/wireframes/diagnostics-list.md` — **§M3 增量**（Tab · 表单 · 只读区 · footnote）
- M1 实现：`inbound-admin/src/views/tourgeo/diagnostics/index.vue`
- Java HANDOFF：`2026-07-10-tech-director-to-dev-java-epic2-schedule-m3.md`
- Admin HANDOFF：`2026-07-10-tech-director-to-dev-admin-epic2-schedule-m3.md`

**约束**：
- 每项目 **1 条** schedule（ADR-26）
- 触发复用 `createRun` · 无任务名字段 · Job 自动命名
- 额度不足 **skip** · UI 不弹错 · footnote 说明
- M3 **无** 邮件/企微/cron 编辑器/多计划列表
- grounded-api 合规 hint 保留 · 不展示缓存类文案

## 交付请求

**需要什么**：在现有诊断列表页叠加「定时计划」Tab + schedule CRUD UI。

**验收标准**：
- [ ] 页级 `el-tabs` — **诊断任务**（M1 不变）| **定时计划**
- [ ] 定时 Tab — `enabled` switch · `WEEKLY`/`MONTHLY` radio
- [ ] 表单复用新建 drawer 字段（市场/语言/问题范围/探针/平台/采样/校准）· **无任务名称**
- [ ] 只读区 — `nextRunAt` · `lastTriggeredAt` · `lastRunId` 链详情页
- [ ] `enabled=false` 时下次执行显示「已暂停」
- [ ] `GET/PUT .../diagnostics/schedule` · 保存 toast · loading 态
- [ ] footnote 卡 —  hourly Job · 超额 skip · 不承诺排名 · 无通知
- [ ] info alert — 与新建 drawer 相同合规文案
- [ ] 只读角色 — 表单 disabled · 无「保存计划」
- [ ] `diagnostic.ts` + types 增补 · `pnpm build:prod` ✅

## 后端依赖

- [ ] DDL `diagnostic_schedule` + `GET/PUT .../schedule`
- [ ] `DiagnosticScheduleJob` 每小时 due → `createRun`
- [ ] smoke `deploy/scripts/test_diagnostic_schedule.py`

## API 封装建议（`diagnostic.ts`）

```typescript
getDiagnosticSchedule(projectId: number)
putDiagnosticSchedule(projectId: number, data: DiagnosticScheduleForm)
```

## 质量 / 证据

**必须提供**：
- Tab 切换截图（列表 Tab + 定时 Tab）
- enabled 开/关 + WEEKLY/MONTHLY 表单截图
- 只读区 nextRunAt / lastRun 链接截图
- footnote 超额跳过说明可见截图
- 只读角色无保存按钮截图

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
