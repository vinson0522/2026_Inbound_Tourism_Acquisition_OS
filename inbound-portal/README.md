# inbound-portal

客户只读门户（Vue 3 或 Nuxt SSG）。

## 职责

报告查看、任务进度、部分诊断结果 — **只读，不直连 LLM**。

## 目标结构

```
inbound-portal/
├── src/
│   ├── api/
│   ├── views/         # 报告预览、进度页
│   ├── components/
│   └── router/
├── public/
└── package.json       # scaffold 时生成
```

## 鉴权

只读 JWT 或 Magic Link（由 `inbound-core` auth 模块签发）。

## 状态

⏳ **待 scaffold** — 目录骨架已就绪。
