package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * inbound-ai 统一响应 {@code { code, message, data, trace_id }}。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiApiResponse<T> {

    private Integer code;

    private String message;

    private T data;

    @JsonProperty("trace_id")
    private String traceId;
}
