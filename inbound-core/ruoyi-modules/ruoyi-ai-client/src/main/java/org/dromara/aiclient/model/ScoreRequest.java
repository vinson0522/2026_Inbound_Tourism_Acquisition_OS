package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * POST /ai/score request body.
 */
@Data
public class ScoreRequest {

    @JsonProperty("run_id")
    private Long runId;

    @JsonProperty("metric_weights_json")
    private Map<String, Double> metricWeightsJson;

    private List<ResultMetricSummary> results;

    @JsonProperty("longtail_question_ids")
    private List<Long> longtailQuestionIds;
}
