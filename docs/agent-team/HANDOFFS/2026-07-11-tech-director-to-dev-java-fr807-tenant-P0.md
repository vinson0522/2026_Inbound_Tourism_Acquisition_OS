# HANDOFF | 技术总监 → 开发（Java）· **P0 重派**

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 Java | 2026-07-11 | **EPIC-1 FR-807** · [重排决策](2026-07-11-tech-director-sprint2-reprioritize-no-third-party.md) |

> ⚠️ **Perplexity live / Gemini 真 E2E 已挂起**。本 HANDOFF 为当前唯一 P0，勿再耗在 Chrome+Perplexity 上。

## 上下文

`BusinessTenantHelper` 在 `ruoyi-project` 与 `ruoyi-diagnostic` 各有一份，逻辑相同：若依 tenant `000000` → 固定 `business tenant_id=1L`。

```java
// ruoyi-project/.../BusinessTenantHelper.java
public static Long getBusinessTenantId() {
    String ruoyiTenantId = TenantHelper.getTenantId();
    if (StringUtils.isBlank(ruoyiTenantId) || DEFAULT_RUOYI_TENANT.equals(ruoyiTenantId)) {
        return DEFAULT_BUSINESS_TENANT_ID;  // 1L — FR-807 缺口
    }
    ...
}
```

**相关文件**：
- `inbound-core/ruoyi-modules/ruoyi-project/src/main/java/org/dromara/project/support/BusinessTenantHelper.java`
- `inbound-core/ruoyi-modules/ruoyi-diagnostic/src/main/java/org/dromara/diagnostic/support/BusinessTenantHelper.java`
- `database/ddl/001_schema.sql` — `tenant` 表 · 各业务表 `tenant_id`
- `PRD_商业化版_V2.0.md` FR-807

## 交付请求

- [x] **统一** 两处 `BusinessTenantHelper`（或抽到 common 模块，最小 diff 优先）
- [x] 从登录用户/若依租户映射 **真实** `tenant.id`（可 seed 第二租户 + 测试用户）
- [x] 集成测试：tenant A 用户 `GET /api/v1/projects/{B_projectId}` → **403** 或 404（不泄露）
- [x] 现有 smoke 仍可用（demo tenant=1 默认登录）
- [x] `run_smoke_regression.ps1` 回归仍 9/9（或说明需改 smoke 登录）

## 验收

```bash
cd inbound-core
mvn test -pl ruoyi-modules/ruoyi-project -Dtest=*Tenant* -am
python deploy/scripts/test_tenant_isolation.py   # projectId=8 → api code 403
python deploy/scripts/run_smoke_regression.ps1   # 9/9
```

## Prompt（开发窗口首行）

```
角色：开发 Java。必读 MEMORY.md、HANDOFF 2026-07-11-tech-director-to-dev-java-fr807-tenant-P0.md、BusinessTenantHelper、FR-807。Perplexity live 已挂起勿做。任务：业务 tenant_id 从登录解析 · 跨 tenant 403 测试 · 更新 HANDOFF Done + MEMORY。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-11
- **结果摘要**：
  - 统一 `BusinessTenantHelper` → `ruoyi-common-tenant` · `BusinessTenantLookup` + `BusinessTenantLookupImpl`
  - DDL `tenant.ruoyi_tenant_id` · `006_fr807_tenant_mapping.sql` · seed tenant B（`000001`）+ projectId=8 · RuoYi user `tenantb`/admin123
  - 单元测试：`BusinessTenantLookupImplTest`（5）· `CustomerProjectTenantIsolationTest`（3）
  - HTTP：`test_tenant_isolation.py` — admin GET `/projects/8` → `api.code=403`
  - `run_smoke_regression.ps1` **9/9** ✅
- **遗留**：重启 Java 后 `BusinessTenantLookup` 从 DB 解析生效（旧进程仍靠 `getOwnedProjectOrThrow` 403）；可选 C25 commit
