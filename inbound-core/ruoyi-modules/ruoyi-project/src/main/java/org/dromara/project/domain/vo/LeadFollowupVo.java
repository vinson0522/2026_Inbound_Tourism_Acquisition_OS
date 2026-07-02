package org.dromara.project.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.project.domain.LeadFollowup;

import java.time.OffsetDateTime;

@Data
@AutoMapper(target = LeadFollowup.class)
public class LeadFollowupVo {

    private Long id;

    private Long leadId;

    private String content;

    private String channel;

    private String suggestion;

    private Long operatorId;

    private String operatorName;

    private OffsetDateTime createdAt;
}
