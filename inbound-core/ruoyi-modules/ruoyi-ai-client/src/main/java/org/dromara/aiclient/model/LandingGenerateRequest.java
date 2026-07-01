package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LandingGenerateRequest {

    @JsonProperty("tenantId")
    private Long tenantId;

    @JsonProperty("projectId")
    private Long projectId;

    @JsonProperty("keywordId")
    private Long keywordId;

    @JsonProperty("keywordText")
    private String keywordText;

    @JsonProperty("templateType")
    private String templateType;

    private String language;

    @JsonProperty("targetMarket")
    private String targetMarket;

    @JsonProperty("useRag")
    private Boolean useRag;

    @JsonProperty("traceId")
    private String traceId;
}
