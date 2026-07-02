# inbound-probe-extension

Plasmo **Chrome MV3** browser GEO probe for TourGEO / Inbound AI Growth Agent.

Polls Java `ProbeController` for `browser-extension` tasks, opens the target platform tab, captures answer + citations via fetch/SSE hooks, and posts results back.

## Scope (EPIC-11 M2)

- Platforms: **perplexity** (`https://www.perplexity.ai/`) · **chatgpt** (`https://chatgpt.com/`)
- Endpoints: register · poll · adapters · submit result
- Poll interval: **30s** (`chrome.alarms`) — round-robin `platform=perplexity` then `platform=chatgpt`
- Dev mock mode: `PLASMO_PUBLIC_PROBE_MOCK=true` (poll→result without DOM hook)

## Prerequisites

- Node 18+ and **pnpm**
- Java `inbound-core` on `:8080` with probe API enabled
- `inbound.probe.allowed-node-keys` must include your node key (default **`demo-probe-1`**)
- DB seed includes `platform_adapter` rows for **perplexity** and **chatgpt** (`002_seed_demo.sql`)

## Setup

```bash
cd inbound-probe-extension
cp .env.example .env.development   # or edit existing .env.development
pnpm install
pnpm dev                           # watch build → build/chrome-mv3-dev
```

Production build:

```bash
pnpm build                         # → build/chrome-mv3-prod
pnpm test:adapter                  # parse rule sanity (no browser)
```

## Load in Chrome

1. Open `chrome://extensions`
2. Enable **Developer mode**
3. **Load unpacked** → select `inbound-probe-extension/build/chrome-mv3-dev` (or `-prod`)
4. Open extension popup — verify **Node key** and **Last poll** update
5. Keep a logged-in session on Perplexity and/or ChatGPT tabs (or let the extension open background tabs)

## Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `PLASMO_PUBLIC_API_BASE` | `http://localhost:8080` | Java API base |
| `PLASMO_PUBLIC_NODE_KEY` | `demo-probe-1` | Must match Java allowlist + DB node |
| `PLASMO_PUBLIC_CLIENT_ID` | RuoYi client id | Optional; sent on probe requests |
| `PLASMO_PUBLIC_PROBE_MOCK` | `false` | Mock capture in content scripts |

## E2E verification

**API-only smoke** (no extension required):

```bash
python deploy/scripts/test_probe_extension_e2e.py          # perplexity
python deploy/scripts/test_probe_calibration.py          # dual-mode + calibration
```

**With extension**:

1. Start Java + Postgres + Redis
2. Load extension (mock off or on)
3. Admin: create diagnostic with `probeModes: ["browser-extension"]`, `models: ["perplexity"]` or `["chatgpt"]`
4. Extension polls within 30s (per platform) → runs task → diagnostic run → `SUCCESS`

**ChatGPT-only quick test** (mock):

1. Set `PLASMO_PUBLIC_PROBE_MOCK=true` in `.env.development` and rebuild
2. Register node with both platforms (automatic on bootstrap)
3. Create diagnostic `models: ["chatgpt"]`, `probeModes: ["browser-extension"]`
4. Extension polls `platform=chatgpt` and submits mock result

Mock mode is useful when platform DOM/API changes; reload the extension after toggling env.

## Structure

```
src/
├── background.ts           # register, alarms, multi-platform poll, tab orchestration
├── contents/
│   ├── perplexity.ts         # Perplexity question submit + fetch/SSE hooks
│   └── chatgpt.ts            # ChatGPT question submit + fetch/SSE hooks
├── adapters/
│   ├── perplexity.ts         # parse rules (aligned with platform_adapter seed)
│   └── chatgpt.ts            # parse rules (message.content.parts / metadata.citations)
├── lib/                      # API client, storage, types, platforms registry
└── popup/index.tsx           # node status UI
```

## Security

- Only processes tasks returned by poll — no other tab/session upload
- Node key header required; must be on server allowlist
- No local persistence of business payloads beyond runtime status

## References

- `AGENTS.md` §9
- `PRD_商业化版_V2.0.md` §7.6
- Java: `ProbeController` · `deploy/scripts/test_probe_extension_e2e.py`
