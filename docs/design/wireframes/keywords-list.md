# 线框：关键词机会词列表（FR-201 / FR-202）

> **PRD**：§8.4 关键词机会识别 · **FR-201** 关键词生成 · **FR-202** 生命周期词库（八阶段）  
> **EPIC**：EPIC-3 M1  
> **路由**：`/projects/:projectId/keywords`（推荐）；侧栏可挂 `/keywords` 并重定向到当前项目  
> **数据表**：`keyword_opportunity`（`001_schema.sql`）

---

## 页面目标

在项目上下文中浏览、筛选 **AI 生成的海外关键词机会词**，按用户生命周期 **八阶段** 归类展示；支持一键 **AI 生成机会词**（FR-201），M1 以列表为主，无详情抽屉。

**入口**：
- 侧栏「关键词洞察」→ 机会词列表（需已选当前项目）
- [project-detail.md](./project-detail.md) 顶部快捷链「关键词机会 →」（P2，与 GEO 诊断并列）
- 工作台 KPI「待覆盖关键词」链到本页（P2）

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 面包屑：关键词洞察 / 机会词列表     当前项目 ▼ Dragon Journey Travel        │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─ 项目选择器 (el-card shadow="never", 与趋势页一致) ──────────────────────┐ │
│ │ 客户项目 [Dragon Journey Travel ▼]  目标市场 US UK AU  品牌 Dragon…     │ │
│ │ 切换项目 → router.replace(`/projects/{id}/keywords`) + 刷新列表         │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 工具栏 (el-card) ──────────────────────────────────────────────────────┐ │
│ │ [AI 生成机会词]  primary          市场 [全部▼]  关键词 [____] [搜索][重置]│ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 八阶段 Tab (el-tabs type="card" 或 border-card) ───────────────────────┐ │
│ │ [全部] [灵感] [种草] [比较] [签证] [规划] [信任] [决策] [复购]          │ │
│ │  Tab 右侧 badge：各阶段词数（可选，来自 list 聚合或前端 count）          │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ el-table border v-loading ─────────────────────────────────────────────┐ │
│ │ 关键词 | 英文 | 中文释义 | 阶段 | 市场 | 机会分 | 状态 | 创建时间 | 操作 │ │
│ │ Chongqing cyberpunk… | Chongqing… | 重庆赛博朋克… | 灵感 | US | 88.5 | 正常 │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                              [分页 10/20/50]                                │
│ ℹ M1 机会分为占位展示；FR-203 评分规则上线后替换 tooltip 说明。              │
└─────────────────────────────────────────────────────────────────────────────┘

生成确认 (el-message-box 或 el-dialog 480px):
┌─ AI 生成机会词 ─────────────────────────────────────── [×] │
│ 将为当前项目生成各生命周期阶段的推荐关键词（FR-201/202）。    │
│ 目标市场：US、UK、AU（来自项目 target_markets_json）         │
│ 预计每阶段 ≥10 条；已有词条默认保留，新生成追加。             │
│ ⚠ 消耗套餐「关键词/月」额度（若计费已启用）。                 │
│                                    [取消]  [开始生成]        │
└─────────────────────────────────────────────────────────────┘

生成中（按钮 + 表格区）:
  主按钮 [AI 生成机会词] → loading + disabled
  表格 v-loading 文案「AI 正在生成关键词，约 30–90 秒…」
  可选 el-alert type="info" 顶部「生成任务进行中，请勿关闭页面」
```

---

## 八阶段枚举对照表（FR-202）

与 PRD §10.4、`inbound-admin/src/constants/diagnostic.ts` → `LIFECYCLE_STAGE_LABELS` **保持一致**：

| DB / API `stage` | Tab 中文 | PRD 阶段名 | 用户问题示例（Tooltip） |
|------------------|----------|------------|-------------------------|
| *(空 / 不传)* | **全部** | — | 展示所有阶段 |
| `inspiration` | 灵感 | 灵感期 | China is becoming popular, where should I go? |
| `planting` | 种草 | 种草期 | I saw Chongqing on TikTok, is it worth visiting? |
| `comparison` | 比较 | 比较期 | Private tour or group tour in China? |
| `visa` | 签证 | 签证期 | Can I visit China visa-free? |
| `planning` | 规划 | 规划期 | 10-day China itinerary for first timers. |
| `trust` | 信任 | 信任期 | Best China travel agency for foreigners. |
| `decision` | 决策 | 决策期 | How much does a private China tour cost? |
| `repurchase` | 复购 | 复购期 | Where to go after Beijing and Shanghai? |

**Tab 行为**：
- 默认激活「全部」；切换 Tab 带 query `?stage=inspiration`（便于分享链接）
- Tab 切换 → 重新请求 list API，`stage` 参数过滤
- 某阶段 0 条时 Tab 仍展示，badge 显示 `0`（灰色）

---

## 项目选择器

| 项 | 规范 |
|----|------|
| 组件 | `el-select` filterable，宽度 280px；右侧只读 Tag 展示项目 `targetMarkets` |
| 数据源 | `GET /api/v1/projects` 或 Pinia `projectStore` 已加载列表 |
| 默认 | 路由 `:projectId` > 顶栏当前项目 > 列表第一项 |
| 切换 | `router.replace({ name: 'KeywordsList', params: { projectId } })` + 重置 Tab 为「全部」 |
| 无项目 | 全页 `el-empty`「请先创建客户项目」→ `/projects` |

与 [diagnostic-trends.md](./diagnostic-trends.md) 顶栏「当前项目 ▼」**二选一实现**：本页以 **卡片内 el-select** 为主（路由含 `projectId`）；顶栏下拉切换时同步跳转本路由。

---

## 筛选栏（工具栏内联）

| 字段 | 组件 | 查询参数 | 说明 |
|------|------|----------|------|
| 市场 | `el-select` | `market` | 选项来自当前项目 `targetMarkets` +「全部」 |
| 关键词 | `el-input` clearable | `keyword` | 模糊匹配 `keyword` / `keyword_en` / `keyword_cn` |
| 状态 | `el-select` | `status` | M1 默认仅 `ACTIVE`；可选展示 INACTIVE（P2） |

Tab `stage` 与上表 **AND** 组合。

---

## 表格列 ↔ DDL / API

| 列 | 宽度 | DDL 列 | API 字段 | 展示 |
|----|------|--------|----------|------|
| 关键词 | min 200 | `keyword` | `keyword` | 主文案，`show-overflow-tooltip`；英文为主搜索词 |
| 英文 | min 180 | `keyword_en` | `keywordEn` | 可与 keyword 相同；空则 `—` |
| 中文释义 | min 160 | `keyword_cn` | `keywordCn` | 空则 `—` |
| 阶段 | 100 | `stage` | `stage` | `el-tag` 映射八阶段中文（见上表） |
| 市场 | 80 | `market` | `market` | `el-tag size="small"`（US、UK…） |
| 机会分 | 100 | `score` | `score` | **M1 占位**：`el-progress` 或数字 + 色阶；`score` 为空显示 `—` + tooltip「评分规则 FR-203 待上线」 |
| 状态 | 90 | `status` | `status` | `entity_status` Tag |
| 创建时间 | 160 | `created_at` | `createdAt` | `YYYY-MM-DD HH:mm` |
| 操作 | fixed right 120 | — | — | M1 仅「删除」；「转内容任务」disabled + tooltip FR-205 |

### `entity_status`（`status` 列）

| 值 | Tag type | 中文 |
|----|----------|------|
| `ACTIVE` | `success` | 正常 |
| `INACTIVE` | `info` | 停用 |
| `SUSPENDED` | `warning` | 暂停 |
| `ARCHIVED` | `info` | 已归档 |

### 机会分色阶（M1 占位，对齐 tokens）

| 分数 | 色 | Token |
|------|-----|-------|
| ≥ 80 | 绿 | `--tg-score-high` |
| 50–79 | 琥珀 | `--tg-score-mid` |
| &lt; 50 | 红 | `--tg-score-low` |

Demo 种子：`Chongqing cyberpunk city tour` · stage=`inspiration` · score=`88.5` · market=`US`。

### 行操作（M1）

| 操作 | 组件 | 行为 |
|------|------|------|
| 删除 | `el-button link` type="danger" | `ElMessageBox.confirm` → `DELETE`（软删）→ 刷新列表 |
| 转任务 | `el-button link` disabled | tooltip「内容任务转化 FR-205」 |

**M1 不做**：详情抽屉、行内编辑、批量导出、渠道列（`channel` 列 FR-204 P1 再开）。

---

## 「AI 生成机会词」主流程（FR-201）

| 步骤 | UI | API |
|------|-----|-----|
| 1 点击 | 主按钮 `type="primary"` icon `MagicStick` 或 `Promotion` | — |
| 2 确认 | `ElMessageBox.confirm` 或 dialog（见 ASCII） | — |
| 3 生成中 | 按钮 `loading`；表格 `v-loading`；禁用 Tab 切换（可选） | `POST /api/v1/projects/{projectId}/keywords/generate` |
| 4 成功 | `ElMessage.success`「已生成 N 条关键词」；刷新 list；切到「全部」Tab | 响应 `data: { count }` 或异步 taskId（若 Java 202） |
| 5 失败 | `ElMessage.error` 展示 `message`；保留列表 | 402 额度不足单独文案 |

**请求体建议（M1）**：

```json
{
  "markets": ["US", "UK"],
  "stages": ["inspiration", "planting", "comparison", "visa", "planning", "trust", "decision", "repurchase"],
  "perStageMin": 10
}
```

未传时后端用项目默认市场 + 全八阶段。M1 前端可只 POST `{}` 由 Java 填默认。

**生成中 loading 文案**：`AI 正在生成关键词，约 30–90 秒…`（与 GEO 诊断 RUNNING 风格一致，`--tg-color-running` 动画点可选）。

---

## 空 / 加载 / 错误态

| 状态 | UI |
|------|-----|
| 首次加载 | 表格 `v-loading` |
| 无词条（全项目） | `el-empty` illustration + description「暂无关键词机会词」+ 主按钮「AI 生成机会词」 |
| 某阶段 Tab 无数据 | 表格区 `el-empty`「该阶段暂无词条」+ 次按钮「AI 生成机会词」 |
| 搜索无结果 | `el-empty`「未找到匹配关键词」+ link「清除筛选」 |
| 生成失败 | `ElMessage.error` + 按钮恢复可点 |
| 未选项目 | 全页 empty → 跳转 `/projects` |
| 402 套餐超额 | `ElMessage.warning`「关键词额度不足，请联系管理员」 |

---

## 与其他页面联动

| 场景 | 行为 |
|------|------|
| 顶栏当前项目 | 与 `:projectId` 双向同步 |
| 项目详情 | 快捷入口「关键词机会 →」带 `projectId` |
| 内容 Agent（EPIC-4） | FR-205 启用后，行操作「转内容任务」→ `/content/tasks?keywordId=` |
| 落地页 Agent（EPIC-6） | 同上转落地页任务（P2） |

---

## 响应式

| 断点 | 行为 |
|------|------|
| ≥1200px | 全列 |
| 768–1199px | 隐藏「中文释义」「创建时间」 |
| &lt;768px | 仅关键词、阶段、机会分、操作；八阶段 Tab 横向 scroll |
| &lt;768px 生成 | 确认 dialog `width="92%"` |

---

## 菜单与路由（若依）

```text
父菜单：关键词洞察 (path: /keywords, icon: key 或 search)
  └─ 机会词列表 (path: /projects/:projectId/keywords, component: tourgeo/keywords/index)
```

**路由 meta**：`title: 机会词列表`，`activeMenu: /keywords`。

**重定向**：访问 `/keywords` 无 `projectId` 时 → `projectStore.currentProjectId` 或 toast「请先选择项目」。

---

## 关联 API（EPIC-3 M1）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/projects/{projectId}/keywords` | 分页列表 `?stage=&market=&keyword=&status=&pageNum=&pageSize=` |
| POST | `/api/v1/projects/{projectId}/keywords/generate` | FR-201 生成 |
| DELETE | `/api/v1/projects/{projectId}/keywords/{id}` | 软删（若 Java 提供） |

响应 `{ code, message, data, trace_id }`；列表 `data.rows` + `data.total`。

Python 内网：`POST /ai/keywords/generate`（Java 代理，开发见 EPIC-3 AI HANDOFF）。

---

## 实现参考

- 列表模式：`inbound-admin/src/views/tourgeo/projects/index.vue`、`diagnostics/index.vue`
- 项目选择器：[diagnostic-trends.md](./diagnostic-trends.md) 筛选栏
- 八阶段常量：复用或抽取 `LIFECYCLE_STAGE_LABELS` → `constants/keyword.ts`
- Token / Tag / 空态：`docs/design/tokens.md` §1.3、§5

---

## M1 范围外（明确不做）

| FR | 说明 |
|----|------|
| FR-203 | 机会评分规则 — 列占位 + tooltip |
| FR-204 | 渠道建议列 |
| FR-205 | 转内容/落地页任务 |
| FR-206/207 | 跨市场模板、效果回传 |

---

## 版本

| 日期 | 说明 |
|------|------|
| 2026-06-29 | 初版 EPIC-3 M1 列表 + 八阶段 Tab + AI 生成线框 |
