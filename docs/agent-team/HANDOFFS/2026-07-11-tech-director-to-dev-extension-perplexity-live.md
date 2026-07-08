# HANDOFF | 技术总监 → 开发（Extension）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 Extension | 2026-07-11 | B-03b · **⏸ 挂起 B-23** · [重排](2026-07-11-tech-director-sprint2-reprioritize-no-third-party.md) |

> **2026-07-11 技术总监：本任务挂起。** fixture/API smoke 已绿 · `verify_perplexity_live.py` 可入库。**勿再占用开发窗口**直至 perplexity.ai 可登录。

## 上下文

维护轨 #1 已完成 adapter hook + fixture（C24 `6f4738a` · `pnpm test:perplexity-hook` ✅ · `test_probe_extension_e2e` API mock ✅）。

**遗留**：真 Perplexity 页面 live 抓取未在 Chrome + 登录会话下验收。

**相关文件**：
- `inbound-probe-extension/README.md` § Load in Chrome · § E2E verification
- `inbound-probe-extension/src/contents/perplexity.ts` · `src/adapters/perplexity.ts`
- `inbound-probe-extension/.env.development` — **`PLASMO_PUBLIC_PROBE_MOCK=false`**
- `deploy/scripts/test_probe_extension_e2e.py`（API-only · 非 live）

## 交付请求

- [ ] `pnpm build` → Chrome **Load unpacked** `build/chrome-mv3-prod`
- [ ] Popup：Node key `demo-probe-1` · Last poll 更新
- [ ] 登录 [perplexity.ai](https://www.perplexity.ai/) 同浏览器 profile
- [ ] Admin 或 API 创建 diagnostic：`probeModes: ["browser-extension"]` · `models: ["perplexity"]` · `sampleCount: 1`
- [ ] 扩展 poll（≤30s）→ 提交 result → run **SUCCESS** · result 含 **≥1 citation**（非 mock 字符串）
- [ ] 若 DOM/API 变更：只改 adapter/contents · 不改 poll 核心
- [ ] README 增补 **Live Perplexity** 小节（步骤 + 常见失败）

## 验收标准

**必须提供**（写入 Done）：
- runId + probeTaskId
- result JSON 片段（citations 数组 ≥1 条含 url/domain）
- popup 截图或 background console 日志（脱敏）
- mock 开关确认为 `false`

## Prompt

```
角色：开发 Extension。必读 README、perplexity adapter/contents、AGENTS.md §9、HANDOFF 2026-07-11-tech-director-to-dev-extension-perplexity-live.md。
任务：Chrome 真 Perplexity live hook 验收 · mock off · citations≥1 · 证据+README。
```

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
