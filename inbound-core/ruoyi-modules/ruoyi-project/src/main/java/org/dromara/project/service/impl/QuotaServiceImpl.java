package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.project.billing.QuotaExceededException;
import org.dromara.project.billing.QuotaType;
import org.dromara.project.domain.Subscription;
import org.dromara.project.mapper.SubscriptionMapper;
import org.dromara.project.service.IQuotaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaServiceImpl implements IQuotaService {

    private final SubscriptionMapper subscriptionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndConsume(Long tenantId, QuotaType type, int amount) {
        if (tenantId == null || type == null || amount <= 0) {
            return;
        }

        Subscription sub = findActiveSubscription(tenantId);
        if (sub == null) {
            log.warn("No ACTIVE subscription for tenantId={}; skipping quota consume for {}", tenantId, type.getKey());
            return;
        }

        Map<String, Object> quotaJson = sub.getQuotaJson() != null ? sub.getQuotaJson() : Map.of();
        Map<String, Object> usedJson = new HashMap<>(sub.getUsedJson() != null ? sub.getUsedJson() : Map.of());

        String key = type.getKey();
        int limit = readInt(quotaJson.get(key));
        int used = readInt(usedJson.get(key));

        if (limit > 0 && used + amount > limit) {
            throw new QuotaExceededException();
        }

        usedJson.put(key, used + amount);
        sub.setUsedJson(usedJson);
        sub.setUpdatedAt(OffsetDateTime.now());
        subscriptionMapper.updateById(sub);
    }

    @Override
    public boolean hasRemainingQuota(Long tenantId, QuotaType type, int amount) {
        if (tenantId == null || type == null || amount <= 0) {
            return true;
        }
        Subscription sub = findActiveSubscription(tenantId);
        if (sub == null) {
            return true;
        }
        Map<String, Object> quotaJson = sub.getQuotaJson() != null ? sub.getQuotaJson() : Map.of();
        Map<String, Object> usedJson = sub.getUsedJson() != null ? sub.getUsedJson() : Map.of();
        String key = type.getKey();
        int limit = readInt(quotaJson.get(key));
        if (limit <= 0) {
            return true;
        }
        int used = readInt(usedJson.get(key));
        return used + amount <= limit;
    }

    private Subscription findActiveSubscription(Long tenantId) {
        return subscriptionMapper.selectOne(
            Wrappers.lambdaQuery(Subscription.class)
                .eq(Subscription::getTenantId, tenantId)
                .apply("status = 'ACTIVE'::subscription_status")
                .isNull(Subscription::getDeletedAt)
                .orderByDesc(Subscription::getId)
                .last("LIMIT 1")
        );
    }

    public static int readInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
