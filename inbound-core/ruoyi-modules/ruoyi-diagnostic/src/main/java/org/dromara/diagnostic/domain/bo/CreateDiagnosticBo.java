package org.dromara.diagnostic.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建 GEO 诊断任务（FR-103）
 */
@Data
public class CreateDiagnosticBo {

    @NotBlank(message = "诊断名称不能为空")
    @Size(max = 200, message = "诊断名称长度不能超过200")
    private String name;

    @NotBlank(message = "目标市场不能为空")
    private String market;

    private String locale;

    private String region;

    private List<String> probeModes;

    private List<String> models;

    private Integer sampleCount;

    private BigDecimal calibrationRatio;

    /** JSON scope string or plain stage key */
    private String questionScope;

    private String questionStage;

    private List<Long> questionIds;
}
