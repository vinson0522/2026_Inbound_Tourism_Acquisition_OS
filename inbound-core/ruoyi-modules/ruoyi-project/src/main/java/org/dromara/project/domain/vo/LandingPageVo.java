package org.dromara.project.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.project.domain.LandingPage;

import java.time.OffsetDateTime;

@Data
@AutoMapper(target = LandingPage.class)
public class LandingPageVo {

    private Long id;

    private Long projectId;

    private Long keywordId;

    private String keywordText;

    private String templateType;

    private String title;

    private String slug;

    private String status;

    private Integer moduleCount;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
