package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FollowupGenerateRequest {

    @JsonProperty("tenantId")
    private Long tenantId;

    @JsonProperty("projectId")
    private Long projectId;

    @JsonProperty("leadId")
    private Long leadId;

    private String name;

    private String message;

    private String budget;

    @JsonProperty("travelDate")
    private LocalDate travelDate;

    private String source;

    @JsonProperty("keywordText")
    private String keywordText;

    @JsonProperty("traceId")
    private String traceId;
}
