package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SeoMetaJson {

    private String title;

    private String description;

    private String h1;

    @JsonProperty("faq_schema")
    private List<Map<String, Object>> faqSchema;
}
