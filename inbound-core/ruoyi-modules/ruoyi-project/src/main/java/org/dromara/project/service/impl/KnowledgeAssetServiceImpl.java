package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.aiclient.client.AiServiceClient;
import org.dromara.aiclient.model.AiApiResponse;
import org.dromara.aiclient.model.RagChunkHit;
import org.dromara.aiclient.model.RagSearchData;
import org.dromara.aiclient.model.RagSearchRequest;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.domain.KnowledgeAsset;
import org.dromara.project.domain.bo.KnowledgeAssetBo;
import org.dromara.project.domain.bo.KnowledgeSearchBo;
import org.dromara.project.domain.vo.KnowledgeAssetVo;
import org.dromara.project.domain.vo.KnowledgeRagHitVo;
import org.dromara.project.domain.vo.KnowledgeRagSearchVo;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.dromara.project.mapper.KnowledgeAssetMapper;
import org.dromara.project.mq.AiEmbedPublisher;
import org.dromara.project.service.IKnowledgeAssetService;
import org.dromara.common.tenant.helper.BusinessTenantHelper;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeAssetServiceImpl implements IKnowledgeAssetService {

    private final KnowledgeAssetMapper knowledgeAssetMapper;
    private final CustomerProjectMapper customerProjectMapper;
    private final AiEmbedPublisher aiEmbedPublisher;
    private final AiServiceClient aiServiceClient;

    @Override
    public TableDataInfo<KnowledgeAssetVo> queryPageList(Long projectId, KnowledgeAssetBo bo, PageQuery pageQuery) {
        assertProjectOwned(projectId);
        LambdaQueryWrapper<KnowledgeAsset> lqw = Wrappers.lambdaQuery(KnowledgeAsset.class)
            .eq(KnowledgeAsset::getProjectId, projectId)
            .eq(KnowledgeAsset::getTenantId, BusinessTenantHelper.getBusinessTenantId())
            .isNull(KnowledgeAsset::getDeletedAt)
            .like(StringUtils.isNotBlank(bo.getTitle()), KnowledgeAsset::getTitle, bo.getTitle())
            .eq(StringUtils.isNotBlank(bo.getType()), KnowledgeAsset::getType, bo.getType())
            .eq(StringUtils.isNotBlank(bo.getVectorStatus()), KnowledgeAsset::getVectorStatus, bo.getVectorStatus())
            .orderByDesc(KnowledgeAsset::getCreatedAt);
        Page<KnowledgeAssetVo> page = knowledgeAssetMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public KnowledgeAssetVo queryById(Long projectId, Long assetId) {
        return MapstructUtils.convert(getOwnedAssetOrThrow(projectId, assetId), KnowledgeAssetVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(Long projectId, KnowledgeAssetBo bo) {
        assertProjectOwned(projectId);
        KnowledgeAsset entity = MapstructUtils.convert(bo, KnowledgeAsset.class);
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        OffsetDateTime now = OffsetDateTime.now();
        entity.setProjectId(projectId);
        entity.setTenantId(tenantId);
        entity.setType(StringUtils.blankToDefault(entity.getType(), "DOCUMENT"));
        entity.setVectorStatus("PENDING");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(LoginHelper.getUserId());
        knowledgeAssetMapper.insert(entity);
        dispatchEmbedAfterCommit(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(Long projectId, Long assetId, KnowledgeAssetBo bo) {
        KnowledgeAsset asset = getOwnedAssetOrThrow(projectId, assetId);
        asset.setTitle(bo.getTitle());
        asset.setType(StringUtils.blankToDefault(bo.getType(), asset.getType()));
        asset.setTags(bo.getTags());
        asset.setUpdatedAt(OffsetDateTime.now());
        return knowledgeAssetMapper.updateById(asset) > 0;
    }

    @Override
    public Boolean deleteById(Long projectId, Long assetId) {
        getOwnedAssetOrThrow(projectId, assetId);
        return knowledgeAssetMapper.update(
            null,
            Wrappers.lambdaUpdate(KnowledgeAsset.class)
                .set(KnowledgeAsset::getDeletedAt, OffsetDateTime.now())
                .set(KnowledgeAsset::getUpdatedAt, OffsetDateTime.now())
                .eq(KnowledgeAsset::getId, assetId)
                .eq(KnowledgeAsset::getProjectId, projectId)
                .eq(KnowledgeAsset::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(KnowledgeAsset::getDeletedAt)
        ) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean triggerReindex(Long projectId, Long assetId) {
        KnowledgeAsset asset = getOwnedAssetOrThrow(projectId, assetId);
        asset.setVectorStatus("PENDING");
        asset.setUpdatedAt(OffsetDateTime.now());
        knowledgeAssetMapper.updateById(asset);
        dispatchEmbedAfterCommit(asset);
        return true;
    }

    @Override
    public KnowledgeRagSearchVo searchRag(Long projectId, KnowledgeSearchBo bo) {
        assertProjectOwned(projectId);
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        int topK = bo.getTopK() == null ? 3 : Math.min(Math.max(bo.getTopK(), 1), 10);

        RagSearchRequest request = new RagSearchRequest();
        request.setTenantId(tenantId);
        request.setProjectId(projectId);
        request.setQuery(bo.getQuery().trim());
        request.setTopK(topK);

        try {
            AiApiResponse<RagSearchData> response = aiServiceClient.ragSearch(request);
            if (response == null || response.getCode() == null || response.getCode() != 0) {
                String msg = response != null && StringUtils.isNotBlank(response.getMessage())
                    ? response.getMessage()
                    : "RAG 检索失败";
                throw new ServiceException(msg, HttpStatus.ERROR);
            }
            KnowledgeRagSearchVo result = new KnowledgeRagSearchVo();
            List<RagChunkHit> hits = response.getData() != null ? response.getData().getHits() : List.of();
            List<KnowledgeRagHitVo> mapped = new ArrayList<>(hits.size());
            for (RagChunkHit hit : hits) {
                KnowledgeRagHitVo vo = new KnowledgeRagHitVo();
                vo.setChunkId(hit.getChunkId());
                vo.setAssetId(hit.getAssetId());
                vo.setChunkIndex(hit.getChunkIndex());
                vo.setChunkText(hit.getChunkText());
                vo.setScore(hit.getScore());
                mapped.add(vo);
            }
            result.setHits(mapped);
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("RAG 检索服务不可用: " + e.getMessage(), HttpStatus.ERROR);
        }
    }

    private void dispatchEmbedAfterCommit(KnowledgeAsset asset) {
        String traceId = StringUtils.blankToDefault(MDC.get("traceId"), UUID.randomUUID().toString());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                aiEmbedPublisher.publish(
                    traceId,
                    asset.getId(),
                    asset.getTenantId(),
                    asset.getProjectId(),
                    asset.getFileUrl()
                );
            }
        });
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
            throw new ServiceException("项目不存在或无权访问");
        }
    }

    private KnowledgeAsset getOwnedAssetOrThrow(Long projectId, Long assetId) {
        assertProjectOwned(projectId);
        KnowledgeAsset asset = knowledgeAssetMapper.selectOne(
            Wrappers.lambdaQuery(KnowledgeAsset.class)
                .eq(KnowledgeAsset::getId, assetId)
                .eq(KnowledgeAsset::getProjectId, projectId)
                .eq(KnowledgeAsset::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(KnowledgeAsset::getDeletedAt)
        );
        if (asset == null) {
            throw new ServiceException("知识库资产不存在");
        }
        return asset;
    }
}
