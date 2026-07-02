package org.dromara.diagnostic.domain.vo;

import lombok.Data;

import java.util.Map;

@Data
public class PlatformAdapterVo {

    private String platform;

    private String version;

    private Map<String, Object> domSelectors;

    private Map<String, Object> apiPatterns;

    private Map<String, Object> parseRules;
}
