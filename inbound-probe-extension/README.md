# inbound-probe-extension

Plasmo Chrome MV3 浏览器 GEO 探针扩展。

## 职责

拉取探针任务 → 在 AI 网页版自动提问 → hook fetch/SSE 抓取 citations → 上报后台。仅处理调度下发的 `probe_task`，不上传其他会话。

## 目标结构

```
inbound-probe-extension/
├── src/
│   ├── background.ts      # 任务轮询
│   ├── contents/          # 各平台 content script
│   ├── adapters/          # platform_adapter 本地缓存
│   └── popup/             # 节点状态 UI
├── plasmo.config.ts
└── package.json
```

## API

- `GET /api/v1/probe/tasks/poll` — 拉任务
- `POST /api/v1/probe/tasks/{id}/result` — 上报
- `POST /api/v1/probe/nodes/register` — 注册/心跳

## 对应 EPIC

EPIC-11 浏览器扩展探针

## 参考

- `PRD_商业化版_V2.0.md` §7.6
- `AGENT.md` §9

## 状态

⏳ **待 scaffold**
