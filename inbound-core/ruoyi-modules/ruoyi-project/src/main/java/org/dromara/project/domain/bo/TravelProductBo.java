package org.dromara.project.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.project.domain.TravelProduct;

import java.util.List;

@Data
@AutoMapper(target = TravelProduct.class, reverseConvertGenerate = false)
public class TravelProductBo {

    private Long id;

    private Long projectId;

    @NotBlank(message = "路线名称不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 300, groups = {AddGroup.class, EditGroup.class})
    private String name;

    @NotEmpty(message = "请至少选择一个目的地", groups = {AddGroup.class, EditGroup.class})
    private List<String> destinations;

    private Integer days;

    @Size(max = 100, groups = {AddGroup.class, EditGroup.class})
    private String priceRange;

    private String suitableFor;

    private String highlights;

    private String inclusions;
}
