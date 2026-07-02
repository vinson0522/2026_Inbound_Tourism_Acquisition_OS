package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class KeywordScoreData {

    private Double score;

    @JsonProperty("score_detail")
    private Map<String, Object> scoreDetail;

    @JsonProperty("needs_human_review")
    private Boolean needsHumanReview;

    private String model;

    @JsonProperty("capture_method")
    private String captureMethod;
}
