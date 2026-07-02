package org.dromara.diagnostic.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class ProbeExtensionResultBo {

    @NotBlank(message = "status 不能为空")
    private String status;

    private Map<String, Object> result;

    private String errorMessage;
}
