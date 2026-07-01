package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.aiclient.client.AiServiceClient;
import org.dromara.aiclient.model.AiApiResponse;
import org.dromara.aiclient.model.FormConfigJson;
import org.dromara.aiclient.model.LandingContentJson;
import org.dromara.aiclient.model.LandingGenerateData;
import org.dromara.aiclient.model.LandingGenerateRequest;
import org.dromara.aiclient.model.LandingModule;
import org.dromara.aiclient.model.SeoMetaJson;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.domain.KeywordOpportunity;
import org.dromara.project.domain.LandingPage;
import org.dromara.project.domain.bo.LandingGenerateBo;
import org.dromara.project.domain.bo.LandingPageBo;
import org.dromara.project.domain.vo.LandingGenerateVo;
import org.dromara.project.domain.vo.LandingPageDetailVo;
import org.dromara.project.domain.vo.LandingPageVo;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.dromara.project.mapper.KeywordOpportunityMapper;
import org.dromara.project.mapper.LandingPageMapper;
import org.dromara.project.service.ILandingPageService;
import org.dromara.project.support.BusinessTenantHelper;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LandingPageServiceImpl implements ILandingPageService {

    private final LandingPageMapper landingPageMapper;
    private final CustomerProjectMapper customerProjectMapper;
    private final KeywordOpportunityMapper keywordOpportunityMapper;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    public TableDataInfo<LandingPageVo> queryPageList(Long projectId, LandingPageBo bo, PageQuery pageQuery) {
        assertProjectOwned(projectId);
        LambdaQueryWrapper<LandingPage> lqw = Wrappers.lambdaQuery(LandingPage.class)
            .eq(LandingPage::getProjectId, projectId)
            .eq(LandingPage::getTenantId, BusinessTenantHelper.getBusinessTenantId())
            .isNull(LandingPage::getDeletedAt)
            .eq(StringUtils.isNotBlank(bo.getStatus()), LandingPage::getStatus, bo.getStatus())
            .eq(StringUtils.isNotBlank(bo.getTemplateType()), LandingPage::getTemplateType, bo.getTemplateType())
            .orderByDesc(LandingPage::getCreatedAt);
        Page<LandingPageVo> page = landingPageMapper.selectVoPage(pageQuery.build(), lqw);
        enrichListRows(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public LandingPageDetailVo queryById(Long projectId, Long pageId) {
        LandingPage page = getOwnedPageOrThrow(projectId, pageId);
        LandingPageDetailVo detail = new LandingPageDetailVo();
        LandingPageVo base = MapstructUtils.convert(page, LandingPageVo.class);
        org.springframework.beans.BeanUtils.copyProperties(base, detail);
        detail.setContentJson(page.getContentJson());
        detail.setSeoMetaJson(page.getSeoMetaJson());
        detail.setFormConfigJson(page.getFormConfigJson());
        detail.setWhatsappLink(page.getWhatsappLink());
        detail.setModuleCount(countModules(page.getContentJson()));
        if (page.getKeywordId() != null) {
            KeywordOpportunity keyword = getOwnedKeywordOrNull(projectId, page.getKeywordId());
            if (keyword != null) {
                detail.setKeywordText(keyword.getKeyword());
            }
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(Long projectId, LandingPageBo bo) {
        getOwnedProjectOrThrow(projectId);
        KeywordOpportunity keyword = getOwnedKeywordOrThrow(projectId, bo.getKeywordId());

        String keywordLabel = StringUtils.blankToDefault(keyword.getKeywordEn(), keyword.getKeyword());
        String title = StringUtils.blankToDefault(bo.getTitle(), keywordLabel + " Landing Page");
        String slug = resolveUniqueSlug(projectId, StringUtils.blankToDefault(bo.getSlug(), keywordLabel));

        OffsetDateTime now = OffsetDateTime.now();
        LandingPage entity = new LandingPage();
        entity.setProjectId(projectId);
        entity.setTenantId(BusinessTenantHelper.getBusinessTenantId());
        entity.setKeywordId(bo.getKeywordId());
        entity.setTemplateType(StringUtils.blankToDefault(bo.getTemplateType(), "destination"));
        entity.setTitle(title);
        entity.setSlug(slug);
        entity.setContentJson(new HashMap<>());
        entity.setSeoMetaJson(new HashMap<>());
        entity.setFormConfigJson(new HashMap<>());
        entity.setStatus("DRAFT");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(LoginHelper.getUserId());
        landingPageMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean deleteById(Long projectId, Long pageId) {
        getOwnedPageOrThrow(projectId, pageId);
        OffsetDateTime now = OffsetDateTime.now();
        return landingPageMapper.update(
            null,
            Wrappers.lambdaUpdate(LandingPage.class)
                .set(LandingPage::getDeletedAt, now)
                .set(LandingPage::getUpdatedAt, now)
                .eq(LandingPage::getId, pageId)
                .eq(LandingPage::getProjectId, projectId)
                .eq(LandingPage::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(LandingPage::getDeletedAt)
        ) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LandingGenerateVo generate(Long projectId, Long pageId, LandingGenerateBo bo) {
        LandingPage page = getOwnedPageOrThrow(projectId, pageId);
        if (page.getKeywordId() == null) {
            throw new ServiceException("落地页未关联关键词，无法生成文案");
        }
        KeywordOpportunity keyword = getOwnedKeywordOrThrow(projectId, page.getKeywordId());
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();

        updatePageStatus(pageId, "EDITING");

        LandingGenerateRequest aiReq = new LandingGenerateRequest();
        aiReq.setTenantId(tenantId);
        aiReq.setProjectId(projectId);
        aiReq.setKeywordId(page.getKeywordId());
        aiReq.setKeywordText(StringUtils.blankToDefault(keyword.getKeywordEn(), keyword.getKeyword()));
        aiReq.setTemplateType(StringUtils.blankToDefault(page.getTemplateType(), "destination"));
        aiReq.setLanguage("en");
        aiReq.setTargetMarket(defaultMarket(keyword));
        aiReq.setUseRag(bo != null && bo.getUseRag() != null ? bo.getUseRag() : Boolean.TRUE);
        aiReq.setTraceId(StringUtils.blankToDefault(MDC.get("traceId"), UUID.randomUUID().toString()));

        AiApiResponse<LandingGenerateData> aiResp;
        try {
            aiResp = aiServiceClient.landingGenerate(aiReq);
        } catch (IllegalStateException ex) {
            updatePageStatus(pageId, "DRAFT");
            throw new ServiceException("AI 落地页生成失败: " + ex.getMessage());
        }
        if (aiResp == null || aiResp.getCode() == null || aiResp.getCode() != 0 || aiResp.getData() == null) {
            updatePageStatus(pageId, "DRAFT");
            String msg = aiResp != null ? aiResp.getMessage() : "empty response";
            throw new ServiceException("AI 落地页生成失败: " + msg);
        }

        LandingGenerateData data = aiResp.getData();
        persistGeneratedContent(pageId, data);

        LandingGenerateVo vo = new LandingGenerateVo();
        vo.setPageId(pageId);
        vo.setNeedsHumanReview(
            Boolean.TRUE.equals(data.getNeedsHumanReview()) || data.getNeedsHumanReview() == null
        );
        vo.setCaptureMethod(data.getCaptureMethod());
        vo.setModuleCount(countModulesFromAi(data.getContentJson()));
        return vo;
    }

    private void persistGeneratedContent(Long pageId, LandingGenerateData data) {
        if (StringUtils.isBlank(data.getTitle()) || data.getContentJson() == null) {
            updatePageStatus(pageId, "DRAFT");
            throw new ServiceException("AI 返回落地页内容不完整");
        }
        List<LandingModule> modules = data.getContentJson().getModules();
        if (modules == null || modules.isEmpty()) {
            updatePageStatus(pageId, "DRAFT");
            throw new ServiceException("AI 返回落地页模块为空");
        }

        Map<String, Object> contentJson = toContentJsonMap(data.getContentJson());
        Map<String, Object> seoMetaJson = toSeoMetaMap(data.getSeoMetaJson());
        Map<String, Object> formConfigJson = toFormConfigMap(data.getFormConfigJson());
        String whatsappLink = data.getFormConfigJson() != null ? data.getFormConfigJson().getWhatsappLink() : null;

        LandingPage patch = new LandingPage();
        patch.setId(pageId);
        patch.setTitle(data.getTitle());
        patch.setContentJson(contentJson);
        patch.setSeoMetaJson(seoMetaJson);
        patch.setFormConfigJson(formConfigJson);
        patch.setWhatsappLink(whatsappLink);
        patch.setStatus("DRAFT");
        patch.setUpdatedAt(OffsetDateTime.now());
        landingPageMapper.updateById(patch);
    }

    private Map<String, Object> toContentJsonMap(LandingContentJson contentJson) {
        Map<String, Object> root = new HashMap<>();
        List<Map<String, Object>> modules = contentJson.getModules().stream()
            .map(module -> {
                Map<String, Object> row = new HashMap<>();
                row.put("key", module.getKey());
                row.put("content", module.getContent() != null ? module.getContent() : Map.of());
                return row;
            })
            .collect(Collectors.toList());
        root.put("modules", modules);
        return root;
    }

    private Map<String, Object> toSeoMetaMap(SeoMetaJson seo) {
        if (seo == null) {
            return new HashMap<>();
        }
        return objectMapper.convertValue(seo, Map.class);
    }

    private Map<String, Object> toFormConfigMap(FormConfigJson form) {
        if (form == null) {
            return new HashMap<>();
        }
        return objectMapper.convertValue(form, Map.class);
    }

    private int countModulesFromAi(LandingContentJson contentJson) {
        if (contentJson == null || contentJson.getModules() == null) {
            return 0;
        }
        return contentJson.getModules().size();
    }

    @SuppressWarnings("unchecked")
    private int countModules(Map<String, Object> contentJson) {
        if (contentJson == null || contentJson.isEmpty()) {
            return 0;
        }
        Object modules = contentJson.get("modules");
        if (modules instanceof List<?> list) {
            return list.size();
        }
        return 0;
    }

    private void enrichListRows(List<LandingPageVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<Long> keywordIds = rows.stream()
            .map(LandingPageVo::getKeywordId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, KeywordOpportunity> keywordById = Collections.emptyMap();
        if (!keywordIds.isEmpty()) {
            keywordById = keywordOpportunityMapper.selectList(
                Wrappers.lambdaQuery(KeywordOpportunity.class)
                    .in(KeywordOpportunity::getId, keywordIds)
                    .isNull(KeywordOpportunity::getDeletedAt)
            ).stream().collect(Collectors.toMap(KeywordOpportunity::getId, Function.identity(), (a, b) -> a));
        }

        Set<Long> pageIds = rows.stream().map(LandingPageVo::getId).collect(Collectors.toSet());
        Map<Long, LandingPage> pageById = landingPageMapper.selectList(
            Wrappers.lambdaQuery(LandingPage.class)
                .in(LandingPage::getId, pageIds)
        ).stream().collect(Collectors.toMap(LandingPage::getId, Function.identity(), (a, b) -> a));

        for (LandingPageVo row : rows) {
            if (row.getKeywordId() != null) {
                KeywordOpportunity kw = keywordById.get(row.getKeywordId());
                if (kw != null) {
                    row.setKeywordText(kw.getKeyword());
                }
            }
            LandingPage page = pageById.get(row.getId());
            if (page != null) {
                row.setModuleCount(countModules(page.getContentJson()));
            }
        }
    }

    private String resolveUniqueSlug(Long projectId, String baseText) {
        String base = slugify(baseText);
        if (StringUtils.isBlank(base)) {
            base = "landing-page";
        }
        String candidate = base;
        int suffix = 2;
        while (slugExists(projectId, candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
            if (suffix > 100) {
                candidate = base + "-" + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }
        return candidate;
    }

    private boolean slugExists(Long projectId, String slug) {
        return landingPageMapper.selectCount(
            Wrappers.lambdaQuery(LandingPage.class)
                .eq(LandingPage::getProjectId, projectId)
                .eq(LandingPage::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .eq(LandingPage::getSlug, slug)
        ) > 0;
    }

    private String slugify(String text) {
        String slug = text.toLowerCase()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
        if (slug.length() > 180) {
            slug = slug.substring(0, 180).replaceAll("-+$", "");
        }
        return slug;
    }

    private void updatePageStatus(Long pageId, String status) {
        LandingPage patch = new LandingPage();
        patch.setId(pageId);
        patch.setStatus(status);
        patch.setUpdatedAt(OffsetDateTime.now());
        landingPageMapper.updateById(patch);
    }

    private String defaultMarket(KeywordOpportunity keyword) {
        if (StringUtils.isNotBlank(keyword.getMarket())) {
            return keyword.getMarket();
        }
        return "US";
    }

    private void assertProjectOwned(Long projectId) {
        getOwnedProjectOrThrow(projectId);
    }

    private CustomerProject getOwnedProjectOrThrow(Long projectId) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        CustomerProject project = customerProjectMapper.selectOne(
            Wrappers.lambdaQuery(CustomerProject.class)
                .eq(CustomerProject::getId, projectId)
                .eq(CustomerProject::getTenantId, tenantId)
                .isNull(CustomerProject::getDeletedAt)
        );
        if (project == null) {
            throw new ServiceException("项目不存在或无权访问", 403);
        }
        return project;
    }

    private KeywordOpportunity getOwnedKeywordOrThrow(Long projectId, Long keywordId) {
        KeywordOpportunity entity = getOwnedKeywordOrNull(projectId, keywordId);
        if (entity == null) {
            throw new ServiceException("关键词不存在或不属于当前项目");
        }
        return entity;
    }

    private KeywordOpportunity getOwnedKeywordOrNull(Long projectId, Long keywordId) {
        return keywordOpportunityMapper.selectOne(
            Wrappers.lambdaQuery(KeywordOpportunity.class)
                .eq(KeywordOpportunity::getId, keywordId)
                .eq(KeywordOpportunity::getProjectId, projectId)
                .eq(KeywordOpportunity::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(KeywordOpportunity::getDeletedAt)
        );
    }

    private LandingPage getOwnedPageOrThrow(Long projectId, Long pageId) {
        assertProjectOwned(projectId);
        LandingPage entity = landingPageMapper.selectOne(
            Wrappers.lambdaQuery(LandingPage.class)
                .eq(LandingPage::getId, pageId)
                .eq(LandingPage::getProjectId, projectId)
                .eq(LandingPage::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(LandingPage::getDeletedAt)
        );
        if (entity == null) {
            throw new ServiceException("落地页不存在");
        }
        return entity;
    }
}
