package org.dromara.diagnostic.domain.vo;

import lombok.Data;

/**
 * 校准对比单侧结果摘要 — EPIC-11 FR-115
 */
@Data
public class DiagnosticCalibrationSideVo {

    private Long resultId;

    private String probeNodeKey;

    private String answerPreview;

    private Boolean brandMentioned;

    private Integer rank;

    private Integer citationCount;
}
