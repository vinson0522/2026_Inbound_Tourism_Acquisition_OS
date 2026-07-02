package org.dromara.project.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * 保存租户报告白标模板 — EPIC-8 FR-704
 */
@Data
public class ReportTemplateSaveBo {

    private String logoUrl;

    private String coverTitle;

    private String companyName;

    private String primaryColor;

    private String footerText;

    private List<String> sections;
}
