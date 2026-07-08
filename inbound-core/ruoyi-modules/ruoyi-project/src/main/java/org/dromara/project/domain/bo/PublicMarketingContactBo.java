package org.dromara.project.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 营销门户联系表单 — 咨询 TourGEO 产品本身（非客户落地页询盘）
 */
@Data
public class PublicMarketingContactBo {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 200)
    private String name;

    @Size(max = 255)
    private String email;

    @Size(max = 64)
    private String phone;

    @Size(max = 200)
    private String company;

    private String message;

    @Size(max = 64)
    private String source;

    private Map<String, Object> utm;

    @Size(max = 200)
    private String device;
}
