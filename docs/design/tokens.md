# 设计 Token — 旅获 AI Admin

> 实现时写入 `variables.module.scss`（`:root`）并同步 `settings.ts` → `theme`。  
> 前缀 `--tg-`（TourGEO）为品牌扩展；`--el-*` 由 Element Plus 主题链继承。

---

## 1. 色彩

### 1.1 品牌主色

| Token | 值 | 用途 | 对比度（白底正文 / 白字按钮） |
|-------|-----|------|-------------------------------|
| `--tg-color-primary` | `#1677A0` | 主按钮、链接、选中态、GEO 分数强调 | 4.52:1 / ✅ AA |
| `--tg-color-primary-light` | `#E8F4FA` | 主色浅底、统计卡背景 | — |
| `--tg-color-primary-dark` | `#0F5575` | Hover 深态、侧栏激活（可选） | — |
| `--tg-color-accent` | `#D4920A` | 预警、待办角标、次要 CTA | 与 `#FFF` 4.6:1 ✅ |
| `--tg-color-accent-light` | `#FEF6E6` | 预警条背景 | — |

**settings.ts 建议**：`theme: '#1677A0'`

### 1.2 中性色（文本与界面）

| Token | 值 | 用途 |
|-------|-----|------|
| `--tg-color-text-primary` | `#1F2937` | 标题、表格主文案 |
| `--tg-color-text-regular` | `#4B5563` | 正文、表单标签 |
| `--tg-color-text-secondary` | `#6B7280` | 辅助说明、表头次要 |
| `--tg-color-text-placeholder` | `#9CA3AF` | 占位符 |
| `--tg-color-border` | `#E5E7EB` | 卡片、表格边框 |
| `--tg-color-border-light` | `#F3F4F6` | 分割线 |
| `--tg-color-bg-page` | `#F5F7FB` | 页面背景（与现有 `--el-bg-color-page` 一致） |
| `--tg-color-bg-surface` | `#FFFFFF` | 卡片、表格内容区 |

### 1.3 语义色（状态 / 诊断）

与 Element Plus 对齐，保证 Tag/Badge 可读：

| 语义 | Token | 值 | 场景 |
|------|-------|-----|------|
| 成功 | `--tg-color-success` | `#059669` | 诊断 SUCCESS、健康度良好 |
| 警告 | `--tg-color-warning` | `#D97706` | PARTIAL_FAILED、额度预警 |
| 危险 | `--tg-color-danger` | `#DC2626` | FAILED、竞品压制高 |
| 信息 | `--tg-color-info` | `#64748B` | 草稿、已取消 CANCELLED |
| 进行中 | `--tg-color-running` | `#1677A0` | RUNNING（主色复用 + 动画点） |

**诊断状态 Tag 映射**（`diagnostic_run.status`）：

| DB 值 | Tag type | 中文 |
|-------|----------|------|
| PENDING | `info` | 待执行 |
| RUNNING | `primary` | 执行中 |
| SUCCESS | `success` | 已完成 |
| PARTIAL_FAILED | `warning` | 部分失败 |
| FAILED | `danger` | 失败 |
| CANCELLED | `info` | 已取消 |

### 1.4 GEO 分数色阶（只读展示）

| 分数区间 | 色 | Token |
|----------|-----|-------|
| ≥ 70 | 绿 | `--tg-score-high` `#059669` |
| 40–69 | 琥珀 | `--tg-score-mid` `#D97706` |
| &lt; 40 | 红 | `--tg-score-low` `#DC2626` |

---

## 2. 字体

### 2.1 字体族

```css
--tg-font-family-base: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
--tg-font-family-mono: "JetBrains Mono", "Consolas", monospace; /* 仅 API ID、runId */
```

英文生成内容预览区可追加：`"Inter", "Segoe UI", sans-serif`（内容 Agent 阶段再用）。

### 2.2 字号与行高（B2B 清晰密度）

| Token | 大小 | 行高 | 字重 | 用途 |
|-------|------|------|------|------|
| `--tg-font-size-xs` | 12px | 18px | 400 | 表头辅助、时间戳 |
| `--tg-font-size-sm` | 13px | 20px | 400 | 表格正文、Tag |
| `--tg-font-size-base` | 14px | 22px | 400 | 表单、段落（**默认**） |
| `--tg-font-size-md` | 16px | 24px | 500 | 卡片标题 |
| `--tg-font-size-lg` | 20px | 28px | 600 | 页面 H1 |
| `--tg-font-size-xl` | 24px | 32px | 600 | 工作台 KPI 数字 |
| `--tg-font-size-display` | 32px | 40px | 600 | GEO 分数大号（详情页） |

Element Plus `size: 'default'` 保持不变；不在全局改为 `small`，仅在超密表格可选 `size="small"`。

---

## 3. 间距

4px 基准网格：

| Token | 值 | 用途 |
|-------|-----|------|
| `--tg-space-1` | 4px | 图标与文字间距 |
| `--tg-space-2` | 8px | Tag 内边距、紧凑列表 |
| `--tg-space-3` | 12px | 表单项间距 |
| `--tg-space-4` | 16px | 卡片内边距（默认） |
| `--tg-space-5` | 20px | `el-row :gutter="20"`（与 plus-ui 一致） |
| `--tg-space-6` | 24px | 区块间距 |
| `--tg-space-8` | 32px | 页面级 section 间距 |
| `--tg-space-10` | 40px | 空状态上下留白 |

**页面边距**：内容区 `padding: var(--tg-space-2)`（对应 `.p-2` = 8px）或卡片外 `mb-[10px]` 搜索区。

---

## 4. 圆角与阴影

继承现有 `variables.module.scss`：

| Token | 值 | 说明 |
|-------|-----|------|
| `--app-radius-base` | 8px | 卡片、按钮、输入框 |
| `--app-radius-sm` | ~5px | Tag、小按钮 |
| `--app-shadow-sm` | 见 variables | 卡片默认 |
| `--app-shadow-md` | 见 variables | 下拉、Popover |

---

## 5. 组件密度规范

| 场景 | 规范 |
|------|------|
| 列表页表格行高 | 默认；操作列 `fixed="right" width="160~200"` |
| 统计卡片 | `el-col :lg="6" :md="12" :xs="24"` 四列 KPI |
| 表单 | `label-width="100px"`，`inline` 搜索表单 |
| 主操作 | 每条列表 1 个 primary（「新建诊断」）；其余 `link` 或 `plain` |
| 空状态 | `el-empty`，附 1 个主按钮引导创建 |

---

## 6. CSS 变量片段（开发复制）

```scss
:root {
  --tg-color-primary: #1677A0;
  --tg-color-primary-light: #E8F4FA;
  --tg-color-primary-dark: #0F5575;
  --tg-color-accent: #D4920A;
  --tg-color-accent-light: #FEF6E6;
  --tg-color-text-primary: #1F2937;
  --tg-color-text-regular: #4B5563;
  --tg-color-text-secondary: #6B7280;
  --tg-color-running: #1677A0;
  --tg-score-high: #059669;
  --tg-score-mid: #D97706;
  --tg-score-low: #DC2626;
  --tg-font-family-base: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
  --tg-font-size-base: 14px;
  --tg-space-4: 16px;
  --tg-space-5: 20px;
  --tg-space-6: 24px;
}
```

同步：`document.documentElement.style.setProperty('--el-color-primary', '#1677A0')`（已有 `theme.ts`）。
