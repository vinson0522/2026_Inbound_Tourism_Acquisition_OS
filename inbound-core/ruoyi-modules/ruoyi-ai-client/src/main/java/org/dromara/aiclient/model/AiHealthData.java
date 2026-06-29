package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiHealthData {

    private String status;

    private String litellm;

    private String db;
}
