package org.dromara.project.domain.bo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** FR-201 AI 生成机会词请求（Java → inbound-ai）。 */
@Data
public class KeywordGenerateBo {

    @NotBlank(message = "目标市场不能为空")
    @Size(max = 16)
    private String market;

    @Size(max = 16)
    private String locale = "en";

    private List<String> stages;

    @Min(1)
    @Max(20)
    private Integer wordsPerStage = 5;

    private Boolean useRag = true;
}
