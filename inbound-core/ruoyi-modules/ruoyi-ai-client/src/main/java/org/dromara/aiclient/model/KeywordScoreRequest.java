package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class KeywordScoreRequest {

    @JsonProperty("tenantId")
    private Long tenantId;

    @JsonProperty("projectId")
    private Long projectId;

    @JsonProperty("keywordId")
    private Long keywordId;

    private String keyword;

    @JsonProperty("keywordEn")
    private String keywordEn;

    private String stage;

    private String market;

    @JsonProperty("brandName")
    private String brandName;

    private List<String> competitors;

    @JsonProperty("geoScore")
    private Double geoScore;

    @JsonProperty("useRag")
    private Boolean useRag;

    @JsonProperty("traceId")
    private String traceId;
}
