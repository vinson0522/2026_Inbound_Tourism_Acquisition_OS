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
import org.dromara.aiclient.model.KeywordScoreData;
import org.dromara.aiclient.model.KeywordScoreRequest;
import org.dromara.aiclient.model.StageKeywords;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.project.domain.Competitor;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.billing.QuotaType;
import org.dromara.project.domain.KeywordOpportunity;
import org.dromara.project.domain.bo.KeywordGenerateBo;
import org.dromara.project.domain.bo.KeywordOpportunityBo;
import org.dromara.project.domain.bo.KeywordScoreBatchBo;
import org.dromara.project.domain.vo.KeywordGenerateVo;
import org.dromara.project.domain.vo.KeywordOpportunityVo;
import org.dromara.project.domain.vo.KeywordScoreBatchVo;
import org.dromara.project.domain.vo.KeywordScoreVo;
import org.dromara.project.mapper.CompetitorMapper;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.dromara.project.mapper.KeywordOpportunityMapper;
import org.dromara.project.service.IKeywordOpportunityService;
import org.dromara.project.service.IQuotaService;
import org.dromara.common.tenant.helper.BusinessTenantHelper;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private static final int SCORE_BATCH_LIMIT = 50;

    private final KeywordOpportunityMapper keywordOpportunityMapper;
    private final CustomerProjectMapper customerProjectMapper;
    private final CompetitorMapper competitorMapper;
    private final AiServiceClient aiServiceClient;
    private final IQuotaService quotaService;

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
            .like(StringUtils.isNotBlank(bo.getKeyword()), KeywordOpportunity::getKeyword, bo.getKeyword());
        PageQuery effectivePageQuery = pageQuery;
        if (StringUtils.isBlank(pageQuery.getOrderByColumn())) {
            lqw.last("ORDER BY score DESC NULLS LAST, created_at DESC");
            effectivePageQuery = pageQueryWithoutOrder(pageQuery);
        } else if (isScoreColumn(pageQuery.getOrderByColumn())) {
            if (isDescSort(pageQuery.getIsAsc())) {
                lqw.last("ORDER BY score DESC NULLS LAST, created_at DESC");
            } else {
                lqw.last("ORDER BY score ASC NULLS LAST, created_at DESC");
            }
            effectivePageQuery = pageQueryWithoutOrder(pageQuery);
        }
        Page<KeywordOpportunityVo> page = keywordOpportunityMapper.selectVoPage(effectivePageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    private static PageQuery pageQueryWithoutOrder(PageQuery pageQuery) {
        return new PageQuery(pageQuery.getPageSize(), pageQuery.getPageNum());
    }

    private static boolean isScoreColumn(String orderByColumn) {
        return "score".equalsIgnoreCase(StringUtils.trim(orderByColumn));
    }

    private static boolean isDescSort(String isAsc) {
        if (StringUtils.isBlank(isAsc)) {
            return true;
        }
        String normalized = StringUtils.replaceEach(
            isAsc,
            new String[] {"ascending", "descending"},
            new String[] {"asc", "desc"}
        );
        return "desc".equalsIgnoreCase(StringUtils.trim(normalized));
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
        quotaService.checkAndConsume(tenantId, QuotaType.KEYWORDS_PER_MONTH, 1);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KeywordScoreVo scoreKeyword(Long projectId, Long keywordId, Boolean useRag) {
        CustomerProject project = getOwnedProjectOrThrow(projectId);
        KeywordOpportunity entity = getOwnedKeywordOrThrow(projectId, keywordId);
        return scoreEntity(project, entity, useRag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KeywordScoreBatchVo scoreBatch(Long projectId, KeywordScoreBatchBo bo) {
        CustomerProject project = getOwnedProjectOrThrow(projectId);
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        List<KeywordOpportunity> targets = resolveScoreTargets(projectId, tenantId, bo);
        if (targets.isEmpty()) {
            throw new ServiceException("没有可评分的关键词");
        }

        KeywordScoreBatchVo batchVo = new KeywordScoreBatchVo();
        Boolean useRag = bo != null ? bo.getUseRag() : null;
        for (KeywordOpportunity entity : targets) {
            batchVo.getResults().add(scoreEntity(project, entity, useRag));
        }
        batchVo.setScoredCount(batchVo.getResults().size());
        return batchVo;
    }

    private List<KeywordOpportunity> resolveScoreTargets(
        Long projectId,
        Long tenantId,
        KeywordScoreBatchBo bo
    ) {
        List<Long> ids = bo != null ? bo.getKeywordIds() : null;
        if (ids != null && !ids.isEmpty()) {
            if (ids.size() > SCORE_BATCH_LIMIT) {
                throw new ServiceException("批量评分最多 " + SCORE_BATCH_LIMIT + " 条");
            }
            return keywordOpportunityMapper.selectList(
                Wrappers.lambdaQuery(KeywordOpportunity.class)
                    .eq(KeywordOpportunity::getProjectId, projectId)
                    .eq(KeywordOpportunity::getTenantId, tenantId)
                    .in(KeywordOpportunity::getId, ids)
                    .isNull(KeywordOpportunity::getDeletedAt)
            );
        }
        return keywordOpportunityMapper.selectList(
            Wrappers.lambdaQuery(KeywordOpportunity.class)
                .eq(KeywordOpportunity::getProjectId, projectId)
                .eq(KeywordOpportunity::getTenantId, tenantId)
                .apply("status = 'ACTIVE'::entity_status")
                .isNull(KeywordOpportunity::getDeletedAt)
                .orderByDesc(KeywordOpportunity::getCreatedAt)
                .last("LIMIT " + SCORE_BATCH_LIMIT)
        );
    }

    private KeywordScoreVo scoreEntity(
        CustomerProject project,
        KeywordOpportunity entity,
        Boolean useRag
    ) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        KeywordScoreRequest aiReq = buildScoreRequest(project, entity, tenantId, useRag);

        AiApiResponse<KeywordScoreData> aiResp;
        try {
            aiResp = aiServiceClient.keywordsScore(aiReq);
        } catch (IllegalStateException ex) {
            throw new ServiceException("AI 关键词评分失败: " + ex.getMessage());
        }
        if (aiResp == null || aiResp.getCode() == null || aiResp.getCode() != 0 || aiResp.getData() == null) {
            String msg = aiResp != null ? aiResp.getMessage() : "empty response";
            throw new ServiceException("AI 关键词评分失败: " + msg);
        }

        KeywordScoreData data = aiResp.getData();
        if (data.getScore() == null) {
            throw new ServiceException("AI 未返回 score");
        }

        BigDecimal score = BigDecimal.valueOf(data.getScore()).setScale(1, RoundingMode.HALF_UP);
        Map<String, Object> detail = data.getScoreDetail() != null
            ? new HashMap<>(data.getScoreDetail())
            : Collections.emptyMap();

        OffsetDateTime now = OffsetDateTime.now();
        entity.setScore(score);
        entity.setScoreDetailJson(detail);
        entity.setUpdatedAt(now);
        keywordOpportunityMapper.updateById(entity);

        KeywordScoreVo vo = new KeywordScoreVo();
        vo.setKeywordId(entity.getId());
        vo.setKeyword(entity.getKeyword());
        vo.setScore(score);
        vo.setScoreDetailJson(detail);
        return vo;
    }

    private KeywordScoreRequest buildScoreRequest(
        CustomerProject project,
        KeywordOpportunity entity,
        Long tenantId,
        Boolean useRag
    ) {
        BigDecimal geoScore = keywordOpportunityMapper.selectLatestGeoScore(tenantId, project.getId());
        List<String> competitors = loadCompetitorNames(project.getId(), tenantId);

        KeywordScoreRequest aiReq = new KeywordScoreRequest();
        aiReq.setTenantId(tenantId);
        aiReq.setProjectId(project.getId());
        aiReq.setKeywordId(entity.getId());
        aiReq.setKeyword(entity.getKeyword());
        aiReq.setKeywordEn(StringUtils.blankToDefault(entity.getKeywordEn(), entity.getKeyword()));
        aiReq.setStage(entity.getStage());
        aiReq.setMarket(StringUtils.blankToDefault(entity.getMarket(), defaultMarket(project)));
        aiReq.setBrandName(StringUtils.blankToDefault(project.getBrandName(), project.getName()));
        if (!competitors.isEmpty()) {
            aiReq.setCompetitors(competitors);
        }
        if (geoScore != null) {
            aiReq.setGeoScore(geoScore.doubleValue());
        }
        aiReq.setUseRag(useRag != null ? useRag : Boolean.TRUE);
        aiReq.setTraceId(StringUtils.blankToDefault(MDC.get("traceId"), UUID.randomUUID().toString()));
        return aiReq;
    }

    private List<String> loadCompetitorNames(Long projectId, Long tenantId) {
        List<Competitor> rows = competitorMapper.selectList(
            Wrappers.lambdaQuery(Competitor.class)
                .eq(Competitor::getProjectId, projectId)
                .eq(Competitor::getTenantId, tenantId)
                .isNull(Competitor::getDeletedAt)
                .orderByDesc(Competitor::getCreatedAt)
                .last("LIMIT 10")
        );
        List<String> names = new ArrayList<>(rows.size());
        for (Competitor row : rows) {
            if (StringUtils.isNotBlank(row.getName())) {
                names.add(row.getName().trim());
            }
        }
        return names;
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
