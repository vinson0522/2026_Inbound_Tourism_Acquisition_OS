package org.dromara.project.domain.vo;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
public class VideoBreakdownVo {

    private Long breakdownId;

    private Long materialId;

    private String sourceUrl;

    private String breakdownStatus;

    private Boolean needsHumanReview;

    private Map<String, Object> dimensions;

    private String reusableStructure;

    private List<Map<String, Object>> frames;

    private OffsetDateTime breakdownCreatedAt;
}
