package org.dromara.project.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.project.domain.KeywordOpportunity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@AutoMapper(target = KeywordOpportunity.class)
public class KeywordOpportunityVo {

    private Long id;

    private Long projectId;

    private String keyword;

    private String keywordEn;

    private String keywordCn;

    private String intent;

    private String market;

    private String stage;

    private BigDecimal score;

    private Map<String, Object> scoreDetailJson;

    private String channel;

    private Map<String, Object> sourceJson;

    private String status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
