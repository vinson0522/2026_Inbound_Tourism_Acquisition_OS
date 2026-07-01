package org.dromara.project.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.project.domain.LandingPage;

@Data
@AutoMapper(target = LandingPage.class, reverseConvertGenerate = false)
public class LandingPageBo {

    private Long id;

    @NotNull(message = "关键词不能为空", groups = AddGroup.class)
    private Long keywordId;

    @Size(max = 64, groups = AddGroup.class)
    private String templateType;

    @Size(max = 500, groups = AddGroup.class)
    private String title;

    @Size(max = 200, groups = AddGroup.class)
    private String slug;

    @Size(max = 16, groups = AddGroup.class)
    private String language;

    @Size(max = 16, groups = AddGroup.class)
    private String targetMarket;

    /** 列表筛选 */
    private String status;
}
