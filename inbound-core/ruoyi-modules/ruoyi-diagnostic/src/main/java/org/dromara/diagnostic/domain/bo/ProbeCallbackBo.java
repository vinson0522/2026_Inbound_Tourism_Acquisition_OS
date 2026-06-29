package org.dromara.diagnostic.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * AI worker 探针回调（内网）
 */
@Data
public class ProbeCallbackBo {

    private String traceId;

    @NotNull(message = "probeTaskId 不能为空")
    private Long probeTaskId;

    @NotBlank(message = "status 不能为空")
    private String status;

    private Map<String, Object> result;

    private String errorMessage;
}
