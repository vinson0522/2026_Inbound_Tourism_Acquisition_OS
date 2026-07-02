package org.dromara.project.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.project.domain.Subscription;
import org.dromara.project.mapper.SubscriptionMapper;
import org.dromara.project.service.ISubscriptionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日 02:00 扫描到期订阅并重置月度 used_json — EPIC-9 M2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionPeriodResetJob {

    private final SubscriptionMapper subscriptionMapper;
    private final ISubscriptionService subscriptionService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void resetExpiredSubscriptions() {
        LocalDate today = LocalDate.now();
        List<Subscription> expired = subscriptionMapper.selectList(
            Wrappers.lambdaQuery(Subscription.class)
                .apply("status = 'ACTIVE'::subscription_status")
                .isNull(Subscription::getDeletedAt)
                .lt(Subscription::getPeriodEnd, today)
        );
        if (expired.isEmpty()) {
            return;
        }
        log.info("subscription_period_reset_job candidates={}", expired.size());
        for (Subscription sub : expired) {
            try {
                subscriptionService.resetPeriod(sub.getTenantId());
            } catch (Exception e) {
                log.error("subscription_period_reset_job failed tenantId={} subId={}", sub.getTenantId(), sub.getId(), e);
            }
        }
    }
}
