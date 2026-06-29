package org.dromara.project.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.project.domain.CustomerProject;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = CustomerProject.class, reverseConvertGenerate = false)
public class CustomerProjectBo extends BaseEntity {

    private Long id;

    @NotBlank(message = "项目名称不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 200, message = "项目名称长度不能超过200", groups = {AddGroup.class, EditGroup.class})
    private String name;

    @NotBlank(message = "品牌名不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 200, message = "品牌名长度不能超过200", groups = {AddGroup.class, EditGroup.class})
    private String brandName;

    @Size(max = 500, message = "官网长度不能超过500", groups = {AddGroup.class, EditGroup.class})
    private String website;

    private String industry;

    @NotEmpty(message = "请至少选择一个目标市场", groups = {AddGroup.class, EditGroup.class})
    private List<String> targetMarkets;

    @NotEmpty(message = "请至少选择一种服务语言", groups = {AddGroup.class, EditGroup.class})
    private List<String> languages;

    private String status;

    /** 列表筛选：目标市场（JSONB 包含） */
    private String market;
}
