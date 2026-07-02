# EPIC-11 M2 探针 Adapter + 校准 Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-09 |
| **优先级** | High |
| **关联** | EPIC-11 · **FR-115/116** · ADR-20260709-22 |
| **前置** | EPIC-11 M1 ✅ · C18 `19e1f36` |

## 商业决策（完整版路线图 #3）

GEO 差异化需 **平台 adapter 可运营热更新** + **API vs 网页版校准可信度**。M1 仅 Perplexity seed + 扩展 poll；M2 补 Admin adapter 配置、诊断校准视图、第二平台 ChatGPT 网页 adapter。

## 目标（M2 MVP）

| 范围 | M2 做 | M2 不做 |
|------|-------|---------|
| FR-116 | Admin `GET/PUT .../platform-adapters/{platform}` · 编辑 dom/api/parse JSON · 扩展 poll 读最新 | 版本灰度 · 国内平台 |
| FR-115 | `GET .../diagnostics/{runId}/calibration` · 同 question 对比 grounded-api vs browser-extension · 偏差率 | 截图样例 FR-117 |
| 诊断创建 | `calibration_ratio`>0 时重叠抽样生成 browser-extension 子任务 | MQ 扩展调度 |
| 扩展 | 第二平台 `chatgpt` content script + adapter（可与 mock 并行） | Headless FR-118 |
| Admin | `/settings/probe-adapters` · 诊断详情「校准对比」Tab | 节点 CRUD |

## 任务拆分

| # | 角色 | HANDOFF | 依赖 | 验收 |
|---|------|---------|------|------|
| **1** | **UI 设计** | [→ adapter+校准线框](2026-07-09-tech-director-to-ui-epic11-probe-m2.md) | — | 2 线框增量 |
| **2** | **开发 Java** | [→ adapter+校准 API](2026-07-09-tech-director-to-dev-java-epic11-probe-m2.md) | — | smoke |
| **3** | **开发 Extension** | [→ ChatGPT adapter](2026-07-09-tech-director-to-dev-extension-epic11-chatgpt.md) | #2 | build |
| **4** | **开发 Admin** | [→ adapter+校准 UI](2026-07-09-tech-director-to-dev-admin-epic11-probe-m2.md) | #1+#2 | build |

## 窗口派发（复制到各 Cursor 窗口）

| # | 窗口 | HANDOFF | 激活 Prompt（Custom Instructions 首行） |
|---|------|---------|----------------------------------------|
| **1** | **UI 设计** | [→ 线框](2026-07-09-tech-director-to-ui-epic11-probe-m2.md) | `角色：UI 设计。必读 probe-nodes.md、diagnostics detail、FR-115/116、HANDOFF 2026-07-09-tech-director-to-ui-epic11-probe-m2.md。任务：probe-adapters.md + diagnostics-detail 校准 Tab 增量 + UI→开发 HANDOFF。` |
| **2** | **开发 Java** | [→ Java API](2026-07-09-tech-director-to-dev-java-epic11-probe-m2.md) | `角色：开发 Java。必读 ProbeServiceImpl、PlatformAdapter、DiagnosticRunServiceImpl、ADR-22、HANDOFF 2026-07-09-tech-director-to-dev-java-epic11-probe-m2.md。任务：platform-adapters Admin API + calibration GET + calibration_ratio 分叉 + test_probe_calibration.py。` |
| **3** | **开发 Extension** | [→ ChatGPT](2026-07-09-tech-director-to-dev-extension-epic11-chatgpt.md) | `角色：开发 Extension。必读 inbound-probe-extension、perplexity adapter、HANDOFF 2026-07-09-tech-director-to-dev-extension-epic11-chatgpt.md。任务：chatgpt content script + adapter · poll platform=chatgpt。` |
| **4** | **开发 Admin** | [→ Admin UI](2026-07-09-tech-director-to-dev-admin-epic11-probe-m2.md) | `角色：开发 Admin。必读 probe-nodes 设置页、diagnostics/detail.vue、HANDOFF 2026-07-09-tech-director-to-dev-admin-epic11-probe-m2.md。任务：/settings/probe-adapters + 详情校准 Tab · build:prod。` |

**并行**：#1 与 #2 可并行；#3 依赖 Java seed/API；#4 依赖 #1+#2。

## 完成后

- smoke：`deploy/scripts/test_probe_calibration.py`
- commit **C19**：`feat(core,admin,extension): EPIC-11 M2 probe adapters and calibration`
- **技术总监签核（2026-07-09）**：✅ smoke runId=6 · Admin build · Extension test:adapter · **C19 待入库**
- 下一 Sprint：**EPIC-9 M2** 计费 → [Sprint 索引](2026-07-09-tech-director-epic9-m2-billing-sprint.md)
