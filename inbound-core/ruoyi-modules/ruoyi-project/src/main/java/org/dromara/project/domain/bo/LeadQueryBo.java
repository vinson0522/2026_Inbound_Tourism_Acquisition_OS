package org.dromara.project.domain.bo;

import lombok.Data;

@Data
public class LeadQueryBo {

    private String name;

    private String email;

    private String phone;

    private String source;

    private String status;
}
