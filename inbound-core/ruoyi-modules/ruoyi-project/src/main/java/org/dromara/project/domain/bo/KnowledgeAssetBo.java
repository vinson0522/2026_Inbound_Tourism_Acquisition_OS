package org.dromara.project.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;

import java.util.List;

@Data
public class KnowledgeAssetBo {

    private Long id;

    @NotNull(message = "projectId不能为空", groups = {AddGroup.class})
    private Long projectId;

    private String type;

    @NotBlank(message = "title不能为空", groups = {AddGroup.class})
    private String title;

    private String content;

    private String fileUrl;

    private List<String> tags;
}
