package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RagChunkHit {

    @JsonProperty("chunk_id")
    private Long chunkId;

    @JsonProperty("asset_id")
    private Long assetId;

    @JsonProperty("chunk_index")
    private Integer chunkIndex;

    @JsonProperty("chunk_text")
    private String chunkText;

    private Double score;
}
