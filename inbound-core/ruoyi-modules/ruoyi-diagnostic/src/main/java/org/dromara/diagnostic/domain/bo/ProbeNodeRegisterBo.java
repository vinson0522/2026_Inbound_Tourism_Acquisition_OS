package org.dromara.diagnostic.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ProbeNodeRegisterBo {

    @NotBlank(message = "nodeKey 不能为空")
    private String nodeKey;

    @NotBlank(message = "region 不能为空")
    private String region;

    private List<String> platforms;

    private String extensionVersion;
}
