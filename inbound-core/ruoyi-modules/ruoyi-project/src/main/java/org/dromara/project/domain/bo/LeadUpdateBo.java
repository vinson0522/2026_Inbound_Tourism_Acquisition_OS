package org.dromara.project.domain.bo;

import lombok.Data;

@Data
public class LeadUpdateBo {

    private String status;

    private Long assigneeId;
}
