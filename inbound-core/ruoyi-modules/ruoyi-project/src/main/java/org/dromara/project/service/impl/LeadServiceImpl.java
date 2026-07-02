package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.domain.KeywordOpportunity;
import org.dromara.project.domain.LandingPage;
import org.dromara.project.domain.Lead;
import org.dromara.project.domain.LeadChannelEvent;
import org.dromara.project.domain.LeadFollowup;
import org.dromara.project.domain.bo.LeadFollowupCreateBo;
import org.dromara.project.domain.bo.LeadQueryBo;
import org.dromara.project.domain.bo.LeadUpdateBo;
import org.dromara.project.domain.bo.PublicLeadEventBo;
import org.dromara.project.domain.bo.PublicLeadSubmitBo;
import org.dromara.project.domain.vo.LeadAiSuggestionVo;
import org.dromara.project.domain.vo.LeadDetailVo;
import org.dromara.project.domain.vo.LeadFollowupVo;
import org.dromara.project.domain.vo.LeadVo;
import org.dromara.project.domain.vo.PublicLeadEventVo;
import org.dromara.project.domain.vo.PublicLeadSubmitVo;
import org.dromara.project.domain.vo.WhatsappClickStatsVo;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.dromara.project.mapper.KeywordOpportunityMapper;
import org.dromara.project.mapper.LandingPageMapper;
import org.dromara.project.mapper.LeadChannelEventMapper;
import org.dromara.project.mapper.LeadFollowupMapper;
import org.dromara.project.mapper.LeadMapper;
import org.dromara.project.service.ILeadService;
import org.dromara.project.support.BusinessTenantHelper;
import org.dromara.project.support.LeadStatusTransition;
import org.dromara.project.support.TurnstileValidator;
import org.dromara.aiclient.client.AiServiceClient;
import org.dromara.aiclient.model.AiApiResponse;
import org.dromara.aiclient.model.FollowupGenerateData;
import org.dromara.aiclient.model.FollowupGenerateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.hutool.crypto.digest.DigestUtil;
import org.dromara.common.core.utils.ServletUtils;
import org.slf4j.MDC;

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
public class LeadServiceImpl implements ILeadService {

    private static final String EVENT_WHATSAPP_CLICK = "whatsapp_click";

    private final LeadMapper leadMapper;
    private final LeadFollowupMapper leadFollowupMapper;
    private final LeadChannelEventMapper leadChannelEventMapper;
    private final LandingPageMapper landingPageMapper;
    private final KeywordOpportunityMapper keywordOpportunityMapper;
    private final CustomerProjectMapper customerProjectMapper;
    private final TurnstileValidator turnstileValidator;
    private final AiServiceClient aiServiceClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublicLeadSubmitVo submitPublic(PublicLeadSubmitBo bo, String turnstileToken) {
        turnstileValidator.verifyOrSkip(turnstileToken);

        if (StringUtils.isBlank(bo.getEmail()) && StringUtils.isBlank(bo.getPhone())) {
            throw new ServiceException("邮箱与电话至少填写一项");
        }

        LandingPage page = landingPageMapper.selectOne(
            Wrappers.lambdaQuery(LandingPage.class)
                .eq(LandingPage::getId, bo.getLandingPageId())
                .isNull(LandingPage::getDeletedAt)
        );
        if (page == null) {
            throw new ServiceException("落地页不存在", 404);
        }

        OffsetDateTime now = OffsetDateTime.now();
        Lead entity = new Lead();
        entity.setTenantId(page.getTenantId());
        entity.setProjectId(page.getProjectId());
        entity.setLandingPageId(page.getId());
        entity.setKeywordId(page.getKeywordId());
        entity.setName(StringUtils.trim(bo.getName()));
        entity.setEmail(StringUtils.trim(bo.getEmail()));
        entity.setPhone(StringUtils.trim(bo.getPhone()));
        entity.setTravelDate(bo.getTravelDate());
        entity.setPartySize(bo.getPartySize());
        entity.setBudget(StringUtils.trim(bo.getBudget()));
        entity.setMessage(StringUtils.trim(bo.getMessage()));
        entity.setSource(StringUtils.blankToDefault(StringUtils.trim(bo.getSource()), "form"));
        entity.setUtmJson(bo.getUtm() != null ? bo.getUtm() : new HashMap<>());
        entity.setDevice(StringUtils.trim(bo.getDevice()));
        entity.setStatus("NEW");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        leadMapper.insert(entity);
        return new PublicLeadSubmitVo(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublicLeadEventVo recordPublicEvent(PublicLeadEventBo bo) {
        String eventType = StringUtils.trim(bo.getEventType());
        if (!EVENT_WHATSAPP_CLICK.equalsIgnoreCase(eventType)) {
            throw new ServiceException("不支持的事件类型: " + eventType);
        }

        CustomerProject project = customerProjectMapper.selectOne(
            Wrappers.lambdaQuery(CustomerProject.class)
                .eq(CustomerProject::getId, bo.getProjectId())
                .isNull(CustomerProject::getDeletedAt)
        );
        if (project == null) {
            throw new ServiceException("项目不存在", 404);
        }

        Long landingPageId = bo.getLandingPageId();
        if (landingPageId != null) {
            LandingPage page = landingPageMapper.selectOne(
                Wrappers.lambdaQuery(LandingPage.class)
                    .eq(LandingPage::getId, landingPageId)
                    .isNull(LandingPage::getDeletedAt)
            );
            if (page == null) {
                throw new ServiceException("落地页不存在", 404);
            }
            if (!Objects.equals(page.getProjectId(), project.getId())) {
                throw new ServiceException("落地页与项目不匹配");
            }
        }

        String clientIp = StringUtils.blankToDefault(ServletUtils.getClientIP(), "unknown");
        OffsetDateTime now = OffsetDateTime.now();
        LeadChannelEvent entity = new LeadChannelEvent();
        entity.setTenantId(project.getTenantId());
        entity.setProjectId(project.getId());
        entity.setLandingPageId(landingPageId);
        entity.setEventType(EVENT_WHATSAPP_CLICK);
        entity.setUtmJson(bo.getUtm() != null ? bo.getUtm() : new HashMap<>());
        entity.setDevice(StringUtils.trim(bo.getDevice()));
        entity.setIpHash(DigestUtil.sha256Hex(clientIp));
        entity.setCreatedAt(now);
        leadChannelEventMapper.insert(entity);
        return new PublicLeadEventVo(entity.getId());
    }

    @Override
    public LeadAiSuggestionVo generateAiSuggestion(Long projectId, Long leadId) {
        Lead lead = getOwnedLeadOrThrow(projectId, leadId);
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();

        String keywordText = null;
        if (lead.getKeywordId() != null) {
            KeywordOpportunity keyword = keywordOpportunityMapper.selectById(lead.getKeywordId());
            if (keyword != null) {
                keywordText = StringUtils.blankToDefault(keyword.getKeywordEn(), keyword.getKeyword());
            }
        }

        FollowupGenerateRequest aiReq = new FollowupGenerateRequest();
        aiReq.setTenantId(tenantId);
        aiReq.setProjectId(projectId);
        aiReq.setLeadId(leadId);
        aiReq.setName(lead.getName());
        aiReq.setMessage(lead.getMessage());
        aiReq.setBudget(lead.getBudget());
        aiReq.setTravelDate(lead.getTravelDate());
        aiReq.setSource(lead.getSource());
        aiReq.setKeywordText(keywordText);
        aiReq.setTraceId(StringUtils.blankToDefault(MDC.get("traceId"), UUID.randomUUID().toString()));

        AiApiResponse<FollowupGenerateData> aiResp;
        try {
            aiResp = aiServiceClient.followupGenerate(aiReq);
        } catch (IllegalStateException ex) {
            throw new ServiceException("AI 跟进建议生成失败: " + ex.getMessage());
        }
        if (aiResp == null || aiResp.getCode() == null || aiResp.getCode() != 0 || aiResp.getData() == null) {
            String msg = aiResp != null ? aiResp.getMessage() : "empty response";
            throw new ServiceException("AI 跟进建议生成失败: " + msg);
        }

        FollowupGenerateData data = aiResp.getData();
        if (StringUtils.isBlank(data.getSuggestionEn()) && StringUtils.isBlank(data.getSuggestionZh())) {
            throw new ServiceException("AI 返回跟进建议为空");
        }

        LeadAiSuggestionVo vo = new LeadAiSuggestionVo();
        vo.setSuggestionEn(data.getSuggestionEn());
        vo.setSuggestionZh(data.getSuggestionZh());
        vo.setNeedsHumanReview(
            Boolean.TRUE.equals(data.getNeedsHumanReview()) || data.getNeedsHumanReview() == null
        );
        return vo;
    }

    @Override
    public TableDataInfo<LeadVo> queryPageList(Long projectId, LeadQueryBo bo, PageQuery pageQuery) {
        assertProjectOwned(projectId);
        LambdaQueryWrapper<Lead> lqw = Wrappers.lambdaQuery(Lead.class)
            .eq(Lead::getProjectId, projectId)
            .eq(Lead::getTenantId, BusinessTenantHelper.getBusinessTenantId())
            .isNull(Lead::getDeletedAt)
            .like(StringUtils.isNotBlank(bo.getName()), Lead::getName, bo.getName())
            .like(StringUtils.isNotBlank(bo.getEmail()), Lead::getEmail, bo.getEmail())
            .like(StringUtils.isNotBlank(bo.getPhone()), Lead::getPhone, bo.getPhone())
            .eq(StringUtils.isNotBlank(bo.getSource()), Lead::getSource, bo.getSource())
            .eq(StringUtils.isNotBlank(bo.getStatus()), Lead::getStatus, bo.getStatus())
            .orderByDesc(Lead::getCreatedAt);
        Page<LeadVo> page = leadMapper.selectVoPage(pageQuery.build(), lqw);
        enrichListRows(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public LeadDetailVo queryById(Long projectId, Long leadId) {
        Lead lead = getOwnedLeadOrThrow(projectId, leadId);
        LeadDetailVo detail = new LeadDetailVo();
        LeadVo base = MapstructUtils.convert(lead, LeadVo.class);
        org.springframework.beans.BeanUtils.copyProperties(base, detail);
        detail.setTravelDate(lead.getTravelDate());
        detail.setPartySize(lead.getPartySize());
        detail.setBudget(lead.getBudget());
        detail.setMessage(lead.getMessage());
        detail.setUtm(lead.getUtmJson());
        detail.setDevice(lead.getDevice());
        detail.setAssigneeId(lead.getAssigneeId());
        enrichAssigneeName(detail);
        enrichDetail(detail, lead);
        enrichWhatsappStats(detail, lead);
        detail.setFollowups(listFollowups(projectId, leadId));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeadDetailVo updateLead(Long projectId, Long leadId, LeadUpdateBo bo) {
        Lead lead = getOwnedLeadOrThrow(projectId, leadId);
        if (bo == null || (StringUtils.isBlank(bo.getStatus()) && bo.getAssigneeId() == null)) {
            throw new ServiceException("请提供 status 或 assigneeId");
        }

        boolean changed = false;
        if (StringUtils.isNotBlank(bo.getStatus())) {
            String nextStatus = bo.getStatus().trim().toUpperCase();
            LeadStatusTransition.validateTransition(lead.getStatus(), nextStatus);
            lead.setStatus(nextStatus);
            changed = true;
        }
        if (bo.getAssigneeId() != null) {
            lead.setAssigneeId(bo.getAssigneeId());
            changed = true;
        }
        if (!changed) {
            throw new ServiceException("请提供 status 或 assigneeId");
        }

        lead.setUpdatedAt(OffsetDateTime.now());
        leadMapper.updateById(lead);
        return queryById(projectId, leadId);
    }

    @Override
    public List<LeadFollowupVo> listFollowups(Long projectId, Long leadId) {
        getOwnedLeadOrThrow(projectId, leadId);
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        List<LeadFollowupVo> rows = leadFollowupMapper.selectVoList(
            Wrappers.lambdaQuery(LeadFollowup.class)
                .eq(LeadFollowup::getLeadId, leadId)
                .eq(LeadFollowup::getTenantId, tenantId)
                .isNull(LeadFollowup::getDeletedAt)
                .orderByAsc(LeadFollowup::getCreatedAt)
        );
        enrichFollowupOperators(rows);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeadFollowupVo createFollowup(Long projectId, Long leadId, LeadFollowupCreateBo bo) {
        Lead lead = getOwnedLeadOrThrow(projectId, leadId);
        if (LeadStatusTransition.isTerminal(lead.getStatus())) {
            throw new ServiceException("终态线索不可新增跟进");
        }
        if (StringUtils.isBlank(bo.getContent())) {
            throw new ServiceException("跟进内容不能为空");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Long operatorId = LoginHelper.getUserId();
        LeadFollowup entity = new LeadFollowup();
        entity.setTenantId(lead.getTenantId());
        entity.setLeadId(leadId);
        entity.setContent(StringUtils.trim(bo.getContent()));
        entity.setChannel(StringUtils.trim(bo.getChannel()));
        entity.setOperatorId(operatorId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(operatorId);
        leadFollowupMapper.insert(entity);

        LeadFollowupVo vo = MapstructUtils.convert(entity, LeadFollowupVo.class);
        enrichFollowupOperator(vo);
        return vo;
    }

    private void enrichDetail(LeadDetailVo detail, Lead lead) {
        if (lead.getLandingPageId() != null) {
            LandingPage page = landingPageMapper.selectById(lead.getLandingPageId());
            if (page != null) {
                detail.setLandingPageTitle(page.getTitle());
                detail.setLandingPageSlug(page.getSlug());
            }
        }
        if (lead.getKeywordId() != null) {
            KeywordOpportunity keyword = keywordOpportunityMapper.selectById(lead.getKeywordId());
            if (keyword != null) {
                detail.setKeywordText(keyword.getKeyword());
                detail.setKeywordMarket(keyword.getMarket());
            }
        }
    }

    private void enrichWhatsappStats(LeadDetailVo detail, Lead lead) {
        if (lead.getLandingPageId() == null) {
            detail.setWhatsappClickCount(0);
            return;
        }
        WhatsappClickStatsVo stats = leadChannelEventMapper.selectWhatsappClickStats(
            lead.getProjectId(),
            lead.getLandingPageId()
        );
        if (stats == null || stats.getClickCount() == null || stats.getClickCount() <= 0) {
            detail.setWhatsappClickCount(0);
            return;
        }
        detail.setWhatsappClickCount(Math.toIntExact(stats.getClickCount()));
        detail.setLastWhatsappClickAt(stats.getLastClickAt());
    }

    private void enrichListRows(List<LeadVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<Long> pageIds = rows.stream()
            .map(LeadVo::getLandingPageId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, LandingPage> pageById = Collections.emptyMap();
        if (!pageIds.isEmpty()) {
            pageById = landingPageMapper.selectList(
                Wrappers.lambdaQuery(LandingPage.class).in(LandingPage::getId, pageIds)
            ).stream().collect(Collectors.toMap(LandingPage::getId, Function.identity(), (a, b) -> a));
        }

        Set<Long> keywordIds = rows.stream()
            .map(LeadVo::getKeywordId)
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

        for (LeadVo row : rows) {
            if (row.getLandingPageId() != null) {
                LandingPage page = pageById.get(row.getLandingPageId());
                if (page != null) {
                    row.setLandingPageTitle(page.getTitle());
                    row.setLandingPageSlug(page.getSlug());
                }
            }
            if (row.getKeywordId() != null) {
                KeywordOpportunity kw = keywordById.get(row.getKeywordId());
                if (kw != null) {
                    row.setKeywordText(kw.getKeyword());
                    row.setKeywordMarket(kw.getMarket());
                }
            }
            enrichAssigneeName(row);
        }
    }

    private void enrichAssigneeName(LeadVo row) {
        if (row.getAssigneeId() == null) {
            return;
        }
        String name = leadMapper.selectUserDisplayName(row.getAssigneeId());
        if (StringUtils.isNotBlank(name)) {
            row.setAssigneeName(name);
        }
    }

    private void enrichFollowupOperators(List<LeadFollowupVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (LeadFollowupVo row : rows) {
            enrichFollowupOperator(row);
        }
    }

    private void enrichFollowupOperator(LeadFollowupVo row) {
        if (row.getOperatorId() == null) {
            return;
        }
        String name = leadMapper.selectUserDisplayName(row.getOperatorId());
        if (StringUtils.isNotBlank(name)) {
            row.setOperatorName(name);
        }
    }

    private Lead getOwnedLeadOrThrow(Long projectId, Long leadId) {
        assertProjectOwned(projectId);
        Lead lead = leadMapper.selectOne(
            Wrappers.lambdaQuery(Lead.class)
                .eq(Lead::getId, leadId)
                .eq(Lead::getProjectId, projectId)
                .eq(Lead::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(Lead::getDeletedAt)
        );
        if (lead == null) {
            throw new ServiceException("线索不存在", 404);
        }
        return lead;
    }

    private void assertProjectOwned(Long projectId) {
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
    }
}
