# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-05 | EPIC-11 M1 · FR-112~114 · ADR-20260705-18 |

## 上下文

诊断 `createRun` 当前 **全部** `probe_task` 走 `grounded-api` + MQ。Admin 创建表单已可选 `browser-extension`，但无后端调度。DDL 与 `handleProbeCallback` 已就绪。

**相关文件**：
- `DiagnosticRunServiceImpl.java` — 创建 run / callback / finalize
- `DiagnosticInternalController.java` — 内网 token 校验模式（参考）
- `001_schema.sql` — `probe_node` · `platform_adapter`
- `inbound-probe-extension/README.md` — API 列表

**约束**：browser-extension 结果 **不走 Python**；扩展 JSON 在 Java 侧解析为 `diagnostic_result`（M1 可简化 citations 映射，后续调 `/ai/parse-citations`）。

## 交付请求

实现探针公开 API + `browser-extension` 任务分支 + Admin 节点列表。

**验收标准**：
- [x] 实体/Mapper：`ProbeNode` · `PlatformAdapter`（若尚未有）
- [x] `ProbeController`（`/api/v1/probe/**`）：
  - `POST /nodes/register` — body: `nodeKey, region, platforms[], extensionVersion` · upsert · 更新 `last_heartbeat_at`
  - `GET /tasks/poll?platform=perplexity` — 匹配租户（M1：`node_key` header 绑 tenant=1 dev）· 取 1 条 `PENDING`+`browser-extension` · 设 `DISPATCHED`+`probe_node_id`
  - `POST /tasks/{id}/result` — 校验 node 归属 · 调 `handleProbeCallback`
  - `GET /adapters` — 返回 enabled adapter（dom/api/parse JSON）
- [x] 鉴权 M1：`@SaIgnore` + Header `X-Probe-Node-Key`（配置 `inbound.probe.node-keys` 白名单或 DB `node_key` 匹配 tenant）
- [x] `DiagnosticRunServiceImpl.createRun`：按 `probeModes` 分叉
  - `grounded-api` → 现有 MQ 路径
  - `browser-extension` → `probe_mode=browser-extension` · status `PENDING` · **不** publish MQ
- [x] `GET /api/v1/probe/nodes` — JWT · 当前租户列表 · 计算 `online`（heartbeat 60s 内）
- [x] seed：`002_seed_demo.sql` 追加 1 条 `platform_adapter`（platform=`perplexity` · version=`1.0` · 最小 selectors）
- [x] `tenant.excludes` += `probe_node` · `platform_adapter`（若需要）
- [x] smoke：`deploy/scripts/test_probe_extension_e2e.py` — register → 创建含 browser-extension 的 run → poll → mock result → run SUCCESS

## result JSON（扩展上报 · M1 最小）

```json
{
  "probeTaskId": 42,
  "status": "SUCCESS",
  "result": {
    "probe_mode": "browser-extension",
    "platform": "perplexity",
    "answer_text": "...",
    "citations": [{ "url": "...", "title": "...", "domain": "...", "rank": 1 }],
    "mentioned_brands": ["China Highlights"],
    "raw_response_json": {}
  }
}
```

## Prompt

```
角色：开发 Java。必读 AGENTS.md §6.1 browser-extension、DiagnosticRunServiceImpl、ADR-18。
任务：ProbeController + createRun 分 probe_mode + Admin GET nodes + smoke。
不调 Python diagnose MQ 处理 extension 任务。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-05
- **结果摘要**：
  - `ProbeNode` / `PlatformAdapter` 实体 + Mapper
  - `ProbeController` — register / poll / result / adapters + JWT `GET /nodes`
  - `ProbeServiceImpl` — `X-Probe-Node-Key` 鉴权 · poll `FOR UPDATE` · result → `handleProbeCallback`
  - `DiagnosticRunServiceImpl.createRun` — `browser-extension` 任务 PENDING 不发 MQ；`grounded-api` 保持 MQ
  - `application-dev.yml` `inbound.probe.allowed-node-keys` · security/tenant excludes
  - `002_seed_demo.sql` — `platform_adapter` perplexity v1.0
  - smoke `test_probe_extension_e2e.py` ✅ runId=3 SUCCESS
- **遗留**：Extension Plasmo · Admin 探针节点页 · C15 commit
