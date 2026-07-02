# 线框：内容 Agent · 爆款拆解（EPIC-5 M1）

> **PRD**：§8.6 爆款拆解与素材 · **FR-401** 上传 · **FR-402** 拆帧 · **FR-403** 七维拆解 · **FR-405** 版权提示（M1 仅展示）  
> **EPIC**：EPIC-5 M1 · **ADR-20260709-24**  
> **路由**：`/projects/:projectId/materials`（侧栏 `/materials` 重定向当前项目）  
> **数据表**：`material_asset` · `video_breakdown`（`frames_json` · `dimensions_json` · `reusable_structure`）

---

## 页面目标

在项目上下文中 **上传参考视频/截图**，触发 **拆帧 + 七维结构拆解**（借鉴结构、非搬运），查看拆帧网格与七维分析表，供内容 Agent 创作参考。

**M1 范围**：
- ✅ 列表 + 上传 dialog + 「开始拆解」+ 拆解状态 Tag
- ✅ 详情 drawer：七维表 + frames 缩略图网格 + `reusable_structure`
- ✅ 版权合规 footnote（FR-405 · M1 不拦截导出）
- ❌ FR-404 素材标签库 · FR-406 智能推荐 · PySceneDetect · 外链抓取 · 批量导入

**入口**：
- 侧栏「内容 Agent → 爆款拆解」
- [content-task-list.md](./content-task-list.md) 占位菜单启用（M2 链「引用拆解结构」P2）
- 工作台「待拆解素材」→ 本页（P2）

**权限**：`tourgeo:material:list` · 上传/拆解 `tourgeo:material:edit`

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 面包屑：内容 Agent / 爆款拆解        当前项目 ▼ Dragon Journey Travel       │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─ 项目选择器 (同 content-task-list) ──────────────────────────────────────┐ │
│ │ 客户项目 [Dragon Journey ▼]   目标市场 US UK AU                          │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 搜索区 (el-card, 可折叠) ───────────────────────────────────────────────┐ │
│ │ 类型 [全部▼]  拆解状态 [全部▼]  版权 [全部▼]  创建时间 [日期范围] [搜索]   │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 工具栏 ─────────────────────────────────────────────────────────────────┐ │
│ │ [上传素材] primary   [刷新]                          [显示搜索]          │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ el-table border v-loading ──────────────────────────────────────────────┐ │
│ │ 缩略图 | 素材 | 类型 | 版权 | 拆解状态 | 帧数 | 创建时间 | 操作            │ │
│ │ [img]  viral-cn-tour.mp4 | 视频 | 外部 | 已完成 | 6 | 07-09 14:20 | 详情  │ │
│ │ [img]  hook-screenshot.png | 图片 | 未知 | 未拆解 | — | … | 开始拆解      │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                              [分页 10/20/50]                                │
│ ┌─ 版权 el-alert type="warning" show-icon :closable="false" ───────────────┐ │
│ │ 外部爆款素材仅供团队内部结构学习，请勿直接搬运发布。用于客户交付或对外导出  │ │
│ │ 前请确认版权归属。（FR-405 · M1 提示不拦截）                               │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘

上传素材 (el-dialog 520px · FR-401):
┌─ 上传参考素材 ──────────────────────────────────────────── [×] │
│ [el-upload drag]  拖拽或点击上传                                │
│ 支持：MP4 / MOV / WebM · JPG / PNG / WebP · 单文件 ≤ 200MB     │
│ 素材类型        [视频 ▼]  (自动识别可改)                       │
│ 版权状态        [外部参考 unknown ▼]  见 copyright 映射       │
│ 来源说明        [TikTok @creator / 竞品广告…]  可选             │
│ ℹ 上传后保存至项目素材库，可发起七维拆解。                       │
│                              [取消]  [上传] primary             │
└─────────────────────────────────────────────────────────────────┘

拆解详情 (el-drawer 720px · FR-402/403):
┌─ 拆解详情 · viral-cn-tour.mp4 ──────────────────────────── [×] │
│ [16:9 预览区 el-image 或 video 控件]  Tag 视频 · 6 帧          │
│ [待人工确认 Tag warning] needs_human_review                     │
│ ── 七维拆解 (el-descriptions :column="1" border) ──           │
│ 主题 theme        │ 中国入境游·首次自由行·信任建立              │
│ 钩子 hook         │ 3 秒城市天际线 + 反差文案…                  │
│ 镜头 shot         │ 快切 B-roll + 手持 POV…                     │
│ 字幕 subtitle     │ 英文大标题 + 关键词高亮…                    │
│ 情绪 emotion      │ 好奇 → 向往 → 安全感                        │
│ 心理 psychology   │ FOMO · 社会证明 · 降低决策风险              │
│ 可复用结构 reusable│ 问题-证据-CTA 三段式…                     │
│ ── 可复用结构摘要 (reusable_structure) ──                       │
│ [el-alert type="info"] 纯文本段落…                              │
│ ── 拆帧网格 (frames_json) el-row :gutter="8" ──                 │
│ [thumb 0:00] [thumb 0:05] [thumb 0:10] …  caption tooltip       │
│ [关闭]  [重新拆解] P2  [转为内容参考] disabled FR-406           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 项目选择器

与 [content-task-list.md](./content-task-list.md) **一致**：

| 项 | 规范 |
|----|------|
| 路由 | `/projects/:projectId/materials` |
| 切换 | `router.replace({ name: 'MaterialList', params: { projectId } })` + 重置筛选 |
| 无项目 | `el-empty`「请先创建客户项目」→ `/projects` |

---

## 筛选栏

| 字段 | 组件 | 查询参数 | 说明 |
|------|------|----------|------|
| 类型 | `el-select` | `type` | `material_asset.type` · IMAGE / VIDEO |
| 拆解状态 | `el-select` | `breakdownStatus` | 见下表 · 后端 join 最新 `video_breakdown` |
| 版权状态 | `el-select` | `copyrightStatus` | `copyright_status` |
| 创建时间 | `el-date-picker` daterange | `createdAt` | |

---

## 表格列 ↔ DDL / API

| 列 | 宽度 | DDL / 来源 | API 字段 | 展示 |
|----|------|------------|----------|------|
| 缩略图 | 80 | `thumbnail_url` 或视频首帧 | `thumbnailUrl` | `el-image` 64×64 fit cover · lazy |
| 素材 | min 180 | `url` 文件名 | `fileName` / `url` | 主文案 + tooltip 完整 URL |
| 类型 | 90 | `type` | `type` | Tag 中文 |
| 版权 | 100 | `copyright_status` | `copyrightStatus` | Tag |
| 拆解状态 | 110 | 计算 / join | `breakdownStatus` | Tag · 见下 |
| 帧数 | 70 | `frames_json` length | `frameCount` | 数字或 `—` |
| 创建时间 | 160 | `created_at` | `createdAt` | `YYYY-MM-DD HH:mm` |
| 操作 | fixed 180 | — | — | 见行操作 |

### `material_asset_type` Tag

| DB 值 | Tag type | 中文 |
|-------|----------|------|
| `VIDEO` | `primary` | 视频 |
| `IMAGE` | `success` | 图片 |
| `AUDIO` | `info` | 音频 |
| `OTHER` | `info` | 其他 |

M1 上传仅 **VIDEO / IMAGE**；列表可显示 AUDIO 预留。

### `copyright_status` Tag

| 值 | Tag type | 中文 |
|----|----------|------|
| `unknown` | `info` | 未知 |
| `licensed` | `success` | 已授权 |
| `external` | `warning` | 外部参考 |
| `owned` | `success` | 自有素材 |

默认上传 `external` 或 `unknown`；**外部参考** 行可选浅黄底提示（P2）。

### 拆解状态 `breakdownStatus`（列表 join 最新 breakdown）

| 值 | Tag type | 中文 | 条件 |
|----|----------|------|------|
| `NONE` | `info` | 未拆解 | 无 `video_breakdown` 记录 |
| `PROCESSING` | `primary` | 拆解中 | 已 POST breakdown · `dimensions_json` 空 · 无失败标记 |
| `SUCCESS` | `success` | 已完成 | `dimensions_json` 非空 |
| `FAILED` | `danger` | 失败 | callback 失败或超时（Java 暴露 `breakdownError` P2） |

**PROCESSING 行**：可选 `el-progress` indeterminate；列表 **5s 轮询** 或手动刷新。

---

## 行操作（M1）

| 操作 | 条件 | 行为 |
|------|------|------|
| **开始拆解** | `breakdownStatus=NONE` 或 FAILED · **VIDEO** | `POST .../materials/{id}/breakdown` → toast「拆解任务已提交」→ 状态 PROCESSING |
| **查看拆解** | SUCCESS | 打开详情 drawer · `GET .../breakdowns/{breakdownId}` |
| **继续等待** | PROCESSING | disabled + tooltip「拆解进行中」 |
| 预览 | 全部 | 新 tab 打开 `url`（MinIO 签名 URL） |
| 删除 | — | M1 **不提供**（审计保留 · P2 软删） |

**IMAGE 素材**：M1「开始拆解」disabled · tooltip「M1 优先支持视频；图片单帧拆解 M2」  
（若 Java 支持图片：单帧 mock 可启用 — 以 smoke 为准，UI 可不禁用 IMAGE）

---

## 上传 Dialog（FR-401）

| UI 标签 | 组件 | API | 必填 | 说明 |
|---------|------|-----|:----:|------|
| 文件 | `el-upload` drag · `:limit="1"` | `file` multipart | ✅ | 见格式限制 |
| 素材类型 | `el-select` | `type` | — | 自动 MIME 识别 · VIDEO/IMAGE |
| 版权状态 | `el-select` | `copyrightStatus` | — | 默认 `external` |
| 来源说明 | `el-input` | `source` | — | max 200 |

**提交**：`POST /api/v1/projects/{projectId}/materials` · `Content-Type: multipart/form-data`

**成功**：201 + `materialId` → 关闭 dialog → 刷新列表 → toast「素材已上传」

**校验**：
- 扩展名白名单 · 单文件 ≤ 200MB（与 Java 对齐）
- 上传中按钮 loading · 失败保留表单

**402 额度**（P2）：若后续加 `materials_per_month` quota · 同 billing 拦截文案。

---

## 开始拆解（FR-402 / FR-403 触发）

| 项 | 规范 |
|----|------|
| API | `POST /api/v1/projects/{projectId}/materials/{materialId}/breakdown` |
| 响应 | 202 Accepted + `{ breakdownId }` |
| 后端 | 写 `video_breakdown`（`source_url` = material.url）· MQ `ai.breakdown` |
| UI | 行状态 → PROCESSING · 可选自动打开 drawer 显示 loading |

**重复拆解**：M1 允许 FAILED 重试；SUCCESS 行「重新拆解」P2 disabled + tooltip。

---

## 详情 Drawer（FR-402 / FR-403）

**触发**：行「查看拆解」或 PROCESSING 完成自动刷新后点击。

**宽度**：`720px` · `destroy-on-close` · Tab 可选（M1 单页滚动即可）。

### 头部

| 元素 | 来源 | 展示 |
|------|------|------|
| 预览 | `source_url` / material | VIDEO：`video` 控件或封面；IMAGE：`el-image` |
| 文件名 | material | drawer 标题 |
| 拆解状态 | `breakdownStatus` | Tag |
| 人工确认 | analyze 响应 | `needsHumanReview=true` → Tag「待人工确认」warning |

### 七维拆解表

`dimensions_json` 键与 PRD **七维度** 对齐（Python analyze 输出）：

| 键 | 中文标签 | 展示 |
|----|----------|------|
| `theme` | 主题 | `el-descriptions-item` 全文 |
| `hook` | 钩子 | |
| `shot` | 镜头 | |
| `subtitle` | 字幕 | |
| `emotion` | 情绪 | |
| `psychology` | 心理 | |
| `reusable` | 可复用结构 | |

组件：`el-descriptions :column="1" border` 或两列 `el-table`（维度 | 分析）。

**空态**：PROCESSING 时 skeleton；FAILED 时 `el-result` +「重新拆解」。

### `reusable_structure`

| 项 | 规范 |
|----|------|
| DDL | `video_breakdown.reusable_structure` TEXT |
| UI | `el-alert type="info"` 或 `el-card` 正文 · 保留换行 |
| 说明 | 与 `dimensions.reusable` 互补；analyze 可写摘要段落 |

### 拆帧网格 `frames_json`

| 字段 | API camelCase | 展示 |
|------|---------------|------|
| 缩略图 | `thumbnailUrl` | `el-image` 120×68 · `fit="cover"` · preview |
| 时间 | `timestamp` / `timestampLabel` | 角标 `0:05` |
| 描述 | `caption` | `el-tooltip` 或卡片下方 2 行 |

布局：`el-row` · `:gutter="8"` · `:xs="12" :sm="8" :md="6"` 响应式网格。

**mock 模式**（ADR-24）：固定 6 帧 · 占位图 + 示例 caption。

### Drawer 底栏

| 按钮 | M1 |
|------|-----|
| 关闭 | ✅ |
| 重新拆解 | P2 |
| 转为内容参考 | disabled · FR-406 M2 |
| 导出拆解表 | disabled · FR-405 M2 加版权 confirm |

---

## 字段 ↔ DDL 完整对照

### `material_asset`

| UI | DDL 列 | API camelCase |
|----|--------|---------------|
| ID | `id` | `id` |
| 类型 | `type` | `type` |
| URL | `url` | `url` |
| 缩略图 | `thumbnail_url` | `thumbnailUrl` |
| 标签 | `tags_json` | `tags` · M1 不编辑 |
| 版权 | `copyright_status` | `copyrightStatus` |
| 来源 | `source` | `source` |
| 创建时间 | `created_at` | `createdAt` |

### `video_breakdown`

| UI | DDL 列 | API camelCase |
|----|--------|---------------|
| ID | `id` | `breakdownId` |
| 素材 URL | `source_url` | `sourceUrl` |
| 拆帧 | `frames_json` | `frames[]` |
| 七维 | `dimensions_json` | `dimensions` |
| 可复用摘要 | `reusable_structure` | `reusableStructure` |
| 创建时间 | `created_at` | `breakdownCreatedAt` |

**List 响应建议**（join 最新 breakdown）：

```json
{
  "id": 1,
  "type": "VIDEO",
  "url": "https://minio/.../viral-cn-tour.mp4",
  "thumbnailUrl": "https://minio/.../thumb.jpg",
  "fileName": "viral-cn-tour.mp4",
  "copyrightStatus": "external",
  "source": "TikTok reference",
  "breakdownId": 10,
  "breakdownStatus": "SUCCESS",
  "frameCount": 6,
  "needsHumanReview": true,
  "createdAt": "2026-07-09T14:20:00+08:00"
}
```

**详情响应**：

```json
{
  "breakdownId": 10,
  "materialId": 1,
  "sourceUrl": "...",
  "breakdownStatus": "SUCCESS",
  "needsHumanReview": true,
  "dimensions": {
    "theme": "First-time China inbound trust building",
    "hook": "Skyline contrast in first 3 seconds",
    "shot": "Fast B-roll cuts with handheld POV",
    "subtitle": "Bold English keywords on screen",
    "emotion": "Curiosity → aspiration → safety",
    "psychology": "Social proof and risk reduction",
    "reusable": "Problem-evidence-CTA three-act"
  },
  "reusableStructure": "Open with a visual hook, establish credibility with stats, close with soft CTA.",
  "frames": [
    {
      "timestamp": 0,
      "timestampLabel": "0:00",
      "thumbnailUrl": "...",
      "caption": "Opening skyline hook"
    }
  ]
}
```

---

## 版权合规 Footnote（FR-405 · 必须展示）

固定 `el-alert type="warning" show-icon :closable="false"`，表格下方：

> 外部爆款素材仅供团队内部结构学习，请勿直接搬运发布。用于客户交付或对外导出前请确认版权归属。（FR-405）

M1 **不拦截**上传/查看/导出；M2+ 导出前 `ElMessageBox.confirm` 版权确认（P2 文档预留）。

上传 dialog 内重复一行 info 小字。

---

## 空 / 加载 / 错误

| 状态 | UI |
|------|-----|
| 加载 | `v-loading` |
| 无素材 | `el-empty`「暂无参考素材」+ 主按钮「上传素材」 |
| 上传失败 | dialog 内 error · 保留文件选择 |
| 拆解失败 | 行 Tag 失败 · drawer `el-result` + 重试 |
| MinIO 不可用 | upload error「存储服务不可用」 |

---

## 响应式

| 断点 | 行为 |
|------|------|
| ≥1200px | 全列 |
| 768–1199px | 隐藏来源/帧数 |
| &lt;768px | 缩略图+素材+状态+操作；drawer 100% 宽；frames 2 列 |

---

## 菜单与路由

```text
父菜单：内容 Agent (path: /content, icon: video-camera)
  └─ 内容任务 (/projects/:projectId/content/tasks)
  └─ 爆款拆解 (/projects/:projectId/materials, component: tourgeo/materials/index)
  └─ 素材标签库 — FR-404 disabled M2+
  └─ 发布计划 — FR-305 disabled
```

**重定向**：`/materials` → `projectStore.currentProjectId`

**权限**：`tourgeo:material:list` · `tourgeo:material:edit` · `tourgeo:material:upload`

---

## API 依赖（开发对齐）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/projects/{projectId}/materials` | multipart 上传 FR-401 |
| GET | `/api/v1/projects/{projectId}/materials` | 分页列表 + breakdown 摘要 |
| POST | `/api/v1/projects/{projectId}/materials/{materialId}/breakdown` | 触发拆解 202 |
| GET | `/api/v1/projects/{projectId}/materials/breakdowns/{breakdownId}` | 详情 drawer |

Python（Java 内网）：`POST /ai/breakdown/extract-frames` · `POST /ai/breakdown/analyze`

---

## 与 content-task 关系

| 场景 | M1 | M2+ |
|------|-----|-----|
| 菜单 | 启用「爆款拆解」 | — |
| 内容任务引用拆解 | — | drawer「转为内容参考」→ content_task |
| 七维 copy | 手动复制 | 注入 FR-302 Prompt P2 |

---

## 实现参考

| 项 | 建议 |
|----|------|
| 视图 | `inbound-admin/src/views/tourgeo/materials/index.vue` |
| API | `src/api/tourgeo/material.ts` |
| 常量 | `src/constants/material.ts` — `MATERIAL_TYPE_LABELS` · `BREAKDOWN_STATUS_META` · `COPYRIGHT_STATUS_META` |
| 列表模式 | [content-task-list.md](./content-task-list.md) · `tourgeo/content/tasks/index.vue` |
| 上传 | 复用 knowledge 上传 pattern · `el-upload` + `FormData` |
| 轮询 | PROCESSING 行 · `setInterval` 5s · `onUnmounted` clear |

---

## M1 范围边界

| 包含 | 不包含 |
|------|--------|
| 上传 VIDEO/IMAGE | 批量 ZIP 导入 |
| 拆帧 grid + 七维表 | 标签库 CRUD FR-404 |
| 拆解状态 + 轮询 | PySceneDetect 智能切镜 |
| FR-405 footnote | 导出版权 confirm 拦截 |
| mock 6 帧可测 | 素材推荐 FR-406 |

---

## 版本

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-09 | UI 设计 | EPIC-5 M1 初版 · FR-401~403 · FR-405 提示 · ADR-20260709-24 |
