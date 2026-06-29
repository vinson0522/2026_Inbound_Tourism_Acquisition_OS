package org.dromara.diagnostic.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * FR-108 单次诊断趋势数据点。
 */
@Data
public class DiagnosticTrendVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long runId;

    private String name;

    private BigDecimal geoScore;

    private OffsetDateTime finishedAt;

    private String market;

    private String status;

    private DiagnosticTrendMetricsVo metrics;
}
