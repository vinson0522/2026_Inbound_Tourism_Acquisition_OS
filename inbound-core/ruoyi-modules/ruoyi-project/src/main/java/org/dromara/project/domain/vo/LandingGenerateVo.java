package org.dromara.project.domain.vo;

import lombok.Data;

@Data
public class LandingGenerateVo {

    private Long pageId;

    private Boolean needsHumanReview;

    private String captureMethod;

    private Integer moduleCount;
}
