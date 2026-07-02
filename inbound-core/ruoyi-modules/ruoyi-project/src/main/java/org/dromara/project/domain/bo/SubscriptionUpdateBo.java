package org.dromara.project.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 更新租户套餐 — EPIC-9 M2 FR-804
 */
@Data
public class SubscriptionUpdateBo {

    @NotBlank(message = "planCode 不能为空")
    private String planCode;

    @NotNull(message = "quotaJson 不能为空")
    private Map<String, Object> quotaJson;

    /** YYYY-MM-DD；省略则保留原值 */
    private String periodStart;

    /** YYYY-MM-DD；省略则保留原值 */
    private String periodEnd;
}
