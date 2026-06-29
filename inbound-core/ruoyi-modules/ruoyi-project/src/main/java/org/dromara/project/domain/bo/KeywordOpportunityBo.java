package org.dromara.project.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.project.domain.KeywordOpportunity;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AutoMapper(target = KeywordOpportunity.class, reverseConvertGenerate = false)
public class KeywordOpportunityBo {

    private Long id;

    @NotBlank(message = "关键词不能为空", groups = AddGroup.class)
    @Size(max = 500, groups = AddGroup.class)
    private String keyword;

    @Size(max = 500, groups = AddGroup.class)
    private String keywordEn;

    @Size(max = 500, groups = AddGroup.class)
    private String keywordCn;

    @Size(max = 64, groups = AddGroup.class)
    private String intent;

    @NotBlank(message = "目标市场不能为空", groups = AddGroup.class)
    @Size(max = 16, groups = AddGroup.class)
    private String market;

    @Size(max = 32, groups = AddGroup.class)
    private String stage;

    private BigDecimal score;

    private Map<String, Object> scoreDetailJson;

    @Size(max = 64, groups = AddGroup.class)
    private String channel;

    private Map<String, Object> sourceJson;

    /** 列表筛选 */
    private String status;
}
