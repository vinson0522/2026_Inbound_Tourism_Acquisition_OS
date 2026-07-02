package org.dromara.project.service;

import org.dromara.project.domain.vo.SubscriptionVo;

public interface ISubscriptionService {

    SubscriptionVo getCurrentSubscription(Long tenantId);
}
