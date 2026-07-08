# HANDOFF | 技术总监 → 开发（夜间一次性总任务）

> **本文件自包含**：接手 agent **无需**其他会话上下文。按顺序执行 T1→T2→T3，全部完成后按 §验收 自测并更新 MEMORY。
> **优先级**：用户指令 > AGENTS.md > CLAUDE.md > 本文件。

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-08 | Sprint #4 收尾 · B-26 / B-27 |

---

## 0. 背景（Single Source of Truth）

- 共享记忆：`docs/agent-team/MEMORY.md`、`DECISIONS.md`、`HANDOFFS/`。**开始读、结束写**。
- 远程当前 HEAD：`origin/main` = C26 `5875276` + docs `ea11612`。
- Sprint #4 四项交付**已实质完成并验通**（C26 push · LOCAL_DOCKER 文档 · Admin 工作台深链 · Landing compose :4321 curl 200），但**增量文件尚未 commit**（B-26），且 smoke 因 demo 额度耗尽掉到 **8/10**（B-27）。
- 三方 live（Perplexity Chrome / Gemini 真 E2E）= **B-23 挂起**，**今晚不做**，失败也不阻塞。

### 环境前提（执行前确认）

```powershell
# Docker：postgres / redis / rabbitmq / inbound-ai 需 healthy
docker ps --format "{{.Names}} {{.Status}}" | Select-String inbound
# 若缺，启动（含 mock LLM 的 local-d 覆盖）
cd deploy
docker compose -f docker-compose.yml -f docker-compose.local-d.yml up -d postgres redis rabbitmq ai-api gotenberg inbound-landing
# Java 业务后端需在 :8080（另一终端）
#   cd inbound-core && mvnw spring-boot:run  （或 ruoyi-admin 模块启动类）
# 会话内强制 mock：ai-api 已默认 DIAGNOSE_MOCK_LLM=true / EMBED_MOCK=true
```

---

## T1 · B-27 修复 smoke 8/10 → 10/10（额度）

### 根因（已定位，勿再排查）

- `QuotaServiceImpl.checkAndConsume`：创建项目 / 发起诊断会累加 `subscription.used_json`。
- seed（`database/ddl/002_seed_demo.sql` L14-23）tenant 1 限额：`projects:5`、`diagnostics_per_month:4`。
- smoke `test_projects_api.py`（POST 创建项目）与 `test_diagnostic_e2e.py`（POST 诊断）**每跑一次就消耗额度**，多轮后 `used ≥ limit` → `code=40201 套餐额度不足`。
- 现象：`8/10 passed · FAILED: test_projects_api.py, test_diagnostic_e2e.py`。

### 交付（按此实现，二选一以「A 幂等重置」为主）

- [ ] **A（主）**：新增可重复执行的额度重置 SQL `database/ddl/007_smoke_quota_reset.sql`：
  - 将 tenant 1 的 **ACTIVE** subscription `used_json` 归零：`UPDATE subscription SET used_json = '{}'::jsonb, updated_at = now() WHERE tenant_id = 1 AND status = 'ACTIVE'::subscription_status;`
  - 可选：同时把 `period_start/period_end` 顺延到当月，避免过期。
- [ ] **B（配套）**：让 `run_smoke_regression.ps1` 在跑创建类脚本**前**自动执行该重置，保证 10/10 可重复：
  - 在脚本顶部（登录前）用 `docker exec inbound-postgres psql -U inbound -d inbound_growth -f /ddl/007_smoke_quota_reset.sql` 或等价 `-c "UPDATE ..."`。
  - 若 `database/ddl` 未挂载进容器，直接用内联 `-c` SQL，勿依赖挂载路径。
  - 保持脚本对「已存在项目/诊断」幂等，不得因重复创建报错。
- [ ] 重跑：`.\deploy\scripts\run_smoke_regression.ps1` → **10/10 passed**（贴 SUMMARY 到本 HANDOFF Done）。

> **勿**改 `QuotaServiceImpl` 计费逻辑（那是业务正确行为）；只做 smoke 侧可重复化 + demo 额度重置。

---

## T2 · B-26 入库 C27（Sprint #4 增量）

### 待入库工作区改动（`git status` 已确认）

```
 M deploy/LOCAL_DOCKER.md
 M deploy/docker-compose.yml
 M docs/agent-team/MEMORY.md
 M inbound-admin/src/views/tourgeo/dashboard/index.vue
 M inbound-landing/Dockerfile
 M inbound-landing/README.md
?? deploy/scripts/README.md
?? docs/agent-team/HANDOFFS/2026-07-11-*.md（Sprint #4 相关，全部纳入）
?? docs/agent-team/HANDOFFS/2026-07-08-tech-director-c27-b27-nightrun-full.md（本文件）
（新增）database/ddl/007_smoke_quota_reset.sql（T1 产物）
（新增/修改）deploy/scripts/run_smoke_regression.ps1（T1 产物）
```

### 交付

- [ ] `git add` **仅** Sprint #4 + T1 相关文件；**排除**：
  - ❌ `inbound-probe-extension/.chrome-live-profile/`（本地 Chrome profile，绝不提交；若未 ignore，顺手加进 `.gitignore`）
  - ❌ `deploy/scripts/verify_perplexity_live.py`（B-23 三方 scaffold，**不进 C27**，留工作区）
- [ ] commit message（HEREDOC，聚焦「为什么」）：
  ```
  feat(devops,admin,landing): C27 Sprint #4 closeout — smoke 10/10 quota reset, docs, dashboard deep links

  - B-27: 007_smoke_quota_reset.sql + run_smoke_regression.ps1 reset demo quota → repeatable 10/10
  - devops: LOCAL_DOCKER §2.9 one-click smoke, deploy/scripts/README index
  - admin: FR-006 dashboard deep links to /diagnostics/trends and run detail
  - landing: marketing portal Docker build args + README local run
  - docs: Sprint #4 HANDOFFs
  ```
- [ ] `git push origin main`（**不** force）。
- [ ] 记录 C27 hash 到本 HANDOFF Done 与 MEMORY 执行日志。

> **不要** amend C26 `5875276`（已 push）。C27 为**新** commit。

---

## T3 · 回归 + 文档收口

- [ ] 重跑 `run_smoke_regression.ps1` → **10/10**（T1 之后应稳定可重复）。
- [ ] Landing 门户回归：`curl.exe -s -o NUL -w "%{http_code}" http://localhost:4321/` → **200**（含 `TourGEO` 文案）。
- [ ] Admin 构建：`cd inbound-admin && pnpm build:prod` → 成功。
- [ ] 更新 `docs/agent-team/MEMORY.md`：
  - 头部 **Git 远程** → C27 `<hash>`；**最后更新/更新角色** = 2026-07-08 / 开发。
  - 阻塞项：**B-26 关闭**（C27 `<hash>`）、**B-27 关闭**（smoke 10/10 可重复）。
  - 执行日志追加一行 C27。
  - Sprint #4 表 6「LOCAL_DOCKER 文档」→ ✅ 已入库。
- [ ] 本 HANDOFF 填 Done。

---

## 验收标准（全绿才算完成）

- [ ] `run_smoke_regression.ps1` 连续跑 **两次** 都 **10/10**（证明幂等，非一次性）。
- [ ] `git status` 干净（除 `verify_perplexity_live.py` / `.chrome-live-profile/` 有意保留/忽略）。
- [ ] `origin/main` 领先，含 C27；`git log -1` = C27。
- [ ] `curl :4321/` = 200；`pnpm build:prod` 成功。
- [ ] MEMORY / 本 HANDOFF 均已更新，B-26 / B-27 关闭。

## 不做 / 边界

- ❌ 不碰 B-23（Perplexity Chrome live / Gemini 真 E2E）。
- ❌ 不改 `QuotaServiceImpl` 计费语义、不改分层边界、不动无关模块。
- ❌ 不 force push；不提交 `.env` / Key / `.chrome-live-profile/`。
- ❌ 子 Agent 不写 MEMORY（仅本主窗口维护）。

---

## Done（由接手 agent 填写）

- **完成时间**：2026-07-08（开发窗口）
- **T1 smoke SUMMARY**（10/10）：
  - 新增 `database/ddl/007_smoke_quota_reset.sql`：tenant 1 **ACTIVE** subscription `used_json → '{}'` + `period_start/end` 顺延当月（幂等）。
  - `run_smoke_regression.ps1`：创建脚本**前**用 `docker exec inbound-postgres psql -U inbound -d inbound_growth -c "<inline UPDATE>"` 重置额度（不依赖 ddl 挂载路径；容器名/库可用 `SMOKE_PG_*` 覆盖）。
  - **连续两次** `run_smoke_regression.ps1` 均 **10/10 passed**（证明幂等）：
    ```
    === SMOKE SUMMARY ===
    10/10 passed
    All smoke checks passed.
    ```
    关键项：`test_projects_api` id=12（额度重置后不再 40201）· `test_diagnostic_e2e` runId=103 SUCCESS geo_score=85.00（mock）· `test_tenant_isolation` 4 端点 403。
  - **未改** `QuotaServiceImpl` 计费语义（仅 smoke 侧可重复化 + demo 额度重置）。
  - 说明：修复 40201 后曾遇 `test_diagnostic_e2e` FAILED —— 根因 ai-api 容器 `DIAGNOSE_MOCK_LLM=false`（`deploy/.env` 覆盖）调真 Gemini 触发配额（B-23 域）。按文档前提以 `DIAGNOSE_MOCK_LLM=true` 重建 ai-api 容器（会话级，未改任何入库文件）后稳定 10/10。
- **T2 C27 hash**：`d7f042a`（`git push origin main` 非 force）
  - `git add` 仅 Sprint #4 + T1 相关；**排除** `inbound-probe-extension/.chrome-live-profile/`（并已加入 `.gitignore`）与 `deploy/scripts/verify_perplexity_live.py`（B-23，留工作区）。
- **T3 curl / build:prod 结果**：
  - `curl :4321/` → **HTTP 200** · 含 `TourGEO` 营销文案。
  - `cd inbound-admin && pnpm build:prod` → **成功**（dist 产物完整，trends/dashboard chunk 均产出）。
- **遗留**：
  - B-23 三方 live（Perplexity Chrome / Gemini 真 E2E）仍挂起（未触碰）。
  - smoke 极快连跑 + 手动重复登录会短暂触发 `/auth/code` 每 IP 限流（`data=None`），等待约 60s 窗口恢复即 10/10；非代码回归。
