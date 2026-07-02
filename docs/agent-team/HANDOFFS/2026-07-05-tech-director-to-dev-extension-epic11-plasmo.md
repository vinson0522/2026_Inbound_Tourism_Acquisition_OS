# HANDOFF | 技术总监 → 开发（Extension）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-05 | EPIC-11 M1 · FR-112 · ADR-20260705-18 |

## 上下文

`inbound-probe-extension/` 仅有 README，待 Plasmo scaffold。Java probe API 由并行 HANDOFF 实现；扩展在 **Chrome MV3** 中 poll 任务、打开目标 AI 页、hook fetch/SSE 抓取回答与 citations，上报结果。

**相关文件**：
- `inbound-probe-extension/README.md`
- `AGENTS.md` §9 浏览器扩展规范
- `PRD_商业化版_V2.0.md` §7.6.4

**约束**：
- 仅处理 poll 返回的 `probe_task`；不上传其他 tab 内容
- 单节点单平台间隔 ≥30s（`chrome.alarms`）
- M1 仅 **1 个平台**：`perplexity`（`https://www.perplexity.ai/`）

## 交付请求

Plasmo 项目 scaffold + 最小可跑通 E2E（配合 Java smoke 或手动）。

**验收标准**：
- [ ] `package.json` + `plasmo.config.ts` + TypeScript
- [ ] `src/background.ts` — `chrome.alarms` 每 30s `GET {API}/api/v1/probe/tasks/poll?platform=perplexity` · Header `X-Probe-Node-Key`
- [ ] `src/contents/perplexity.ts` — 接收 background 消息 · 注入提问（或 M1：打开问题 URL + 等待）· hook `fetch`/SSE 解析 citations
- [ ] `src/adapters/perplexity.ts` — 本地 parse rules（与 Java seed adapter 字段对齐）
- [ ] `src/popup/index.tsx` — 显示 nodeKey · 上次 poll 时间 · 当前任务状态
- [ ] `POST .../probe/tasks/{id}/result` — 上报 SUCCESS/FAILED
- [ ] `POST .../probe/nodes/register` — 扩展安装/启动时注册
- [ ] env：`PLASMO_PUBLIC_API_BASE=http://localhost:8080` · `PLASMO_PUBLIC_NODE_KEY=dev-probe-1`
- [ ] README 更新：安装步骤 · load unpacked · 与 Admin 联调说明
- [ ] 可选：`pnpm build` 产出 `build/chrome-mv3-prod`

## M1 简化路径（若 hook 困难）

允许 **dev mock 模式**：content script 读 `question` 文本，构造固定 citations JSON 上报，先打通 poll→result 闭环；hook 抓取作为同 Sprint stretch goal。

## Prompt

```
角色：开发 Extension。必读 AGENTS.md §9、inbound-probe-extension/README。
任务：Plasmo MV3 · background poll · perplexity content script · 仅处理下发任务。
依赖 Java ProbeController 就绪后再联调。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-05
- **结果摘要**：Plasmo MV3 scaffold ✅ · `background.ts` 30s alarm poll + register/adapters · `contents/perplexity.ts` fetch/SSE hook + mock mode · `adapters/perplexity.ts` 对齐 seed parse_rules · popup 节点状态 · `pnpm build` → `build/chrome-mv3-prod` ✅
- **遗留**：真实 Perplexity DOM 可能变更需 adapter 热更新验证；C15 commit 待用户指令；与 Java 联调需 load unpacked + mock off
