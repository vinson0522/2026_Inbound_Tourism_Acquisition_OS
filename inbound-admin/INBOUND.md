# inbound-admin — 底座说明

本目录基于 **[plus-ui](https://github.com/JavaLionLi/plus-ui)** 拉取，为 RuoYi-Vue-Plus 5.X 的 **官方 Vue3 管理后台**。

| 项 | 值 |
|----|-----|
| 上游仓库 | https://github.com/JavaLionLi/plus-ui |
| 版本 | **5.6.2-2.6.2**（`package.json`） |
| 拉取日期 | 2026-06-25 |
| 技术栈 | Vue 3 + Element Plus + Vite + TypeScript |
| 上游 README | 见同目录 `README.md` |

## 与本项目的关系

- 对应 PRD §6.1 管理后台导航，后续在 `src/views` 增加 GEO / 关键词 / 内容等业务页
- API 代理指向 `inbound-core`（`ruoyi-admin`），见 `.env.development` 中 `VITE_APP_BASE_API`
- **不直连 LLM**；AI 能力走 `inbound-ai`

## 本地启动

```bash
cd inbound-admin
pnpm install   # 或 npm install
pnpm dev
```

开发环境默认通过 Vite 代理访问后端 `/dev-api`。需先启动 `inbound-core/ruoyi-admin`。
