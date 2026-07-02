package org.dromara.project.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Python AI worker 拆解回调（内网）
 */
@Data
public class MaterialBreakdownCallbackBo {

    private String traceId;

    @NotNull(message = "breakdownId 不能为空")
    private Long breakdownId;

    @NotBlank(message = "status 不能为空")
    private String status;

    private List<Map<String, Object>> frames;

    private Map<String, Object> dimensions;

    private String reusableStructure;

    private Boolean needsHumanReview;

    private String errorMessage;
}
