package org.dromara.project.service;

import org.dromara.project.billing.QuotaType;

public interface IQuotaService {

    /**
     * 事务内校验额度并扣减 {@code used_json}；无 ACTIVE 订阅时 M1 放行（warn log）。
     */
    void checkAndConsume(Long tenantId, QuotaType type, int amount);

    /**
     * 仅检查剩余额度，不扣减（定时 Job 触发前 skip 用）。
     */
    boolean hasRemainingQuota(Long tenantId, QuotaType type, int amount);
}
