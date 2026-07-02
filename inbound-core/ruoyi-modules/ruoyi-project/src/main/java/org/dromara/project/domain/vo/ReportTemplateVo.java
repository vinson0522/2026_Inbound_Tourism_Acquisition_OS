package org.dromara.project.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 租户报告白标模板 — EPIC-8 FR-704
 */
@Data
public class ReportTemplateVo {

    private Long templateId;

    private String logoUrl;

    private String coverTitle;

    private String companyName;

    private String primaryColor;

    private String footerText;

    private List<String> sections;

    /** Full config_json mirror for Admin form round-trip */
    private Map<String, Object> configJson;
}
