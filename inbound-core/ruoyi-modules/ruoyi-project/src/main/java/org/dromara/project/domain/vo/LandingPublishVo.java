package org.dromara.project.domain.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class LandingPublishVo {

    private Long pageId;

    private String status;

    private String publishedUrl;

    private OffsetDateTime publishedAt;
}
