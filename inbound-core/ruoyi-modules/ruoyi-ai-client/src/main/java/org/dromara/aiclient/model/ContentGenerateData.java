package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ContentGenerateData {

    private String title;

    private String hook;

    private String script;

    private String voiceover;

    @JsonProperty("on_screen_text")
    private String onScreenText;

    private String cta;

    @JsonProperty("storyboard_json")
    private List<StoryboardScene> storyboardJson;

    @JsonProperty("needs_human_review")
    private Boolean needsHumanReview;

    @JsonProperty("chunk_ids")
    private List<Long> chunkIds;

    @JsonProperty("target_audience")
    private String targetAudience;

    private String hashtags;

    @JsonProperty("landing_page_suggestion")
    private String landingPageSuggestion;

    private String model;

    @JsonProperty("capture_method")
    private String captureMethod;
}
