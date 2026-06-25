# 架构与产品决策日志（ADR 轻量版）

> 技术总监主笔；开发/UI/运维可 **提议**，仅技术总监或用户可 **定案**。  
> 所有 Agent 实施前扫一眼最近 5 条，避免推翻已定决策。

---

## 记录格式

```markdown
### ADR-YYYYMMDD-NN | 标题
- **状态**：已采纳 / 已废弃 / 待讨论
- **决策者**：技术总监 / 用户
- **背景**：…
- **决策**：…
- **影响**：涉及目录/EPIC/角色 …
```

---

## 已采纳

### ADR-20260623-01 | 业务底座选用 RuoYi-Vue-Plus
- **状态**：已采纳
- **决策者**：用户 + 文档基线
- **背景**：需要多租户 IAM、代码生成、快速 CRUD
- **决策**：拉取 RuoYi-Vue-Plus 5.X + plus-ui；**不**采用若依内置业务表
- **影响**：`inbound-core`、`inbound-admin`；业务表以 `001_schema.sql` 为准

### ADR-20260623-02 | MVP 数据库统一 PostgreSQL
- **状态**：已采纳
- **决策者**：ARCHITECTURE / PRD
- **背景**：业务 + pgvector 向量合一
- **决策**：不用 MySQL；若依需改数据源
- **影响**：运维 compose、开发 Flyway、若依配置

### ADR-20260625-03 | 多 Agent 共享记忆走文件而非会话
- **状态**：已采纳
- **决策者**：用户
- **背景**：多 Cursor 窗口互不共享上下文
- **决策**：`docs/agent-team/MEMORY.md` + `DECISIONS.md` + `HANDOFFS/`
- **影响**：所有角色；见 `AGENTS.md` §21

### ADR-20260625-04 | 若依与业务表共用单库 PostgreSQL
- **状态**：已采纳
- **决策者**：技术总监
- **背景**：ADR-02 已定 MVP 统一 PG；若依默认 MySQL；`001_schema.sql` 已通过 compose init 写入 `inbound_growth`
- **决策**：**单库** `inbound_growth`（`public` schema）；若依系统表走 RuoYi 自带 Flyway/SQL 脚本；业务表保持 `001_schema.sql` 命名；**不做** MySQL 双库过渡
- **影响**：运维 compose 验证；开发改 `application-dev.yml` + PG driver；Langfuse 同库共存

---

## 待讨论

- （暂无）
