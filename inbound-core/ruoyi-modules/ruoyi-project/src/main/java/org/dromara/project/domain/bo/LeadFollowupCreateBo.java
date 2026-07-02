package org.dromara.project.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeadFollowupCreateBo {

    @NotBlank(message = "跟进内容不能为空")
    private String content;

    private String channel;
}
