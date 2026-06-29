package org.dromara.diagnostic.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * FR-108 趋势图六分项指标（0–1 比率，与 diagnostic-detail / PRD GEO 公式一致）。
 */
@Data
public class DiagnosticTrendMetricsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Double brandMentionRate;

    private Double top3Rate;

    private Double competitorSuppression;

    private Double citationCoverage;

    private Double longtailCoverage;

    private Double assetCompleteness;
}
