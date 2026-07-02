package org.dromara.diagnostic.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 诊断校准对比汇总 — EPIC-11 FR-115
 */
@Data
public class DiagnosticCalibrationVo {

    private BigDecimal deviationRate;

    private BigDecimal brandMentionAgreementRate;

    private Integer sampleCount;

    private Integer pairedCount;

    private List<DiagnosticCalibrationPairVo> pairs = new ArrayList<>();
}
