package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.project.billing.QuotaType;
import org.dromara.project.domain.Subscription;
import org.dromara.project.domain.bo.SubscriptionUpdateBo;
import org.dromara.project.domain.vo.QuotaItemVo;
import org.dromara.project.domain.vo.SubscriptionVo;
import org.dromara.project.mapper.SubscriptionMapper;
import org.dromara.project.service.ISubscriptionService;
import org.dromara.project.support.SubscriptionPeriodSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements ISubscriptionService {

    private static final Map<String, String> PLAN_LABELS = Map.ofEntries(
        Map.entry("diagnostic_report", "诊断报告版"),
        Map.entry("basic_saas", "基础 SaaS 版"),
        Map.entry("growth_service", "增长服务版"),
        Map.entry("trial", "试用版"),
        Map.entry("oem_private", "OEM/私有化版"),
        Map.entry("starter", "入门版"),
        Map.entry("enterprise", "企业版")
    );

    private final SubscriptionMapper subscriptionMapper;

    @Override
    public SubscriptionVo getCurrentSubscription(Long tenantId) {
        Subscription sub = requireActiveSubscription(tenantId);
        return toVo(sub);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubscriptionVo updateSubscription(Long tenantId, SubscriptionUpdateBo bo) {
        if (!SubscriptionPeriodSupport.PLAN_CODES.contains(bo.getPlanCode())) {
            throw new ServiceException("无效的 planCode: " + bo.getPlanCode(), HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> quotaJson;
        try {
            quotaJson = SubscriptionPeriodSupport.normalizeQuotaJson(bo.getQuotaJson());
        } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        Subscription sub = requireActiveSubscription(tenantId);
        LocalDate periodStart = sub.getPeriodStart();
        LocalDate periodEnd = sub.getPeriodEnd();

        if (StringUtils.isNotBlank(bo.getPeriodStart())) {
            periodStart = LocalDate.parse(bo.getPeriodStart());
        }
        if (StringUtils.isNotBlank(bo.getPeriodEnd())) {
            periodEnd = LocalDate.parse(bo.getPeriodEnd());
        }
        if (periodStart == null || periodEnd == null) {
            throw new ServiceException("periodStart 与 periodEnd 不能为空", HttpStatus.BAD_REQUEST);
        }
        if (!periodEnd.isAfter(periodStart)) {
            throw new ServiceException("periodEnd 必须晚于 periodStart", HttpStatus.BAD_REQUEST);
        }

        sub.setPlanCode(bo.getPlanCode());
        sub.setQuotaJson(quotaJson);
        sub.setPeriodStart(periodStart);
        sub.setPeriodEnd(periodEnd);
        sub.setUpdatedAt(OffsetDateTime.now());
        subscriptionMapper.updateById(sub);

        log.info(
            "subscription_updated tenantId={} subId={} planCode={} quotaJson={} periodStart={} periodEnd={}",
            tenantId, sub.getId(), sub.getPlanCode(), quotaJson, periodStart, periodEnd
        );
        return toVo(sub);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubscriptionVo resetPeriod(Long tenantId) {
        Subscription sub = requireActiveSubscription(tenantId);
        LocalDate today = LocalDate.now();
        LocalDate oldPeriodEnd = sub.getPeriodEnd();

        SubscriptionPeriodSupport.advancePeriodIfExpired(sub, today);
        SubscriptionPeriodSupport.resetMonthlyUsed(sub);
        sub.setUpdatedAt(OffsetDateTime.now());
        subscriptionMapper.updateById(sub);

        log.info(
            "subscription_period_reset tenantId={} subId={} oldPeriodEnd={} periodStart={} periodEnd={} usedJson={}",
            tenantId, sub.getId(), oldPeriodEnd, sub.getPeriodStart(), sub.getPeriodEnd(), sub.getUsedJson()
        );
        return toVo(sub);
    }

    private Subscription requireActiveSubscription(Long tenantId) {
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
        return sub;
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
