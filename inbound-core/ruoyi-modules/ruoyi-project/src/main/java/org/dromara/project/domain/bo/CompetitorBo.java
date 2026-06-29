package org.dromara.project.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.project.domain.Competitor;

import java.util.Map;

@Data
@AutoMapper(target = Competitor.class, reverseConvertGenerate = false)
public class CompetitorBo {

    private Long id;

    private Long projectId;

    @NotBlank(message = "竞品名称不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 200, groups = {AddGroup.class, EditGroup.class})
    private String name;

    @Size(max = 500, groups = {AddGroup.class, EditGroup.class})
    private String website;

    private Map<String, Object> socialLinks;

    private String mainProducts;

    private String notes;
}
