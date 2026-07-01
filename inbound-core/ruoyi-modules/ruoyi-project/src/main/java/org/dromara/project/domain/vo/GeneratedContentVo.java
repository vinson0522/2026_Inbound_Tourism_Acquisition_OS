package org.dromara.project.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.project.domain.GeneratedContent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@AutoMapper(target = GeneratedContent.class)
public class GeneratedContentVo {

    private Long id;

    private Long taskId;

    private String title;

    private String hook;

    private String targetAudience;

    private String script;

    private List<Map<String, Object>> storyboardJson;

    private String voiceover;

    private String onScreenText;

    private String hashtags;

    private String cta;

    private String landingPageSuggestion;

    private Boolean needsHumanReview;

    private Integer version;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
