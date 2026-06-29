package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class KeywordGenerateData {

    @JsonProperty("needs_human_review")
    private Boolean needsHumanReview;

    private List<StageKeywords> stages;

    private String model;

    @JsonProperty("capture_method")
    private String captureMethod;
}
