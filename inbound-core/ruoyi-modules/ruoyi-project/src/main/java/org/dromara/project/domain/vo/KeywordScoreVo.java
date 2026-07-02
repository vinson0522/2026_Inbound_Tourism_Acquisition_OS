package org.dromara.project.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class KeywordScoreVo {

    private Long keywordId;

    private String keyword;

    private BigDecimal score;

    private Map<String, Object> scoreDetailJson;
}
