# HANDOFF | 技术总监 → 开发（Java）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 Java | 2026-07-11 | EPIC-1 · **FR-807** · [Sprint 索引](2026-07-11-tech-director-post-maintenance-b03b-sprint.md) |

## 上下文

EPIC-1 签核遗留：**租户隔离 FR-807 部分** — `BusinessTenantHelper.getBusinessTenantId()` 固定返回 `1L`，多租户业务 API 未从登录用户解析真实 `tenant_id`。

**相关文件**：
- `inbound-core/ruoyi-modules/*/src/main/java/**/support/BusinessTenantHelper.java`
- `database/ddl/001_schema.sql` — `tenant` · 各表 `tenant_id`
- `PRD_商业化版_V2.0.md` FR-807
- Casbin / MyBatis-Plus 租户插件（若依）

## 交付请求

- [ ] 从登录上下文（或用户-租户绑定表）解析 **business tenant_id**，替换硬编码 `1L`
- [ ] 所有 tourgeo 业务 Controller/Service 经 `BusinessTenantHelper` 取 tenant
- [ ] 集成测试：**tenant A 用户访问 tenant B 的 projectId → 403**
- [ ] seed/demo：至少 2 tenant 或测试 fixture 可构造跨租户用例
- [ ] 不破坏现有 smoke（默认 demo tenant=1 仍可用）

## 验收标准

```bash
# 新增或扩展 ruoyi-* 模块测试
mvn test -pl ruoyi-modules/ruoyi-project -Dtest=*Tenant* -am
```

- [ ] 跨 tenant GET project → 403 或 404（不泄露 B 的数据）
- [ ] MEMORY EPIC-1 验收表 FR-807 → ✅

## Prompt

```
角色：开发 Java。必读 BusinessTenantHelper、FR-807、001_schema tenant、HANDOFF 2026-07-11-tech-director-to-dev-java-fr807-tenant.md。
任务：业务 tenant_id 解析 + 跨租户 403 测试 · 最小 diff。
```

## Done（由 To 角色填写）

- **完成时间**：
- **结果摘要**：
- **遗留**：
