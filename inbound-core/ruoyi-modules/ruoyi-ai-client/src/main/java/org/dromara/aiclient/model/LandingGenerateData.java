package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class LandingGenerateData {

    private String title;

    @JsonProperty("content_json")
    private LandingContentJson contentJson;

    @JsonProperty("seo_meta_json")
    private SeoMetaJson seoMetaJson;

    @JsonProperty("form_config_json")
    private FormConfigJson formConfigJson;

    @JsonProperty("needs_human_review")
    private Boolean needsHumanReview;

    @JsonProperty("chunk_ids")
    private List<Long> chunkIds;

    private String model;

    @JsonProperty("capture_method")
    private String captureMethod;
}
