package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.aiclient.client.AiServiceClient;
import org.dromara.aiclient.model.AiApiResponse;
import org.dromara.aiclient.model.GeneratedKeyword;
import org.dromara.aiclient.model.KeywordGenerateData;
import org.dromara.aiclient.model.KeywordGenerateRequest;
import org.dromara.aiclient.model.StageKeywords;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.domain.KeywordOpportunity;
import org.dromara.project.domain.bo.KeywordGenerateBo;
import org.dromara.project.domain.bo.KeywordOpportunityBo;
import org.dromara.project.domain.vo.KeywordGenerateVo;
import org.dromara.project.domain.vo.KeywordOpportunityVo;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.dromara.project.mapper.KeywordOpportunityMapper;
import org.dromara.project.service.IKeywordOpportunityService;
import org.dromara.project.support.BusinessTenantHelper;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KeywordOpportunityServiceImpl implements IKeywordOpportunityService {

    private final KeywordOpportunityMapper keywordOpportunityMapper;
    private final CustomerProjectMapper customerProjectMapper;
    private final AiServiceClient aiServiceClient;

    @Override
    public TableDataInfo<KeywordOpportunityVo> queryPageList(
        Long projectId,
        KeywordOpportunityBo bo,
        PageQuery pageQuery
    ) {
        assertProjectOwned(projectId);
        LambdaQueryWrapper<KeywordOpportunity> lqw = Wrappers.lambdaQuery(KeywordOpportunity.class)
            .eq(KeywordOpportunity::getProjectId, projectId)
            .eq(KeywordOpportunity::getTenantId, BusinessTenantHelper.getBusinessTenantId())
            .isNull(KeywordOpportunity::getDeletedAt)
            .eq(StringUtils.isNotBlank(bo.getStage()), KeywordOpportunity::getStage, bo.getStage())
            .eq(StringUtils.isNotBlank(bo.getMarket()), KeywordOpportunity::getMarket, bo.getMarket())
            .eq(StringUtils.isNotBlank(bo.getStatus()), KeywordOpportunity::getStatus, bo.getStatus())
            .like(StringUtils.isNotBlank(bo.getKeyword()), KeywordOpportunity::getKeyword, bo.getKeyword())
            .orderByDesc(KeywordOpportunity::getScore)
            .orderByDesc(KeywordOpportunity::getCreatedAt);
        Page<KeywordOpportunityVo> page = keywordOpportunityMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(Long projectId, KeywordOpportunityBo bo) {
        assertProjectOwned(projectId);
        KeywordOpportunity entity = MapstructUtils.convert(bo, KeywordOpportunity.class);
        OffsetDateTime now = OffsetDateTime.now();
        entity.setProjectId(projectId);
        entity.setTenantId(BusinessTenantHelper.getBusinessTenantId());
        entity.setKeywordEn(StringUtils.blankToDefault(entity.getKeywordEn(), entity.getKeyword()));
        entity.setStatus(StringUtils.blankToDefault(entity.getStatus(), "ACTIVE"));
        if (entity.getScoreDetailJson() == null) {
            entity.setScoreDetailJson(Collections.emptyMap());
        }
        if (entity.getSourceJson() == null) {
            entity.setSourceJson(Map.of("source", "manual"));
        }
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(LoginHelper.getUserId());
        keywordOpportunityMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean deleteById(Long projectId, Long keywordId) {
        getOwnedKeywordOrThrow(projectId, keywordId);
        return keywordOpportunityMapper.update(
            null,
            Wrappers.lambdaUpdate(KeywordOpportunity.class)
                .set(KeywordOpportunity::getDeletedAt, OffsetDateTime.now())
                .set(KeywordOpportunity::getUpdatedAt, OffsetDateTime.now())
                .eq(KeywordOpportunity::getId, keywordId)
                .eq(KeywordOpportunity::getProjectId, projectId)
                .eq(KeywordOpportunity::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(KeywordOpportunity::getDeletedAt)
        ) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KeywordGenerateVo generateKeywords(Long projectId, KeywordGenerateBo bo) {
        CustomerProject project = getOwnedProjectOrThrow(projectId);
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        String market = StringUtils.blankToDefault(bo.getMarket(), defaultMarket(project));

        KeywordGenerateRequest aiReq = new KeywordGenerateRequest();
        aiReq.setTenantId(tenantId);
        aiReq.setProjectId(projectId);
        aiReq.setMarket(market);
        aiReq.setLocale(StringUtils.blankToDefault(bo.getLocale(), "en"));
        aiReq.setStages(bo.getStages());
        aiReq.setWordsPerStage(bo.getWordsPerStage() != null ? bo.getWordsPerStage() : 5);
        aiReq.setUseRag(bo.getUseRag() != null ? bo.getUseRag() : Boolean.TRUE);
        aiReq.setTraceId(StringUtils.blankToDefault(MDC.get("traceId"), UUID.randomUUID().toString()));

        AiApiResponse<KeywordGenerateData> aiResp;
        try {
            aiResp = aiServiceClient.keywordsGenerate(aiReq);
        } catch (IllegalStateException ex) {
            throw new ServiceException("AI 关键词生成失败: " + ex.getMessage());
        }
        if (aiResp == null || aiResp.getCode() == null || aiResp.getCode() != 0 || aiResp.getData() == null) {
            String msg = aiResp != null ? aiResp.getMessage() : "empty response";
            throw new ServiceException("AI 关键词生成失败: " + msg);
        }

        KeywordGenerateData data = aiResp.getData();
        int inserted = persistGeneratedKeywords(projectId, tenantId, market, data);
        KeywordGenerateVo vo = new KeywordGenerateVo();
        vo.setInsertedCount(inserted);
        vo.setNeedsHumanReview(Boolean.TRUE.equals(data.getNeedsHumanReview()) || data.getNeedsHumanReview() == null);
        vo.setCaptureMethod(data.getCaptureMethod());
        return vo;
    }

    private int persistGeneratedKeywords(
        Long projectId,
        Long tenantId,
        String market,
        KeywordGenerateData data
    ) {
        if (data.getStages() == null || data.getStages().isEmpty()) {
            throw new ServiceException("AI 未返回任何阶段关键词");
        }
        OffsetDateTime now = OffsetDateTime.now();
        Long userId = LoginHelper.getUserId();
        int count = 0;
        for (StageKeywords stageBlock : data.getStages()) {
            if (stageBlock.getKeywords() == null) {
                continue;
            }
            for (GeneratedKeyword kw : stageBlock.getKeywords()) {
                if (StringUtils.isBlank(kw.getText())) {
                    continue;
                }
                KeywordOpportunity entity = new KeywordOpportunity();
                entity.setTenantId(tenantId);
                entity.setProjectId(projectId);
                entity.setKeyword(kw.getText().trim());
                entity.setKeywordEn(kw.getText().trim());
                entity.setKeywordCn(null);
                entity.setIntent(StringUtils.blankToDefault(kw.getRationale(), null));
                entity.setMarket(market);
                entity.setStage(stageBlock.getStage());
                entity.setScore(null);
                entity.setScoreDetailJson(Collections.emptyMap());
                entity.setChannel(null);
                entity.setSourceJson(buildSourceJson(data, kw));
                entity.setStatus("ACTIVE");
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                entity.setCreatedBy(userId);
                keywordOpportunityMapper.insert(entity);
                count++;
            }
        }
        if (count == 0) {
            throw new ServiceException("AI 返回关键词为空，未写入数据库");
        }
        return count;
    }

    private Map<String, Object> buildSourceJson(KeywordGenerateData data, GeneratedKeyword kw) {
        Map<String, Object> source = new HashMap<>();
        source.put("source", "ai");
        source.put("needs_human_review", kw.getNeedsHumanReview() != null ? kw.getNeedsHumanReview() : true);
        source.put("capture_method", data.getCaptureMethod());
        source.put("model", data.getModel());
        if (StringUtils.isNotBlank(kw.getRationale())) {
            source.put("rationale", kw.getRationale());
        }
        if (kw.getChunkIds() != null && !kw.getChunkIds().isEmpty()) {
            source.put("chunk_ids", new ArrayList<>(kw.getChunkIds()));
        }
        return source;
    }

    private String defaultMarket(CustomerProject project) {
        if (project.getTargetMarkets() != null && !project.getTargetMarkets().isEmpty()) {
            return project.getTargetMarkets().get(0);
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
        assertProjectOwned(projectId);
        KeywordOpportunity entity = keywordOpportunityMapper.selectOne(
            Wrappers.lambdaQuery(KeywordOpportunity.class)
                .eq(KeywordOpportunity::getId, keywordId)
                .eq(KeywordOpportunity::getProjectId, projectId)
                .eq(KeywordOpportunity::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(KeywordOpportunity::getDeletedAt)
        );
        if (entity == null) {
            throw new ServiceException("关键词不存在");
        }
        return entity;
    }
}
