package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.domain.MaterialAsset;
import org.dromara.project.domain.VideoBreakdown;
import org.dromara.project.domain.bo.MaterialAssetQueryBo;
import org.dromara.project.domain.bo.MaterialBreakdownCallbackBo;
import org.dromara.project.domain.vo.MaterialAssetVo;
import org.dromara.project.domain.vo.MaterialBreakdownTriggerVo;
import org.dromara.project.domain.vo.VideoBreakdownVo;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.dromara.project.mapper.MaterialAssetMapper;
import org.dromara.project.mapper.VideoBreakdownMapper;
import org.dromara.project.mq.AiBreakdownPublisher;
import org.dromara.project.service.IMaterialAssetService;
import org.dromara.project.service.MaterialStorageService;
import org.dromara.common.tenant.helper.BusinessTenantHelper;
import org.dromara.project.support.MaterialBreakdownSupport;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialAssetServiceImpl implements IMaterialAssetService {

    private final MaterialAssetMapper materialAssetMapper;
    private final VideoBreakdownMapper videoBreakdownMapper;
    private final CustomerProjectMapper customerProjectMapper;
    private final MaterialStorageService materialStorageService;
    private final AiBreakdownPublisher aiBreakdownPublisher;

    @Override
    public TableDataInfo<MaterialAssetVo> queryPageList(Long projectId, MaterialAssetQueryBo bo, PageQuery pageQuery) {
        assertProjectOwned(projectId);
        MaterialAssetQueryBo query = bo != null ? bo : new MaterialAssetQueryBo();
        LambdaQueryWrapper<MaterialAsset> lqw = Wrappers.lambdaQuery(MaterialAsset.class)
            .eq(MaterialAsset::getProjectId, projectId)
            .eq(MaterialAsset::getTenantId, BusinessTenantHelper.getBusinessTenantId())
            .isNull(MaterialAsset::getDeletedAt)
            .eq(StringUtils.isNotBlank(query.getType()), MaterialAsset::getType, query.getType())
            .eq(StringUtils.isNotBlank(query.getCopyrightStatus()), MaterialAsset::getCopyrightStatus, query.getCopyrightStatus())
            .orderByDesc(MaterialAsset::getCreatedAt);
        Page<MaterialAssetVo> page = materialAssetMapper.selectVoPage(pageQuery.build(), lqw);
        enrichListRows(projectId, page.getRecords());
        if (StringUtils.isNotBlank(query.getBreakdownStatus())) {
            List<MaterialAssetVo> filtered = page.getRecords().stream()
                .filter(row -> query.getBreakdownStatus().equalsIgnoreCase(row.getBreakdownStatus()))
                .toList();
            page.setRecords(filtered);
            page.setTotal(filtered.size());
        }
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upload(Long projectId, MultipartFile file, String type, String copyrightStatus, String source) {
        assertProjectOwned(projectId);
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        String url = materialStorageService.uploadMaterial(tenantId, projectId, file);
        String filename = file.getOriginalFilename();
        String resolvedType = StringUtils.isNotBlank(type)
            ? type.toUpperCase()
            : MaterialBreakdownSupport.inferMaterialType(filename, file.getContentType());

        OffsetDateTime now = OffsetDateTime.now();
        MaterialAsset entity = new MaterialAsset();
        entity.setTenantId(tenantId);
        entity.setProjectId(projectId);
        entity.setType(resolvedType);
        entity.setUrl(url);
        entity.setThumbnailUrl(null);
        entity.setTags(Collections.emptyList());
        entity.setCopyrightStatus(StringUtils.blankToDefault(copyrightStatus, "unknown"));
        entity.setSource(source);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(LoginHelper.getUserId());
        materialAssetMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialBreakdownTriggerVo triggerBreakdown(Long projectId, Long materialId) {
        MaterialAsset material = getOwnedMaterialOrThrow(projectId, materialId);
        if (!"VIDEO".equalsIgnoreCase(material.getType())) {
            throw new ServiceException("仅 VIDEO 类型素材支持拆解", HttpStatus.BAD_REQUEST);
        }

        OffsetDateTime now = OffsetDateTime.now();
        VideoBreakdown breakdown = new VideoBreakdown();
        breakdown.setTenantId(material.getTenantId());
        breakdown.setProjectId(projectId);
        breakdown.setSourceUrl(material.getUrl());
        breakdown.setFramesJson(Collections.emptyList());
        breakdown.setDimensionsJson(new HashMap<>());
        breakdown.setReusableStructure(null);
        breakdown.setCreatedAt(now);
        breakdown.setUpdatedAt(now);
        breakdown.setCreatedBy(LoginHelper.getUserId());
        videoBreakdownMapper.insert(breakdown);

        dispatchBreakdownAfterCommit(breakdown, material);
        MaterialBreakdownTriggerVo vo = new MaterialBreakdownTriggerVo();
        vo.setBreakdownId(breakdown.getId());
        return vo;
    }

    @Override
    public VideoBreakdownVo queryBreakdown(Long projectId, Long breakdownId) {
        VideoBreakdown breakdown = getOwnedBreakdownOrThrow(projectId, breakdownId);
        MaterialAsset material = findMaterialBySourceUrl(projectId, breakdown.getSourceUrl());
        return toBreakdownVo(breakdown, material != null ? material.getId() : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleBreakdownCallback(MaterialBreakdownCallbackBo bo) {
        VideoBreakdown breakdown = videoBreakdownMapper.selectOne(
            Wrappers.lambdaQuery(VideoBreakdown.class)
                .eq(VideoBreakdown::getId, bo.getBreakdownId())
                .isNull(VideoBreakdown::getDeletedAt)
        );
        if (breakdown == null) {
            throw new ServiceException("拆解任务不存在", HttpStatus.NOT_FOUND);
        }

        if ("FAILED".equalsIgnoreCase(bo.getStatus())) {
            Map<String, Object> failed = new LinkedHashMap<>();
            failed.put("_status", "FAILED");
            failed.put("_error", StringUtils.blankToDefault(bo.getErrorMessage(), "breakdown failed"));
            breakdown.setDimensionsJson(failed);
            breakdown.setFramesJson(Collections.emptyList());
            breakdown.setReusableStructure(null);
        } else {
            Map<String, Object> dimensions = bo.getDimensions() != null
                ? new LinkedHashMap<>(bo.getDimensions())
                : new LinkedHashMap<>();
            if (Boolean.TRUE.equals(bo.getNeedsHumanReview())) {
                dimensions.put("_needsHumanReview", true);
            }
            breakdown.setDimensionsJson(dimensions);
            breakdown.setFramesJson(bo.getFrames() != null ? bo.getFrames() : Collections.emptyList());
            breakdown.setReusableStructure(bo.getReusableStructure());
        }
        breakdown.setUpdatedAt(OffsetDateTime.now());
        videoBreakdownMapper.updateById(breakdown);
        log.info(
            "material_breakdown_callback breakdownId={} status={} frameCount={}",
            bo.getBreakdownId(),
            bo.getStatus(),
            MaterialBreakdownSupport.frameCount(breakdown.getFramesJson())
        );
    }

    private void dispatchBreakdownAfterCommit(VideoBreakdown breakdown, MaterialAsset material) {
        String traceId = StringUtils.blankToDefault(MDC.get("traceId"), UUID.randomUUID().toString());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                aiBreakdownPublisher.publish(
                    traceId,
                    breakdown.getId(),
                    material.getId(),
                    material.getTenantId(),
                    material.getProjectId(),
                    material.getUrl()
                );
            }
        });
    }

    private void enrichListRows(Long projectId, List<MaterialAssetVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<String> urls = rows.stream().map(MaterialAssetVo::getUrl).filter(StringUtils::isNotBlank).distinct().toList();
        Map<String, VideoBreakdown> latestByUrl = loadLatestBreakdowns(projectId, urls);
        for (MaterialAssetVo row : rows) {
            row.setFileName(MaterialBreakdownSupport.extractFileName(row.getUrl()));
            VideoBreakdown breakdown = latestByUrl.get(row.getUrl());
            if (breakdown == null) {
                row.setBreakdownStatus(MaterialBreakdownSupport.STATUS_NONE);
                row.setFrameCount(0);
                row.setNeedsHumanReview(false);
                continue;
            }
            row.setBreakdownId(breakdown.getId());
            row.setBreakdownStatus(MaterialBreakdownSupport.resolveStatus(breakdown));
            row.setFrameCount(MaterialBreakdownSupport.frameCount(breakdown.getFramesJson()));
            row.setNeedsHumanReview(MaterialBreakdownSupport.readNeedsHumanReview(breakdown.getDimensionsJson()));
        }
    }

    private Map<String, VideoBreakdown> loadLatestBreakdowns(Long projectId, List<String> urls) {
        if (urls.isEmpty()) {
            return Map.of();
        }
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        List<VideoBreakdown> breakdowns = videoBreakdownMapper.selectList(
            Wrappers.lambdaQuery(VideoBreakdown.class)
                .eq(VideoBreakdown::getProjectId, projectId)
                .eq(VideoBreakdown::getTenantId, tenantId)
                .in(VideoBreakdown::getSourceUrl, urls)
                .isNull(VideoBreakdown::getDeletedAt)
                .orderByDesc(VideoBreakdown::getCreatedAt)
        );
        Map<String, VideoBreakdown> latest = new LinkedHashMap<>();
        for (VideoBreakdown item : breakdowns) {
            latest.putIfAbsent(item.getSourceUrl(), item);
        }
        return latest;
    }

    private VideoBreakdownVo toBreakdownVo(VideoBreakdown breakdown, Long materialId) {
        VideoBreakdownVo vo = new VideoBreakdownVo();
        vo.setBreakdownId(breakdown.getId());
        vo.setMaterialId(materialId);
        vo.setSourceUrl(breakdown.getSourceUrl());
        vo.setBreakdownStatus(MaterialBreakdownSupport.resolveStatus(breakdown));
        Map<String, Object> dimensions = breakdown.getDimensionsJson() != null
            ? new LinkedHashMap<>(breakdown.getDimensionsJson())
            : new LinkedHashMap<>();
        dimensions.remove("_status");
        dimensions.remove("_error");
        dimensions.remove("_needsHumanReview");
        vo.setDimensions(dimensions);
        vo.setNeedsHumanReview(MaterialBreakdownSupport.readNeedsHumanReview(breakdown.getDimensionsJson()));
        vo.setReusableStructure(breakdown.getReusableStructure());
        vo.setFrames(breakdown.getFramesJson() != null ? breakdown.getFramesJson() : new ArrayList<>());
        vo.setBreakdownCreatedAt(breakdown.getCreatedAt());
        return vo;
    }

    private MaterialAsset findMaterialBySourceUrl(Long projectId, String sourceUrl) {
        return materialAssetMapper.selectOne(
            Wrappers.lambdaQuery(MaterialAsset.class)
                .eq(MaterialAsset::getProjectId, projectId)
                .eq(MaterialAsset::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .eq(MaterialAsset::getUrl, sourceUrl)
                .isNull(MaterialAsset::getDeletedAt)
                .orderByDesc(MaterialAsset::getId)
                .last("LIMIT 1")
        );
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

    private MaterialAsset getOwnedMaterialOrThrow(Long projectId, Long materialId) {
        assertProjectOwned(projectId);
        MaterialAsset material = materialAssetMapper.selectOne(
            Wrappers.lambdaQuery(MaterialAsset.class)
                .eq(MaterialAsset::getId, materialId)
                .eq(MaterialAsset::getProjectId, projectId)
                .eq(MaterialAsset::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(MaterialAsset::getDeletedAt)
        );
        if (material == null) {
            throw new ServiceException("素材不存在", HttpStatus.NOT_FOUND);
        }
        return material;
    }

    private VideoBreakdown getOwnedBreakdownOrThrow(Long projectId, Long breakdownId) {
        assertProjectOwned(projectId);
        VideoBreakdown breakdown = videoBreakdownMapper.selectOne(
            Wrappers.lambdaQuery(VideoBreakdown.class)
                .eq(VideoBreakdown::getId, breakdownId)
                .eq(VideoBreakdown::getProjectId, projectId)
                .eq(VideoBreakdown::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(VideoBreakdown::getDeletedAt)
        );
        if (breakdown == null) {
            throw new ServiceException("拆解记录不存在", HttpStatus.NOT_FOUND);
        }
        return breakdown;
    }
}
