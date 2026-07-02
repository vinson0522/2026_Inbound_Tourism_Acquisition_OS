# HANDOFF | 技术总监 → 开发（Extension）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-09 | EPIC-11 M2 · FR-112 · ADR-20260709-22 |

## 交付请求

- [ ] `src/adapters/chatgpt.ts` + `src/contents/chatgpt.ts` — 对齐 Java seed parse_rules
- [ ] `background.ts` poll 支持 `platform=chatgpt`（与 perplexity 轮换或按任务 platform）
- [ ] mock 模式可测（无 Key/DOM 变更时）
- [ ] README 更新第二平台安装说明
- [ ] `pnpm build` ✅

## Prompt

```
角色：开发 Extension。必读 perplexity 实现、ADR-22。
任务：ChatGPT 第二平台 adapter + content script · mock 可测。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-09
- **结果摘要**：
  - `src/adapters/chatgpt.ts` — DOM/API/parse 对齐 Java seed `message.content.parts` + `message.metadata.citations`
  - `src/contents/chatgpt.ts` — fetch/SSE hook + mock + `#prompt-textarea` 提交
  - `background.ts` — 注册双平台 · round-robin `pollTask(perplexity|chatgpt)` · 按 `task.platform` 打开对应 tab
  - `lib/platforms.ts` · README M2 · `pnpm build` ✅ · `pnpm test:adapter` ✅
- **遗留**：真实 ChatGPT DOM 变更时需 Admin 热更新 adapter JSON；`chat.openai.com` 未纳入 M2
