# 线框：系统设置 · 报告模板（EPIC-8 M2）

> **PRD**：§8.9 报告中心 · **FR-704** 报告模板配置（白标）  
> **EPIC**：EPIC-8 M2 · **ADR-20260708-21**  
> **路由**：`/settings/report-template`（侧栏「系统设置 → 报告模板」）  
> **数据表**：`template`（`type=REPORT` · `config_json` JSONB）

---

## 页面目标

在 **租户级** 配置报告导出白标：Logo、封面标题、公司名称、主色、页脚与 **章节开关**。保存后，周报 / 月报 / 诊断报告 DOCX·PDF 导出自动套用（Java 渲染注入 `report.template_id`）。

**M2 范围**：
- ✅ 表单编辑 + 章节 checklist + 封面预览 mock + 保存/重置
- ✅ 从报告列表「模板配置」链入
- ❌ 自定义域名 FR-805 · GrapesJS 可视化 · 多模板切换 · MinIO 上传 Logo（M2 仅 URL）

**入口**：
- 侧栏「系统设置 → 报告模板」
- [reports-list.md](./reports-list.md) 工具栏「模板配置」
- 月报 dialog「去配置模板」（未配置 alert）

**权限**：`tourgeo:report:template`（租户管理员可编辑；运营可只读查看 — 与若依菜单对齐）

---

## 布局结构（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 面包屑：系统设置 / 报告模板                                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─ el-row :gutter="16" ────────────────────────────────────────────────────┐ │
│ │ ┌─ 左：配置表单 el-card (span=14) ──────────────────────────────────────┐ │ │
│ │ │ H3 报告白标模板                                                         │ │ │
│ │ │ ℹ 配置将应用于本租户所有项目的报告导出（周报/月报/诊断）。               │ │ │
│ │ │                                                                         │ │ │
│ │ │ Logo URL      [https://cdn.example.com/logo.png________]  [预览]        │ │ │
│ │ │ 封面标题*     [Inbound Growth Report________________]                   │ │ │
│ │ │ 公司名称*     [Dragon Journey Travel________________]                   │ │ │
│ │ │ 主题主色      [■ #059669]  el-color-picker  + hex 输入                 │ │ │
│ │ │ 页脚文案      [Confidential · TourGEO Agent___________]  textarea 2行   │ │ │
│ │ │ ── 导出章节 (至少选 1 项) ──                                            │ │ │
│ │ │ ☑ GEO 可见率与变化    ☑ 关键词机会                                      │ │ │
│ │ │ ☑ 内容产出            ☑ 落地页                                          │ │ │
│ │ │ ☑ 询盘与 CRM          ☑ 优化建议                                        │ │ │
│ │ │                                                                         │ │ │
│ │ │ [恢复默认]  [取消]  [保存模板] primary                                   │ │ │
│ │ └─────────────────────────────────────────────────────────────────────────┘ │ │
│ │ ┌─ 右：导出预览 mock el-card (span=10) ─────────────────────────────────┐ │ │
│ │ │ H3 封面预览（示意）                                                     │ │ │
│ │ │ ┌─ 封面区 border dashed · min-height 280px ──────────────────────────┐ │ │ │
│ │ │ │  [Logo img 120×40 或 placeholder]                                   │ │ │ │
│ │ │ │  ── primaryColor 顶条 4px ──                                       │ │ │ │
│ │ │ │  {coverTitle}  H2 居中                                              │ │ │ │
│ │ │ │  {companyName}  副标题                                             │ │ │ │
│ │ │ │  2026年6月 · 增长月报  (示例 period)                                │ │ │ │
│ │ │ │  … 章节列表随 checklist 实时增减 …                                  │ │ │ │
│ │ │ │  ── footer ──                                                      │ │ │ │
│ │ │ │  {footerText}  12px 灰色居中                                       │ │ │ │
│ │ │ └─────────────────────────────────────────────────────────────────────┘ │ │ │
│ │ │ 实际 DOCX/PDF 版式由服务端模板引擎渲染；此处仅前端 mock。                │ │ │
│ │ └─────────────────────────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
│ ┌─ 说明 el-alert type="info" show-icon :closable="false" ────────────────────┐ │
│ │ · Logo 请使用 HTTPS 可访问图片 URL；建议 PNG/SVG 透明底，宽 ≤ 400px。      │ │
│ │ · 关闭某章节后，导出文档中不渲染该章（聚合数据仍可在 Admin 预览 drawer 查看）。│ │
│ │ · 完整门户白标（自定义域名）见 FR-805，不在 M2 范围。                       │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

**响应式**：&lt;992px 预览卡片下移全宽；表单与预览上下堆叠。

---

## 表单字段 ↔ DDL / API

| UI 标签 | `config_json` 键 | 组件 | 必填 | 校验 / 说明 |
|---------|------------------|------|:----:|-------------|
| Logo URL | `logoUrl` | `el-input` + link「预览」 | — | URL 格式；空则导出无 Logo 图 |
| 封面标题 | `coverTitle` | `el-input` max 80 | ✅ | 导出封面 H1 |
| 公司名称 | `companyName` | `el-input` max 80 | ✅ | 封面副标题 · 页眉 |
| 主题主色 | `primaryColor` | `el-color-picker` + hex | — | 默认 `#1677A0`（品牌）或 `#059669` |
| 页脚文案 | `footerText` | `el-input` textarea | — | max 200 · 每页页脚 |
| 导出章节 | `sections[]` | `el-checkbox-group` | ✅ ≥1 | 见下表 |

### `sections[]` 枚举（与 Java 渲染器一致）

| 值 | 中文标签 | 默认 |
|----|----------|:----:|
| `geo` | GEO 可见率与变化 | ☑ |
| `keywords` | 关键词机会 | ☑ |
| `content` | 内容产出 | ☑ |
| `landing` | 落地页 | ☑ |
| `leads` | 询盘与 CRM | ☑ |
| `recommendations` | 优化建议 | ☑ |

**顺序**：导出章节顺序固定为上表顺序；checkbox 仅控制显隐，不拖拽排序（M2）。

---

## API 契约

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/settings/report-template` | 返回 `{ templateId, configJson }`；无记录时 **200 + 默认 config** |
| PUT | `/api/v1/settings/report-template` | upsert 租户级 `template.type=REPORT` |

**GET 响应示例**：

```json
{
  "code": 0,
  "data": {
    "templateId": 101,
    "configJson": {
      "logoUrl": "https://cdn.example.com/logo.png",
      "coverTitle": "Inbound Growth Report",
      "companyName": "Dragon Journey Travel",
      "primaryColor": "#059669",
      "footerText": "Confidential · TourGEO Agent",
      "sections": ["geo", "keywords", "content", "landing", "leads", "recommendations"]
    }
  }
}
```

**PUT 请求体**：同上 `configJson` 结构（camelCase 与后端 DTO 对齐）。

**成功**：toast「模板已保存」· 预览 mock 刷新。

**校验失败**：400 — 如 `sections` 为空 · `primaryColor` 非 hex · 必填项缺失。

---

## 默认模板（恢复默认）

点击「恢复默认」→ `ElMessageBox.confirm` → 表单回填 seed / 后端默认值：

| 字段 | 默认值 |
|------|--------|
| `logoUrl` | `""` |
| `coverTitle` | `Inbound Growth Report` |
| `companyName` | 租户名（若 API 提供 `tenantName`）或空 |
| `primaryColor` | `#1677A0` |
| `footerText` | `Generated by TourGEO Agent · Confidential` |
| `sections` | 六项全选 |

**未保存离开**：`onBeforeRouteLeave` 或表单 dirty 提示。

---

## Logo 预览交互

| 操作 | 行为 |
|------|------|
| 输入 URL blur | 右侧 mock + 小图 `el-image` 尝试加载 |
| 加载失败 | `el-image` error slot「无法加载 Logo，请检查 URL」 |
| 「预览」按钮 | `window.open(logoUrl)` 新 tab（可选） |

M2 **不上传** MinIO；运维文档注明「Logo 托管在客户 CDN 或对象存储公网 URL」。

---

## 封面预览 Mock（右栏）

**实时绑定**表单 v-model：

- 顶条 `height: 4px; background: primaryColor`
- Logo：`el-image` fit="contain" max 120×48
- 标题 / 公司名 / 示例 period 文案
- 章节列表：根据 `sections` 勾选显示 6 行中文标签
- 页脚：`footerText` 12px `#909399`

**示例 period 文案**：固定 mock「2026年6月 · 增长月报」；不随业务数据变化。

---

## 与报告中心关系

| 场景 | 行为 |
|------|------|
| 保存模板后 | 后续 `POST .../reports/weekly|monthly` 写入 `report.template_id` |
| 历史报告 | 导出读 **生成时** 绑定的 template（若已存 id）；否则当前模板 |
| 报告列表 | 工具栏「模板配置」跳转本页 |
| 预览 drawer | 可选展示 `templateSnapshot`（生成月报时写入 summary） |

---

## 菜单与路由

```text
父菜单：系统设置 (path: /settings)
  ├─ 套餐与额度 → /settings/billing
  ├─ 探针节点 → /settings/probe-nodes
  └─ 报告模板 → /settings/report-template  (component: tourgeo/settings/report-template/index)
```

**权限标识**：`tourgeo:report:template`（编辑）· 只读角色隐藏「保存」

---

## 空 / 加载 / 错误

| 状态 | UI |
|------|-----|
| 加载 | 整页 `v-loading` |
| GET 失败 | `el-result` +「重试」 |
| 只读角色 | 表单 `:disabled="true"` · 隐藏保存/恢复默认 |
| 403 | 若依无菜单则不渲染 |

---

## 组件与实现提示

| 项 | 建议 |
|----|------|
| 视图 | `inbound-admin/src/views/tourgeo/settings/report-template/index.vue` |
| API | `src/api/tourgeo/report.ts` — `getReportTemplate` · `saveReportTemplate` |
| 布局参考 | [billing-settings.md](./billing-settings.md) 卡片 + footnote alert |
| 表单 | `el-form` `:rules` · `@submit.prevent` |
| 常量 | `src/constants/report.ts` — `REPORT_SECTION_LABELS` |

---

## 版本

| 日期 | 作者 | 说明 |
|------|------|------|
| 2026-07-08 | UI 设计 | EPIC-8 M2 初版 · FR-704 · 白标表单 + 封面预览 mock · ADR-20260708-21 |
