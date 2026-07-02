package org.dromara.project.support;

import org.dromara.project.billing.QuotaType;
import org.dromara.project.domain.Subscription;
import org.dromara.project.service.impl.QuotaServiceImpl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 套餐周期推进与 used_json 月度重置 — ADR-20260709-23
 */
public final class SubscriptionPeriodSupport {

    public static final Set<String> PLAN_CODES = Set.of(
        "diagnostic_report",
        "basic_saas",
        "growth_service",
        "trial",
        "oem_private",
        "starter",
        "enterprise"
    );

    private SubscriptionPeriodSupport() {
    }

    public static Map<String, Object> normalizeQuotaJson(Map<String, Object> raw) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (QuotaType type : QuotaType.values()) {
            int value = QuotaServiceImpl.readInt(raw != null ? raw.get(type.getKey()) : null);
            if (value < 0) {
                throw new IllegalArgumentException(type.getKey() + " 不能为负数");
            }
            normalized.put(type.getKey(), value);
        }
        return normalized;
    }

    /**
     * 当 {@code period_end < today} 时循环推进至当前周期。
     */
    public static void advancePeriodIfExpired(Subscription sub, LocalDate today) {
        if (sub.getPeriodEnd() == null) {
            return;
        }
        while (sub.getPeriodEnd().isBefore(today)) {
            LocalDate nextStart = sub.getPeriodEnd();
            sub.setPeriodStart(nextStart);
            sub.setPeriodEnd(nextStart.plusMonths(1));
        }
    }

    /**
     * 月度额度键归零；{@code projects} 累计用量保留。
     */
    public static void resetMonthlyUsed(Subscription sub) {
        Map<String, Object> used = new HashMap<>(sub.getUsedJson() != null ? sub.getUsedJson() : Map.of());
        for (QuotaType type : QuotaType.values()) {
            if ("monthly".equals(type.getPeriod())) {
                used.put(type.getKey(), 0);
            }
        }
        sub.setUsedJson(used);
    }
}
