# EPIC-11 M1 浏览器探针 Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-05 |
| **优先级** | High |
| **关联** | EPIC-11 · **FR-112/113/114** · ADR-20260705-18 |
| **前置** | EPIC-2 M1 诊断 ✅ · C14 EPIC-9 计费 ⏳ |

## 目标（M1 MVP）

**扩展节点注册 → poll 拉任务 → 单平台网页提问 → 结果回写 → Admin 节点列表**

| 范围 | M1 做 | M1 不做 |
|------|-------|---------|
| FR-112 | Plasmo MV3 scaffold · background poll · **1 个海外平台**（`perplexity_web`）content script + hook 抓取 | 国内平台 adapter · 自动输入拟人化 polish |
| FR-113 | `POST .../probe/nodes/register` 心跳 · Admin 节点列表 | 节点 CRUD 后台 · 今日任务量统计图表 |
| FR-114 | poll 分配 PENDING `browser-extension` 任务 · 失败 RETRY≤3 · 复用 `handleProbeCallback` | MQ `diag.probe-extension` · Headless 兜底 |
| FR-116 | seed + `GET .../probe/adapters` 扩展拉取 | Admin adapter 编辑 UI |
| 诊断创建 | `probeModes` 含 `browser-extension` 时生成对应 `probe_task`（不送 MQ） | FR-111 校准比例调度 |
| FR-115/117/118 | — | 校准对比 · 截图存证 · Playwright |

## 已有基础（勿重复造）

- DDL：`probe_node` · `probe_task` · `platform_adapter`
- Java：`ProbeTask` 实体 · `DiagnosticRunServiceImpl.handleProbeCallback` · 内网 `/api/v1/internal/diagnostics/probe-callback`（grounded-api 用）
- Admin：诊断创建已支持 `browser-extension` checkbox · 详情页「探针进度」Tab 已有

## 任务拆分

| # | 角色 | HANDOFF | 依赖 | 验收 |
|---|------|---------|------|------|
| **0** | **开发** | [→ C14 commit](2026-07-05-tech-director-to-dev-c14-commit.md) | — | push |
| **1** | **UI 设计** | [→ 探针节点线框](2026-07-05-tech-director-to-ui-epic11-probe-nodes.md) | — | `probe-nodes.md` |
| **2** | **开发 Java** | [→ probe API + 调度](2026-07-05-tech-director-to-dev-java-epic11-probe.md) | — | smoke |
| **3** | **开发 Extension** | [→ Plasmo scaffold](2026-07-05-tech-director-to-dev-extension-epic11-plasmo.md) | #2 | 手动 E2E |
| **4** | **开发 Admin** | [→ 探针节点页](2026-07-05-tech-director-to-dev-admin-epic11-probe-nodes.md) | #1+#2 | build |

**无 Python / 无运维 M1**（扩展直连 Java :8080）。

## API 契约（M1）

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/api/v1/probe/nodes/register` | `@SaIgnore` + `X-Probe-Node-Key` | upsert node · 更新 heartbeat |
| GET | `/api/v1/probe/tasks/poll` | 同上 + query `platform` | 分配 1 条 DISPATCHED 任务 |
| POST | `/api/v1/probe/tasks/{id}/result` | 同上 | body 对齐 `ProbeCallbackBo` |
| GET | `/api/v1/probe/adapters` | 扩展 | 启用的 adapter 列表 |
| GET | `/api/v1/probe/nodes` | JWT Admin | 租户节点列表 |

**Poll 响应示例**：

```json
{
  "taskId": 42,
  "runId": 5,
  "question": "Best China tour for first-time visitors?",
  "platform": "perplexity",
  "probeMode": "browser-extension",
  "targetUrl": "https://www.perplexity.ai/",
  "adapterVersion": "1.0"
}
```

## 窗口 Prompt 摘要

| 角色 | Prompt |
|------|--------|
| 开发 | 先 C14 · 再 probe API + browser-extension 分支 |
| UI | `probe-nodes.md` 节点列表 + 在线/离线 + 地区/平台 |
| Java | `ProbeController` + poll 调度 + createRun 分 probe_mode |
| Extension | Plasmo poll → Perplexity tab → hook → POST result |
| Admin | `/settings/probe-nodes` 只读列表 |

## 完成后

- smoke：`deploy/scripts/test_probe_extension_e2e.py`（Java API mock result 路径 + 可选真扩展）
- commit **C15**：`feat(core,admin,extension): EPIC-11 M1 browser probe poll and node registry`
- 下一 Sprint：**EPIC-3 M2** 关键词评分 → [Sprint 索引](2026-07-06-tech-director-epic3-m2-keyword-score-sprint.md)
