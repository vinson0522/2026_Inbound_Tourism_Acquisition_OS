# HANDOFF | UI 设计 → 开发

| 字段 | 值 |
|------|-----|
| **From** | UI 设计 |
| **To** | 开发 |
| **日期** | 2026-07-09 |
| **优先级** | High |
| **关联** | EPIC-11 M2 · FR-115/116 · [技术总监 → UI](2026-07-09-tech-director-to-ui-epic11-probe-m2.md) · ADR-20260709-22 |

## 上下文

**当前状态**：M1 `/settings/probe-nodes` 只读节点列表 ✅ · 扩展 Perplexity poll ✅。M2 需运营编辑 platform adapter + 诊断详情查看 API vs 扩展校准。

**相关文件**：
- `docs/design/wireframes/probe-adapters.md` — **新建** FR-116 列表 + 编辑 drawer + JSON 三块
- `docs/design/wireframes/diagnostic-detail.md` — **§M2** 校准对比 Tab
- `docs/design/wireframes/probe-nodes.md` — 设置页版式 · 平台 Tag 映射
- `docs/design/wireframes/diagnostics-list.md` — 创建表单 `calibration_ratio` 滑块（已有 UI 则接后端）
- M1 实现：`settings/probe-nodes/index.vue` · `diagnostics/detail.vue`
- Java HANDOFF：`2026-07-09-tech-director-to-dev-java-epic11-probe-m2.md`
- Admin HANDOFF：`2026-07-09-tech-director-to-dev-admin-epic11-probe-m2.md`

**约束**：
- M2 无 adapter 版本灰度 · 节点 CRUD · FR-117 截图 · Headless
- 校准 Tab **仅当** `calibrationRatio > 0` 且探针含 grounded-api + browser-extension
- JSON 编辑 M2 用 textarea + 格式化/校验（非 Monaco）
- 扩展 poll 路径 `/probe/adapters` 不变；Admin 用 `/settings/platform-adapters`

## 交付请求

**需要什么**：平台 Adapter 设置页 + 诊断详情校准 Tab + 创建诊断 calibration_ratio 联调。

### A. 平台 Adapter `/settings/probe-adapters`

**验收标准**：
- [ ] 侧栏「系统设置 → 平台 Adapter」
- [ ] 概览：已配置/启用/停用 + link 探针节点
- [ ] 表格：platform Tag、version、enabled、updatedAt、编辑/复制 JSON
- [ ] 编辑 drawer 640px：enabled switch · dom/api/parse 三块 JSON textarea · 格式化/校验
- [ ] `GET/PUT /api/v1/settings/platform-adapters/{platform}` · 保存 toast
- [ ] 合规 info alert footnote
- [ ] 只读角色禁用保存 · 新增平台 disabled
- [ ] 权限 `tourgeo:probe:adapter:edit`

### B. 诊断详情校准 Tab

**验收标准**：
- [ ] Tab「校准对比」可见性规则（见线框 §M2）
- [ ] `GET .../diagnostics/{runId}/calibration` · KPI：deviationRate · brandMentionAgreementRate · pairedCount
- [ ] 重叠问题对比表 + 行展开双栏（grounded-api vs browser-extension 答案摘要）
- [ ] 任务头展示 calibration_ratio（>0 时）
- [ ] footnote 链 `/settings/probe-adapters`
- [ ] 空态：未完成 / 无 pairs / 扩展全失败

### C. 创建诊断（若尚未接后端）

**验收标准**：
- [ ] `calibration_ratio` 滑块 0–30% 提交至 `POST .../diagnostics`
- [ ] 未选 browser-extension 时滑块 disabled + tooltip
- [ ] `pnpm build:prod` ✅

## 后端依赖

- [ ] `GET /api/v1/settings/platform-adapters` · GET/PUT `/{platform}`
- [ ] `GET /api/v1/projects/{projectId}/diagnostics/{runId}/calibration`
- [ ] `createRun`：`calibration_ratio`>0 双模式重叠抽样
- [ ] seed `chatgpt` adapter v1.0

## API 封装建议（`probe.ts` / `diagnostic.ts`）

```typescript
listPlatformAdapters()
getPlatformAdapter(platform)
savePlatformAdapter(platform, body)
getDiagnosticCalibration(projectId, runId)
```

## 质量 / 证据

**必须提供**：
- probe-adapters 编辑保存 + JSON 校验截图
- 校准 Tab KPI + 展开双栏对比截图
- calibration_ratio=0 时 Tab 隐藏截图
- ChatGPT adapter 列表行（seed 后）

---

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
