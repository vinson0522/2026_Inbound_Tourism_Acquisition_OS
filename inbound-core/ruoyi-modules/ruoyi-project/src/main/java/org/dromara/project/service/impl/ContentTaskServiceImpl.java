package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.aiclient.client.AiServiceClient;
import org.dromara.aiclient.model.AiApiResponse;
import org.dromara.aiclient.model.ContentGenerateData;
import org.dromara.aiclient.model.ContentGenerateRequest;
import org.dromara.aiclient.model.StoryboardScene;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.project.billing.QuotaType;
import org.dromara.project.domain.ContentTask;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.domain.GeneratedContent;
import org.dromara.project.domain.KeywordOpportunity;
import org.dromara.project.domain.bo.ContentGenerateBo;
import org.dromara.project.domain.bo.ContentTaskBo;
import org.dromara.project.domain.vo.ContentGenerateVo;
import org.dromara.project.domain.vo.ContentTaskDetailVo;
import org.dromara.project.domain.vo.ContentTaskVo;
import org.dromara.project.domain.vo.GeneratedContentVo;
import org.dromara.project.mapper.ContentTaskMapper;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.dromara.project.mapper.GeneratedContentMapper;
import org.dromara.project.mapper.KeywordOpportunityMapper;
import org.dromara.project.service.IContentTaskService;
import org.dromara.project.service.IQuotaService;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentTaskServiceImpl implements IContentTaskService {

    private final ContentTaskMapper contentTaskMapper;
    private final GeneratedContentMapper generatedContentMapper;
    private final CustomerProjectMapper customerProjectMapper;
    private final KeywordOpportunityMapper keywordOpportunityMapper;
    private final AiServiceClient aiServiceClient;
    private final IQuotaService quotaService;

    @Override
    public TableDataInfo<ContentTaskVo> queryPageList(Long projectId, ContentTaskBo bo, PageQuery pageQuery) {
        assertProjectOwned(projectId);
        LambdaQueryWrapper<ContentTask> lqw = Wrappers.lambdaQuery(ContentTask.class)
            .eq(ContentTask::getProjectId, projectId)
            .eq(ContentTask::getTenantId, BusinessTenantHelper.getBusinessTenantId())
            .isNull(ContentTask::getDeletedAt)
            .eq(StringUtils.isNotBlank(bo.getStatus()), ContentTask::getStatus, bo.getStatus())
            .eq(StringUtils.isNotBlank(bo.getPlatform()), ContentTask::getPlatform, bo.getPlatform())
            .orderByDesc(ContentTask::getCreatedAt);
        Page<ContentTaskVo> page = contentTaskMapper.selectVoPage(pageQuery.build(), lqw);
        enrichListRows(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public ContentTaskDetailVo queryById(Long projectId, Long taskId) {
        ContentTask task = getOwnedTaskOrThrow(projectId, taskId);
        ContentTaskDetailVo detail = new ContentTaskDetailVo();
        ContentTaskVo base = MapstructUtils.convert(task, ContentTaskVo.class);
        org.springframework.beans.BeanUtils.copyProperties(base, detail);
        if (task.getKeywordId() != null) {
            KeywordOpportunity keyword = getOwnedKeywordOrNull(projectId, task.getKeywordId());
            if (keyword != null) {
                detail.setKeywordText(keyword.getKeyword());
            }
        }
        GeneratedContent latest = findLatestContent(taskId);
        if (latest != null) {
            detail.setGeneratedContent(MapstructUtils.convert(latest, GeneratedContentVo.class));
            detail.setContentTitle(latest.getTitle());
            detail.setContentVersion(latest.getVersion());
            detail.setNeedsHumanReview(latest.getNeedsHumanReview());
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(Long projectId, ContentTaskBo bo) {
        CustomerProject project = getOwnedProjectOrThrow(projectId);
        KeywordOpportunity keyword = getOwnedKeywordOrThrow(projectId, bo.getKeywordId());

        OffsetDateTime now = OffsetDateTime.now();
        ContentTask entity = MapstructUtils.convert(bo, ContentTask.class);
        entity.setProjectId(projectId);
        entity.setTenantId(BusinessTenantHelper.getBusinessTenantId());
        entity.setFormat(StringUtils.blankToDefault(entity.getFormat(), "short_video"));
        entity.setLanguage(StringUtils.blankToDefault(entity.getLanguage(), "en"));
        entity.setTone(StringUtils.blankToDefault(entity.getTone(), "friendly"));
        entity.setDuration(entity.getDuration() != null ? entity.getDuration() : 30);
        entity.setTargetMarket(
            StringUtils.blankToDefault(entity.getTargetMarket(), defaultMarket(project, keyword))
        );
        entity.setStatus("DRAFT");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(LoginHelper.getUserId());
        contentTaskMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean deleteById(Long projectId, Long taskId) {
        getOwnedTaskOrThrow(projectId, taskId);
        OffsetDateTime now = OffsetDateTime.now();
        return contentTaskMapper.update(
            null,
            Wrappers.lambdaUpdate(ContentTask.class)
                .set(ContentTask::getDeletedAt, now)
                .set(ContentTask::getUpdatedAt, now)
                .eq(ContentTask::getId, taskId)
                .eq(ContentTask::getProjectId, projectId)
                .eq(ContentTask::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(ContentTask::getDeletedAt)
        ) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentGenerateVo generate(Long projectId, Long taskId, ContentGenerateBo bo) {
        ContentTask task = getOwnedTaskOrThrow(projectId, taskId);
        if (task.getKeywordId() == null) {
            throw new ServiceException("内容任务未关联关键词，无法生成脚本");
        }
        KeywordOpportunity keyword = getOwnedKeywordOrThrow(projectId, task.getKeywordId());
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        quotaService.checkAndConsume(tenantId, QuotaType.CONTENT_PER_MONTH, 1);

        updateTaskStatus(taskId, "GENERATING");

        ContentGenerateRequest aiReq = new ContentGenerateRequest();
        aiReq.setTenantId(tenantId);
        aiReq.setProjectId(projectId);
        aiReq.setKeywordId(task.getKeywordId());
        aiReq.setKeywordText(StringUtils.blankToDefault(keyword.getKeywordEn(), keyword.getKeyword()));
        aiReq.setPlatform(task.getPlatform());
        aiReq.setDurationSec(task.getDuration() != null ? task.getDuration() : 30);
        aiReq.setTone(StringUtils.blankToDefault(task.getTone(), "friendly"));
        aiReq.setLanguage(StringUtils.blankToDefault(task.getLanguage(), "en"));
        aiReq.setTargetMarket(StringUtils.blankToDefault(task.getTargetMarket(), keyword.getMarket()));
        aiReq.setUseRag(bo != null && bo.getUseRag() != null ? bo.getUseRag() : Boolean.TRUE);
        aiReq.setTraceId(StringUtils.blankToDefault(MDC.get("traceId"), UUID.randomUUID().toString()));

        AiApiResponse<ContentGenerateData> aiResp;
        try {
            aiResp = aiServiceClient.contentGenerate(aiReq);
        } catch (IllegalStateException ex) {
            markTaskFailed(taskId);
            throw new ServiceException("AI 内容生成失败: " + ex.getMessage());
        }
        if (aiResp == null || aiResp.getCode() == null || aiResp.getCode() != 0 || aiResp.getData() == null) {
            markTaskFailed(taskId);
            String msg = aiResp != null ? aiResp.getMessage() : "empty response";
            throw new ServiceException("AI 内容生成失败: " + msg);
        }

        ContentGenerateData data = aiResp.getData();
        GeneratedContent content = persistGeneratedContent(task, tenantId, data);

        updateTaskStatus(taskId, "DRAFT");

        ContentGenerateVo vo = new ContentGenerateVo();
        vo.setContentId(content.getId());
        vo.setVersion(content.getVersion());
        vo.setNeedsHumanReview(
            Boolean.TRUE.equals(content.getNeedsHumanReview()) || content.getNeedsHumanReview() == null
        );
        vo.setCaptureMethod(data.getCaptureMethod());
        return vo;
    }

    private GeneratedContent persistGeneratedContent(
        ContentTask task,
        Long tenantId,
        ContentGenerateData data
    ) {
        if (StringUtils.isBlank(data.getHook()) || StringUtils.isBlank(data.getScript())) {
            markTaskFailed(task.getId());
            throw new ServiceException("AI 返回脚本不完整");
        }
        int nextVersion = nextContentVersion(task.getId());
        OffsetDateTime now = OffsetDateTime.now();
        GeneratedContent entity = new GeneratedContent();
        entity.setTenantId(tenantId);
        entity.setTaskId(task.getId());
        entity.setTitle(data.getTitle());
        entity.setHook(data.getHook());
        entity.setTargetAudience(data.getTargetAudience());
        entity.setScript(data.getScript());
        entity.setStoryboardJson(toStoryboardMaps(data.getStoryboardJson()));
        entity.setVoiceover(data.getVoiceover());
        entity.setOnScreenText(data.getOnScreenText());
        entity.setHashtags(data.getHashtags());
        entity.setCta(data.getCta());
        entity.setLandingPageSuggestion(data.getLandingPageSuggestion());
        entity.setNeedsHumanReview(
            data.getNeedsHumanReview() != null ? data.getNeedsHumanReview() : Boolean.TRUE
        );
        entity.setVersion(nextVersion);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(LoginHelper.getUserId());
        generatedContentMapper.insert(entity);
        return entity;
    }

    private int nextContentVersion(Long taskId) {
        GeneratedContent latest = findLatestContent(taskId);
        return latest != null && latest.getVersion() != null ? latest.getVersion() + 1 : 1;
    }

    private GeneratedContent findLatestContent(Long taskId) {
        return generatedContentMapper.selectOne(
            Wrappers.lambdaQuery(GeneratedContent.class)
                .eq(GeneratedContent::getTaskId, taskId)
                .eq(GeneratedContent::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(GeneratedContent::getDeletedAt)
                .orderByDesc(GeneratedContent::getVersion)
                .last("LIMIT 1")
        );
    }

    private List<Map<String, Object>> toStoryboardMaps(List<StoryboardScene> scenes) {
        if (scenes == null || scenes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> rows = new ArrayList<>(scenes.size());
        for (StoryboardScene scene : scenes) {
            Map<String, Object> row = new HashMap<>();
            row.put("scene", scene.getScene());
            row.put("duration", scene.getDuration());
            row.put("visual", scene.getVisual());
            if (scene.getNote() != null) {
                row.put("note", scene.getNote());
            }
            rows.add(row);
        }
        return rows;
    }

    private void enrichListRows(List<ContentTaskVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<Long> keywordIds = rows.stream()
            .map(ContentTaskVo::getKeywordId)
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

        Set<Long> taskIds = rows.stream().map(ContentTaskVo::getId).collect(Collectors.toSet());
        Map<Long, GeneratedContent> latestByTask = loadLatestContentByTaskIds(taskIds);

        for (ContentTaskVo row : rows) {
            if (row.getKeywordId() != null) {
                KeywordOpportunity kw = keywordById.get(row.getKeywordId());
                if (kw != null) {
                    row.setKeywordText(kw.getKeyword());
                }
            }
            GeneratedContent latest = latestByTask.get(row.getId());
            if (latest != null) {
                row.setContentTitle(latest.getTitle());
                row.setContentVersion(latest.getVersion());
                row.setNeedsHumanReview(latest.getNeedsHumanReview());
            }
        }
    }

    private Map<Long, GeneratedContent> loadLatestContentByTaskIds(Set<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<GeneratedContent> all = generatedContentMapper.selectList(
            Wrappers.lambdaQuery(GeneratedContent.class)
                .in(GeneratedContent::getTaskId, taskIds)
                .eq(GeneratedContent::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(GeneratedContent::getDeletedAt)
                .orderByDesc(GeneratedContent::getVersion)
        );
        Map<Long, GeneratedContent> latest = new HashMap<>();
        for (GeneratedContent item : all) {
            latest.putIfAbsent(item.getTaskId(), item);
        }
        return latest;
    }

    private void updateTaskStatus(Long taskId, String status) {
        ContentTask patch = new ContentTask();
        patch.setId(taskId);
        patch.setStatus(status);
        patch.setUpdatedAt(OffsetDateTime.now());
        contentTaskMapper.updateById(patch);
    }

    private void markTaskFailed(Long taskId) {
        updateTaskStatus(taskId, "FAILED");
    }

    private String defaultMarket(CustomerProject project, KeywordOpportunity keyword) {
        if (StringUtils.isNotBlank(keyword.getMarket())) {
            return keyword.getMarket();
        }
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

    private ContentTask getOwnedTaskOrThrow(Long projectId, Long taskId) {
        assertProjectOwned(projectId);
        ContentTask entity = contentTaskMapper.selectOne(
            Wrappers.lambdaQuery(ContentTask.class)
                .eq(ContentTask::getId, taskId)
                .eq(ContentTask::getProjectId, projectId)
                .eq(ContentTask::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(ContentTask::getDeletedAt)
        );
        if (entity == null) {
            throw new ServiceException("内容任务不存在");
        }
        return entity;
    }
}
