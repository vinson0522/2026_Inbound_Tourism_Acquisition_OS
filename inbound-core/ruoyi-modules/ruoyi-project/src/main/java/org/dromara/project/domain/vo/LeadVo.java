package org.dromara.project.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.project.domain.Lead;

import java.time.OffsetDateTime;

@Data
@AutoMapper(target = Lead.class)
public class LeadVo {

    private Long id;

    private Long projectId;

    private Long landingPageId;

    private Long keywordId;

    private String name;

    private String email;

    private String phone;

    private String source;

    private String status;

    private String landingPageTitle;

    private String landingPageSlug;

    private String keywordText;

    private String keywordMarket;

    private OffsetDateTime createdAt;
}
