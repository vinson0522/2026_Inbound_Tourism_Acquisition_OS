package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * POST /ai/rag/search request body (camelCase — matches inbound-ai RagSearchRequest aliases).
 */
@Data
public class RagSearchRequest {

    @JsonProperty("tenantId")
    private Long tenantId;

    @JsonProperty("projectId")
    private Long projectId;

    private String query;

    @JsonProperty("topK")
    private Integer topK;
}
