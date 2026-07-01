package org.dromara.project.domain.vo;

import lombok.Data;

@Data
public class ContentGenerateVo {

    private Long contentId;

    private Integer version;

    private Boolean needsHumanReview;

    private String captureMethod;
}
