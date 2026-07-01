# HANDOFF | 技术总监 → 运维

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 运维 | 2026-07-03 | EPIC-6 M2 · inbound-landing |

## 交付

- [x] `inbound-landing/Dockerfile`（Node 20 · `pnpm build` · preview/serve `:4321`）
- [x] `deploy/docker-compose.yml` 或 `docker-compose.local-d.yml` 增加 `inbound-landing` 服务
- [x] 环境变量文档（`deploy/.env.example` 或 `TECH_STACK` 补充）：
  - `PUBLIC_API_BASE_URL=http://localhost:8080`（Astro → Java）
  - `TURNSTILE_SITE_KEY` / `TURNSTILE_SECRET_KEY`（可选；无则 skip）
  - `LANDING_PUBLIC_BASE_URL=http://localhost:4321`
- [x] Java CORS 与 landing 同源联调说明写入 `deploy/LOCAL_DOCKER.md` 一节
- [x] 验收：`docker compose up -d inbound-landing` → `curl http://localhost:4321/` 200

## Prompt

```
角色：运维。必读 EPIC-6 M2 Sprint、inbound-landing README。
任务：Astro 容器 :4321 + env 文档；不阻塞 Java/Admin 可先 scaffold 后集成。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-03
- **结果摘要**：`inbound-landing/Dockerfile`（Node 20 · pnpm build · preview :4321）· `docker-compose.yml` + `local-d.yml` 服务 · `deploy/.env.example` / `.env.local.example` / `TECH_STACK` §4.4 · `LOCAL_DOCKER.md` EPIC-6 M2 节 · 验收 `curl http://localhost:4321/` → **200** · container **healthy**
- **遗留**：公网路由 `/p/...` 与 Turnstile 表单由开发 Landing/Java 补齐；生产 staging 尚未部署 inbound-landing
