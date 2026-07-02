package org.dromara.project.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class PublicLeadEventBo {

    @NotBlank(message = "eventType 不能为空")
    @Size(max = 64)
    private String eventType;

    @NotNull(message = "projectId 不能为空")
    private Long projectId;

    private Long landingPageId;

    private Map<String, Object> utm;

    @Size(max = 200)
    private String device;
}
