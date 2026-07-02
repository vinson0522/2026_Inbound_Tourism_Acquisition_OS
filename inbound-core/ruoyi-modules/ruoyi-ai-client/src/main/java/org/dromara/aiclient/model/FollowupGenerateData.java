package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FollowupGenerateData {

    @JsonProperty("suggestionEn")
    private String suggestionEn;

    @JsonProperty("suggestionZh")
    private String suggestionZh;

    @JsonProperty("needs_human_review")
    private Boolean needsHumanReview;

    private String model;

    @JsonProperty("capture_method")
    private String captureMethod;
}
