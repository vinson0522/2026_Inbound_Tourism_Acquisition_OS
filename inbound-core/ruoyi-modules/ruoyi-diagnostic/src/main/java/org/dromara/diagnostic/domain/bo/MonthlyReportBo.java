package org.dromara.diagnostic.domain.bo;

import lombok.Data;

/**
 * 手动月报生成 — EPIC-8 FR-703
 */
@Data
public class MonthlyReportBo {

    /** 自然年，与 month 成对使用 */
    private Integer year;

    /** 自然月 1-12 */
    private Integer month;

    /** yyyy-MM-dd，与 periodEnd 成对；最长 62 天 */
    private String periodStart;

    /** yyyy-MM-dd */
    private String periodEnd;
}
