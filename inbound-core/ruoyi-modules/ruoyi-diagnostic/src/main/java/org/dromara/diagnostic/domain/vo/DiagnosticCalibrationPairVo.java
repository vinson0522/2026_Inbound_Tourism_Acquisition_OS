package org.dromara.diagnostic.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 校准对比 question × platform 配对 — EPIC-11 FR-115
 */
@Data
public class DiagnosticCalibrationPairVo {

    private Long questionId;

    private String question;

    private String stage;

    private String platform;

    private Boolean brandMatch;

    private BigDecimal similarityScore;

    private BigDecimal deviationScore;

    private DiagnosticCalibrationSideVo groundedApi;

    private DiagnosticCalibrationSideVo browserExtension;
}
