package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GeneratedKeyword {

    private String text;

    private String rationale;

    @JsonProperty("chunk_ids")
    private List<Long> chunkIds;

    @JsonProperty("suggested_score")
    private Double suggestedScore;

    @JsonProperty("needs_human_review")
    private Boolean needsHumanReview;
}
