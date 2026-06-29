package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class KeywordGenerateRequest {

    @JsonProperty("tenantId")
    private Long tenantId;

    @JsonProperty("projectId")
    private Long projectId;

    private String market;

    private String locale;

    private List<String> stages;

    @JsonProperty("wordsPerStage")
    private Integer wordsPerStage;

    @JsonProperty("useRag")
    private Boolean useRag;

    @JsonProperty("traceId")
    private String traceId;
}
