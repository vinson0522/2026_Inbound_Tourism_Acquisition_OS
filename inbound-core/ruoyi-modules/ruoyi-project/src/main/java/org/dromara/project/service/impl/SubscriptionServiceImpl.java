package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.project.billing.QuotaType;
import org.dromara.project.domain.Subscription;
import org.dromara.project.domain.vo.QuotaItemVo;
import org.dromara.project.domain.vo.SubscriptionVo;
import org.dromara.project.mapper.SubscriptionMapper;
import org.dromara.project.service.ISubscriptionService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements ISubscriptionService {

    private static final Map<String, String> PLAN_LABELS = Map.of(
        "growth_service", "增长服务版",
        "trial", "试用版",
        "starter", "入门版",
        "enterprise", "企业版"
    );

    private final SubscriptionMapper subscriptionMapper;

    @Override
    public SubscriptionVo getCurrentSubscription(Long tenantId) {
        Subscription sub = subscriptionMapper.selectOne(
            Wrappers.lambdaQuery(Subscription.class)
                .eq(Subscription::getTenantId, tenantId)
                .apply("status = 'ACTIVE'::subscription_status")
                .isNull(Subscription::getDeletedAt)
                .orderByDesc(Subscription::getId)
                .last("LIMIT 1")
        );
        if (sub == null) {
            throw new ServiceException("暂无有效套餐", HttpStatus.NOT_FOUND);
        }
        return toVo(sub);
    }

    private SubscriptionVo toVo(Subscription sub) {
        SubscriptionVo vo = new SubscriptionVo();
        vo.setPlanCode(sub.getPlanCode());
        vo.setPlanName(PLAN_LABELS.getOrDefault(sub.getPlanCode(), sub.getPlanCode()));
        vo.setStatus(sub.getStatus());
        if (sub.getPeriodStart() != null) {
            vo.setPeriodStart(sub.getPeriodStart().toString());
        }
        if (sub.getPeriodEnd() != null) {
            vo.setPeriodEnd(sub.getPeriodEnd().toString());
            long days = ChronoUnit.DAYS.between(LocalDate.now(), sub.getPeriodEnd());
            vo.setDaysRemaining((int) Math.max(days, 0));
        }

        Map<String, Object> quotaJson = sub.getQuotaJson() != null ? sub.getQuotaJson() : Map.of();
        Map<String, Object> usedJson = sub.getUsedJson() != null ? sub.getUsedJson() : Map.of();

        List<QuotaItemVo> quotas = new ArrayList<>();
        List<String> overageKeys = new ArrayList<>();
        boolean hasWarning = false;

        for (QuotaType type : QuotaType.values()) {
            int limit = QuotaServiceImpl.readInt(quotaJson.get(type.getKey()));
            int used = QuotaServiceImpl.readInt(usedJson.get(type.getKey()));
            QuotaItemVo item = new QuotaItemVo();
            item.setKey(type.getKey());
            item.setLabel(type.getLabel());
            item.setUsed(used);
            item.setLimit(limit);
            item.setUnit(type.getUnit());
            item.setPeriod(type.getPeriod());

            if (limit > 0) {
                int pct = Math.min(100, (int) Math.round(used * 100.0 / limit));
                item.setPercentage(pct);
                if (used >= limit) {
                    item.setStatus("overage");
                    overageKeys.add(type.getKey());
                } else if (pct >= 90) {
                    item.setStatus("warning");
                    hasWarning = true;
                } else {
                    item.setStatus("normal");
                }
            } else {
                item.setPercentage(0);
                item.setStatus("normal");
            }
            quotas.add(item);
        }

        vo.setQuotas(quotas);
        vo.setOverageKeys(overageKeys);
        vo.setHasOverage(!overageKeys.isEmpty());
        vo.setHasWarning(hasWarning);
        return vo;
    }
}
