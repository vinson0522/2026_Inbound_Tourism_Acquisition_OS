package org.dromara.diagnostic.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 保存 GEO 诊断定时计划（FR-109）
 */
@Data
public class DiagnosticScheduleUpsertBo {

    @NotNull(message = "enabled 不能为空")
    private Boolean enabled;

    @NotBlank(message = "执行频率不能为空")
    private String frequency;

    @NotBlank(message = "目标市场不能为空")
    private String market;

    @NotBlank(message = "语言/地区不能为空")
    private String locale;

    private String region;

    @NotNull(message = "探针模式不能为空")
    @Size(min = 1, message = "至少选择一种探针模式")
    private List<String> probeModes;

    @NotNull(message = "AI 平台不能为空")
    @Size(min = 1, message = "至少选择一个 AI 平台")
    private List<String> models;

    @NotNull(message = "采样次数不能为空")
    private Integer sampleCount;

    private BigDecimal calibrationRatio;

    /** JSON scope string or plain stage key */
    private String questionScope;

    private String questionStage;

    private List<Long> questionIds;
}
