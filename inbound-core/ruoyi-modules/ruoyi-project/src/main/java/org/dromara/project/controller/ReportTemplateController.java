package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.ReportTemplateSaveBo;
import org.dromara.project.domain.vo.ReportTemplateVo;
import org.dromara.project.service.IReportTemplateService;
import org.dromara.common.tenant.helper.BusinessTenantHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报告白标模板 — EPIC-8 FR-704
 */
@SaCheckLogin
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/settings/report-template")
public class ReportTemplateController extends BaseController {

    private final IReportTemplateService reportTemplateService;

    @GetMapping
    public R<ReportTemplateVo> getReportTemplate() {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        return R.ok(reportTemplateService.getReportTemplate(tenantId));
    }

    @Log(title = "报告模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<ReportTemplateVo> saveReportTemplate(@RequestBody ReportTemplateSaveBo bo) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        return R.ok(reportTemplateService.saveReportTemplate(tenantId, bo));
    }
}
