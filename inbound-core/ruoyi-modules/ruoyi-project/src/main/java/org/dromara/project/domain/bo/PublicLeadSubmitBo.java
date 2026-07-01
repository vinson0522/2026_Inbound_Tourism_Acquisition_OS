package org.dromara.project.domain.bo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class PublicLeadSubmitBo {

    @NotNull(message = "落地页不能为空")
    private Long landingPageId;

    @Size(max = 200)
    private String name;

    @Size(max = 255)
    private String email;

    @Size(max = 64)
    private String phone;

    private LocalDate travelDate;

    private Integer partySize;

    @Size(max = 100)
    private String budget;

    private String message;

    @Size(max = 64)
    private String source;

    private Map<String, Object> utm;

    @Size(max = 200)
    private String device;
}
