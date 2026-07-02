package org.dromara.project.domain.vo;

import lombok.Data;

@Data
public class LeadAiSuggestionVo {

    private String suggestionEn;

    private String suggestionZh;

    private Boolean needsHumanReview;
}
