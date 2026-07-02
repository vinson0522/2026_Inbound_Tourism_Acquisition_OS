package org.dromara.project.service;

import org.dromara.project.domain.bo.ReportTemplateSaveBo;
import org.dromara.project.domain.vo.ReportTemplateVo;
import org.dromara.project.report.ReportBranding;

/**
 * 租户报告白标模板 — EPIC-8 FR-704
 */
public interface IReportTemplateService {

    ReportTemplateVo getReportTemplate(Long tenantId);

    ReportTemplateVo saveReportTemplate(Long tenantId, ReportTemplateSaveBo bo);

    ReportBranding resolveBranding(Long tenantId, Long templateId);
}
