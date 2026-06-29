package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Per-question metric summary for GEO score aggregation.
 */
@Data
public class ResultMetricSummary {

    @JsonProperty("question_id")
    private Long questionId;

    @JsonProperty("brand_mentioned")
    private Boolean brandMentioned;

    @JsonProperty("in_top3")
    private Boolean inTop3;

    @JsonProperty("competitor_dominance")
    private Double competitorDominance;

    @JsonProperty("citation_hit")
    private Boolean citationHit;

    @JsonProperty("is_longtail")
    private Boolean isLongtail;

    @JsonProperty("asset_complete")
    private Double assetComplete;
}
