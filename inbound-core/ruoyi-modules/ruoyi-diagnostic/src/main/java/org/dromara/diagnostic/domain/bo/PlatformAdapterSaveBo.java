package org.dromara.diagnostic.domain.bo;

import lombok.Data;

import java.util.Map;

/**
 * 保存平台 Adapter — EPIC-11 FR-116
 */
@Data
public class PlatformAdapterSaveBo {

    private String version;

    private Boolean enabled;

    private Map<String, Object> domSelectorsJson;

    private Map<String, Object> apiPatternsJson;

    private Map<String, Object> parseRulesJson;
}
