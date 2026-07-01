package org.dromara.project.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.project.domain.ContentTask;

import java.time.OffsetDateTime;

@Data
@AutoMapper(target = ContentTask.class)
public class ContentTaskVo {

    private Long id;

    private Long projectId;

    private Long keywordId;

    private String keywordText;

    private String platform;

    private String format;

    private Integer duration;

    private String tone;

    private String language;

    private String targetMarket;

    private String status;

    private String contentTitle;

    private Integer contentVersion;

    private Boolean needsHumanReview;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
