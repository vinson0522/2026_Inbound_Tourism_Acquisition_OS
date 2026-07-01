package org.dromara.diagnostic.domain.bo;

import lombok.Data;

/**
 * 手动周报生成 — EPIC-8 FR-702
 */
@Data
public class WeeklyReportBo {

    /** yyyy-MM-dd，默认近 7 日 */
    private String periodStart;

    /** yyyy-MM-dd */
    private String periodEnd;
}
