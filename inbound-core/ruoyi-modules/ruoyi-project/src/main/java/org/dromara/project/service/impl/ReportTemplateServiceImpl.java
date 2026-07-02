package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.project.domain.Template;
import org.dromara.project.domain.bo.ReportTemplateSaveBo;
import org.dromara.project.domain.vo.ReportTemplateVo;
import org.dromara.project.mapper.TemplateMapper;
import org.dromara.project.report.ReportBranding;
import org.dromara.project.service.IReportTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportTemplateServiceImpl implements IReportTemplateService {

    private static final String TYPE_REPORT = "REPORT";
    private static final String DEFAULT_NAME = "Default Report Template";

    private final TemplateMapper templateMapper;

    @Override
    public ReportTemplateVo getReportTemplate(Long tenantId) {
        Template row = findReportTemplate(tenantId);
        if (row == null) {
            return toVo(null, ReportBranding.defaults());
        }
        return toVo(row.getId(), ReportBranding.fromConfig(row.getConfigJson()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportTemplateVo saveReportTemplate(Long tenantId, ReportTemplateSaveBo bo) {
        if (bo == null) {
            throw new ServiceException("请求体不能为空", HttpStatus.BAD_REQUEST);
        }
        List<String> sections = bo.getSections();
        if (sections == null || sections.isEmpty()) {
            throw new ServiceException("sections 至少选择 1 项", HttpStatus.BAD_REQUEST);
        }

        ReportBranding branding = ReportBranding.builder()
            .logoUrl(StringUtils.blankToDefault(bo.getLogoUrl(), ""))
            .coverTitle(StringUtils.blankToDefault(bo.getCoverTitle(), ReportBranding.defaults().getCoverTitle()))
            .companyName(StringUtils.blankToDefault(bo.getCompanyName(), ReportBranding.defaults().getCompanyName()))
            .primaryColor(StringUtils.blankToDefault(bo.getPrimaryColor(), ReportBranding.defaults().getPrimaryColor()))
            .footerText(StringUtils.blankToDefault(bo.getFooterText(), ReportBranding.defaults().getFooterText()))
            .sections(sections)
            .build();
        Map<String, Object> configJson = branding.toConfigMap();

        Template existing = findReportTemplate(tenantId);
        OffsetDateTime now = OffsetDateTime.now();
        if (existing == null) {
            Template created = new Template();
            created.setTenantId(tenantId);
            created.setType(TYPE_REPORT);
            created.setName(DEFAULT_NAME);
            created.setConfigJson(configJson);
            created.setIsDefault(true);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            created.setCreatedBy(LoginHelper.getUserId());
            templateMapper.insert(created);
            return toVo(created.getId(), branding);
        }

        existing.setConfigJson(configJson);
        existing.setUpdatedAt(now);
        templateMapper.updateById(existing);
        return toVo(existing.getId(), branding);
    }

    @Override
    public ReportBranding resolveBranding(Long tenantId, Long templateId) {
        if (templateId != null) {
            Template row = templateMapper.selectOne(
                Wrappers.lambdaQuery(Template.class)
                    .eq(Template::getId, templateId)
                    .eq(Template::getTenantId, tenantId)
                    .apply("type = 'REPORT'::template_type")
                    .isNull(Template::getDeletedAt)
            );
            if (row != null) {
                return ReportBranding.fromConfig(row.getConfigJson());
            }
        }
        Template row = findReportTemplate(tenantId);
        if (row != null) {
            return ReportBranding.fromConfig(row.getConfigJson());
        }
        return ReportBranding.defaults();
    }

    public Long resolveTemplateId(Long tenantId) {
        Template row = findReportTemplate(tenantId);
        return row != null ? row.getId() : null;
    }

    private Template findReportTemplate(Long tenantId) {
        return templateMapper.selectOne(
            Wrappers.lambdaQuery(Template.class)
                .eq(Template::getTenantId, tenantId)
                .apply("type = 'REPORT'::template_type")
                .isNull(Template::getDeletedAt)
                .orderByDesc(Template::getIsDefault)
                .orderByDesc(Template::getUpdatedAt)
                .last("LIMIT 1")
        );
    }

    private static ReportTemplateVo toVo(Long templateId, ReportBranding branding) {
        ReportTemplateVo vo = new ReportTemplateVo();
        vo.setTemplateId(templateId);
        vo.setLogoUrl(branding.getLogoUrl());
        vo.setCoverTitle(branding.getCoverTitle());
        vo.setCompanyName(branding.getCompanyName());
        vo.setPrimaryColor(branding.getPrimaryColor());
        vo.setFooterText(branding.getFooterText());
        vo.setSections(branding.getSections());
        vo.setConfigJson(branding.toConfigMap());
        return vo;
    }
}
