package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ContentGenerateRequest {

    @JsonProperty("tenantId")
    private Long tenantId;

    @JsonProperty("projectId")
    private Long projectId;

    @JsonProperty("keywordId")
    private Long keywordId;

    @JsonProperty("keywordText")
    private String keywordText;

    private String platform;

    @JsonProperty("durationSec")
    private Integer durationSec;

    private String tone;

    private String language;

    @JsonProperty("targetMarket")
    private String targetMarket;

    @JsonProperty("useRag")
    private Boolean useRag;

    @JsonProperty("traceId")
    private String traceId;
}
