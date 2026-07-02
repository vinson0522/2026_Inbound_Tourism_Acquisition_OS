package org.dromara.diagnostic.domain.vo;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Admin 平台 Adapter 配置 — EPIC-11 FR-116
 */
@Data
public class PlatformAdapterAdminVo {

    private Long id;

    private String platform;

    private String version;

    private Boolean enabled;

    private Map<String, Object> domSelectorsJson;

    private Map<String, Object> apiPatternsJson;

    private Map<String, Object> parseRulesJson;

    private OffsetDateTime updatedAt;
}
