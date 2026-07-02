package org.dromara.project.billing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * FR-804 quota keys — aligned with PRD §13 and {@code subscription.quota_json}.
 */
@Getter
@RequiredArgsConstructor
public enum QuotaType {

    PROJECTS("projects", "客户项目数", "个", "total"),
    DIAGNOSTICS_PER_MONTH("diagnostics_per_month", "GEO 诊断", "次", "monthly"),
    KEYWORDS_PER_MONTH("keywords_per_month", "关键词生成", "次", "monthly"),
    CONTENT_PER_MONTH("content_per_month", "内容生成", "次", "monthly"),
    LANDING_PAGES_PER_MONTH("landing_pages_per_month", "落地页生成", "次", "monthly"),
    REPORTS_PER_MONTH("reports_per_month", "周报生成", "次", "monthly");

    private final String key;
    private final String label;
    private final String unit;
    private final String period;
}
