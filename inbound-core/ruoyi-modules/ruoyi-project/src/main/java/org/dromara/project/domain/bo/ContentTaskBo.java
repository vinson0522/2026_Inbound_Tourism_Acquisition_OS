package org.dromara.project.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.project.domain.ContentTask;

@Data
@AutoMapper(target = ContentTask.class, reverseConvertGenerate = false)
public class ContentTaskBo {

    private Long id;

    @NotNull(message = "关键词不能为空", groups = AddGroup.class)
    private Long keywordId;

    @NotBlank(message = "平台不能为空", groups = AddGroup.class)
    @Size(max = 64, groups = AddGroup.class)
    private String platform;

    @Size(max = 64, groups = AddGroup.class)
    private String format;

    private Integer duration;

    @Size(max = 64, groups = AddGroup.class)
    private String tone;

    @Size(max = 16, groups = AddGroup.class)
    private String language;

    @Size(max = 16, groups = AddGroup.class)
    private String targetMarket;

    /** 列表筛选 */
    private String status;
}
