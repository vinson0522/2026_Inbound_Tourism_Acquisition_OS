package org.dromara.project.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class LandingPageDetailVo extends LandingPageVo {

    private Map<String, Object> contentJson;

    private Map<String, Object> seoMetaJson;

    private Map<String, Object> formConfigJson;

    private String whatsappLink;
}
