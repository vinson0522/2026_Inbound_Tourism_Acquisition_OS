# HANDOFF | 技术总监 → 开发（Extension）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 Extension | 2026-07-10 | EPIC-11 维护 · B-03b |

## 上下文

浏览器探针 M1/M2 已 scaffold + mock adapter。维护轨目标：**真 Perplexity 页面** hook 抓取 citations/SSE，替换纯 fixture 路径。

**相关文件**：
- `inbound-probe-extension/src/adapters/`
- `deploy/scripts/test_probe_extension_e2e.py`
- `docs/agent-team/MEMORY.md` B-03b

## 交付请求

- [x] Perplexity adapter：fetch/SSE hook 解析 citations（与 PRD §7.6 一致）
- [x] 频率控制 ≥30s · 仅处理 poll 下发 task
- [x] 手工或 E2E 证据：真实 URL 返回 ≥1 citation

## 约束

- 不上传非任务 tab 内容
- adapter 变更可热更新（platform_adapter 表）

## Prompt

```
角色：开发 Extension。必读 Plasmo background poll、perplexity adapter、AGENTS.md §9、MEMORY B-03b。
任务：真 Perplexity hook citations。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-02
- **结果摘要**：
  - `adapters/perplexity.ts`：多路径 citation 解析（`citations`/`web_results`/`search_results`/`hidden_params`）、SSE/NDJSON 流合并、`urlMatchesChatApi` 覆盖 `/rest/sse/perplexity_ask` + `/api/chat`
  - `contents/perplexity.ts`：fetch + XHR + EventSource hook · 与 background `ContentMessage` 对齐 · mock 开关 `PLASMO_PUBLIC_PROBE_MOCK`
  - `fixtures/perplexity-sse-sample.txt` + `pnpm test:perplexity-hook` — 4 fixture 均 ≥1 citation
  - `005_probe_perplexity_adapter_hook.sql` + `002_seed_demo` adapter 热更新字段
  - `test_probe_extension_e2e.py` hook 形态 result · runId=14 SUCCESS
- **遗留**：真 Perplexity 页面 live 抓取需 Chrome 加载扩展 + 登录 Perplexity；本地 smoke 用 adapter fixture + Java 回调形态验证
