package org.dromara.project.service;

import org.dromara.project.domain.bo.SubscriptionUpdateBo;
import org.dromara.project.domain.vo.SubscriptionVo;

public interface ISubscriptionService {

    SubscriptionVo getCurrentSubscription(Long tenantId);

    SubscriptionVo updateSubscription(Long tenantId, SubscriptionUpdateBo bo);

    SubscriptionVo resetPeriod(Long tenantId);
}
