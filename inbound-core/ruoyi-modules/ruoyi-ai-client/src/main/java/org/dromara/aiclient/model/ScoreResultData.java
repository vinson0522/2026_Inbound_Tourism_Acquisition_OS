package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * POST /ai/score response data.
 */
@Data
public class ScoreResultData {

    @JsonProperty("geo_score")
    private Double geoScore;

    private Map<String, Double> metrics;

    @JsonProperty("metric_weights")
    private Map<String, Double> metricWeights;

    @JsonProperty("uncovered_questions")
    private List<Long> uncoveredQuestions;
}
