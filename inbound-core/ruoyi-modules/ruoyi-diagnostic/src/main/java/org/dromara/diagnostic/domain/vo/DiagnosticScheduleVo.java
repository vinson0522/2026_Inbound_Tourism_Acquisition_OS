package org.dromara.diagnostic.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * GEO 诊断定时计划（FR-109）
 */
@Data
public class DiagnosticScheduleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long projectId;

    /** 是否已保存过计划 */
    private Boolean configured;

    private Boolean enabled;

    private String frequency;

    private String market;

    private String locale;

    private String region;

    private List<String> probeModes;

    private List<String> models;

    private Integer sampleCount;

    private BigDecimal calibrationRatio;

    /** 展示用：all / stage / custom */
    private String questionScope;

    private OffsetDateTime nextRunAt;

    private OffsetDateTime lastTriggeredAt;

    private Long lastRunId;

    private String lastRunName;
}
