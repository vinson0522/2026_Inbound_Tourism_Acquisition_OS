package org.dromara.diagnostic.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.diagnostic.domain.DiagnosticRun;
import org.dromara.diagnostic.domain.DiagnosticSchedule;
import org.dromara.diagnostic.domain.bo.CreateDiagnosticBo;
import org.dromara.diagnostic.domain.bo.DiagnosticScheduleUpsertBo;
import org.dromara.diagnostic.domain.vo.DiagnosticScheduleVo;
import org.dromara.diagnostic.mapper.DiagnosticRunMapper;
import org.dromara.diagnostic.mapper.DiagnosticScheduleMapper;
import org.dromara.diagnostic.service.IDiagnosticRunService;
import org.dromara.diagnostic.service.IDiagnosticScheduleService;
import org.dromara.common.tenant.helper.BusinessTenantHelper;
import org.dromara.project.billing.QuotaType;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.dromara.project.service.IQuotaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosticScheduleServiceImpl implements IDiagnosticScheduleService {

    private static final String FREQUENCY_WEEKLY = "WEEKLY";
    private static final String FREQUENCY_MONTHLY = "MONTHLY";
    private static final DateTimeFormatter RUN_NAME_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DiagnosticScheduleMapper diagnosticScheduleMapper;
    private final DiagnosticRunMapper diagnosticRunMapper;
    private final CustomerProjectMapper customerProjectMapper;
    private final IDiagnosticRunService diagnosticRunService;
    private final IQuotaService quotaService;

    @Override
    public DiagnosticScheduleVo getByProject(Long projectId) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        assertProjectOwned(projectId, tenantId);
        DiagnosticSchedule schedule = findByProjectId(projectId);
        if (schedule == null) {
            return defaultVo(projectId);
        }
        return toVo(schedule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiagnosticScheduleVo upsert(Long projectId, DiagnosticScheduleUpsertBo bo) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        assertProjectOwned(projectId, tenantId);
        validateFrequency(bo.getFrequency());

        OffsetDateTime now = OffsetDateTime.now();
        DiagnosticSchedule existing = findByProjectId(projectId);
        boolean wasEnabled = existing != null && Boolean.TRUE.equals(existing.getEnabled());
        boolean enabling = Boolean.TRUE.equals(bo.getEnabled());
        boolean frequencyChanged = existing != null && !Objects.equals(existing.getFrequency(), bo.getFrequency());

        Map<String, Object> questionScope = buildQuestionScopeMap(bo);
        DiagnosticSchedule entity = existing != null ? existing : new DiagnosticSchedule();
        entity.setTenantId(tenantId);
        entity.setProjectId(projectId);
        entity.setFrequency(bo.getFrequency().toUpperCase());
        entity.setEnabled(bo.getEnabled());
        entity.setMarket(bo.getMarket());
        entity.setLocale(bo.getLocale());
        entity.setRegion(bo.getRegion());
        entity.setProbeModes(bo.getProbeModes());
        entity.setModels(bo.getModels());
        entity.setSampleCount(bo.getSampleCount());
        entity.setQuestionScope(questionScope);
        entity.setCalibrationRatio(bo.getCalibrationRatio() == null ? BigDecimal.ZERO : bo.getCalibrationRatio());
        entity.setUpdatedAt(now);

        if (existing == null) {
            entity.setCreatedAt(now);
            if (enabling) {
                entity.setNextRunAt(now);
            }
            diagnosticScheduleMapper.insert(entity);
        } else {
            if (enabling && (!wasEnabled || frequencyChanged || entity.getNextRunAt() == null)) {
                entity.setNextRunAt(now);
            }
            diagnosticScheduleMapper.updateById(entity);
        }
        return toVo(entity);
    }

    @Override
    public void processDueSchedules() {
        OffsetDateTime now = OffsetDateTime.now();
        List<DiagnosticSchedule> due = diagnosticScheduleMapper.selectList(
            Wrappers.lambdaQuery(DiagnosticSchedule.class)
                .eq(DiagnosticSchedule::getEnabled, true)
                .isNull(DiagnosticSchedule::getDeletedAt)
                .isNotNull(DiagnosticSchedule::getNextRunAt)
                .le(DiagnosticSchedule::getNextRunAt, now)
        );
        if (due.isEmpty()) {
            return;
        }
        log.info("diagnostic_schedule_job due_count={}", due.size());
        for (DiagnosticSchedule schedule : due) {
            try {
                triggerSchedule(schedule, false);
            } catch (Exception e) {
                log.error("diagnostic_schedule_job failed scheduleId={} projectId={}",
                    schedule.getId(), schedule.getProjectId(), e);
            }
        }
    }

    @Override
    public Long triggerForProject(Long projectId, boolean force) {
        DiagnosticSchedule schedule = findByProjectId(projectId);
        if (schedule == null || schedule.getDeletedAt() != null) {
            throw new ServiceException("定时计划不存在", HttpStatus.NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(schedule.getEnabled())) {
            log.warn("diagnostic_schedule trigger skipped disabled projectId={}", projectId);
            return null;
        }
        return triggerSchedule(schedule, force);
    }

    private Long triggerSchedule(DiagnosticSchedule schedule, boolean force) {
        if (!force) {
            OffsetDateTime nextRunAt = schedule.getNextRunAt();
            if (nextRunAt == null || nextRunAt.isAfter(OffsetDateTime.now())) {
                return null;
            }
        }
        Long tenantId = schedule.getTenantId();
        Long projectId = schedule.getProjectId();
        if (!quotaService.hasRemainingQuota(tenantId, QuotaType.DIAGNOSTICS_PER_MONTH, 1)) {
            log.warn("diagnostic_schedule skipped quota exceeded projectId={} tenantId={} scheduleId={}",
                projectId, tenantId, schedule.getId());
            return null;
        }

        CustomerProject project = customerProjectMapper.selectById(projectId);
        if (project == null || project.getDeletedAt() != null) {
            log.warn("diagnostic_schedule skipped missing project projectId={}", projectId);
            return null;
        }

        CreateDiagnosticBo bo = toCreateBo(schedule, project);
        Long runId = diagnosticRunService.createRunForSchedule(projectId, tenantId, bo);

        OffsetDateTime now = OffsetDateTime.now();
        schedule.setLastRunId(runId);
        schedule.setLastTriggeredAt(now);
        schedule.setNextRunAt(computeNextRunAt(schedule.getFrequency(), now));
        schedule.setUpdatedAt(now);
        diagnosticScheduleMapper.updateById(schedule);
        log.info("diagnostic_schedule triggered projectId={} runId={} nextRunAt={}",
            projectId, runId, schedule.getNextRunAt());
        return runId;
    }

    static OffsetDateTime computeNextRunAt(String frequency, OffsetDateTime from) {
        if (FREQUENCY_MONTHLY.equalsIgnoreCase(frequency)) {
            return from.plusMonths(1);
        }
        return from.plusWeeks(1);
    }

    private CreateDiagnosticBo toCreateBo(DiagnosticSchedule schedule, CustomerProject project) {
        CreateDiagnosticBo bo = new CreateDiagnosticBo();
        String date = LocalDate.now().format(RUN_NAME_DATE);
        bo.setName(project.getName() + "-定时-" + date);
        bo.setMarket(schedule.getMarket());
        bo.setLocale(schedule.getLocale());
        bo.setRegion(schedule.getRegion());
        bo.setProbeModes(schedule.getProbeModes());
        bo.setModels(schedule.getModels());
        bo.setSampleCount(schedule.getSampleCount());
        bo.setCalibrationRatio(schedule.getCalibrationRatio());
        applyQuestionScopeToBo(bo, schedule.getQuestionScope());
        return bo;
    }

    @SuppressWarnings("unchecked")
    private void applyQuestionScopeToBo(CreateDiagnosticBo bo, Map<String, Object> scope) {
        if (scope == null || scope.isEmpty()) {
            bo.setQuestionScope("all");
            return;
        }
        if (scope.containsKey("questionIds")) {
            Object raw = scope.get("questionIds");
            if (raw instanceof List<?> list) {
                bo.setQuestionIds(list.stream().map(v -> Long.valueOf(String.valueOf(v))).toList());
            }
            return;
        }
        if (scope.containsKey("stage")) {
            bo.setQuestionStage(String.valueOf(scope.get("stage")));
            return;
        }
        if ("all".equals(String.valueOf(scope.get("mode")))) {
            bo.setQuestionScope("all");
            return;
        }
        bo.setQuestionScope(JsonUtils.toJsonString(scope));
    }

    private Map<String, Object> buildQuestionScopeMap(DiagnosticScheduleUpsertBo bo) {
        Map<String, Object> scope = new HashMap<>();
        if (bo.getQuestionIds() != null && !bo.getQuestionIds().isEmpty()) {
            scope.put("questionIds", bo.getQuestionIds());
            return scope;
        }
        if (StringUtils.isNotBlank(bo.getQuestionStage())) {
            scope.put("stage", bo.getQuestionStage());
            return scope;
        }
        if (StringUtils.isNotBlank(bo.getQuestionScope())) {
            String trimmed = bo.getQuestionScope().trim();
            if (trimmed.startsWith("{")) {
                Map<String, Object> parsed = JsonUtils.parseMap(trimmed);
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            }
            if ("all".equalsIgnoreCase(trimmed)) {
                scope.put("mode", "all");
                return scope;
            }
            scope.put("stage", trimmed);
            return scope;
        }
        scope.put("mode", "all");
        return scope;
    }

    private DiagnosticScheduleVo toVo(DiagnosticSchedule schedule) {
        DiagnosticScheduleVo vo = new DiagnosticScheduleVo();
        vo.setId(schedule.getId());
        vo.setProjectId(schedule.getProjectId());
        vo.setConfigured(true);
        vo.setEnabled(schedule.getEnabled());
        vo.setFrequency(schedule.getFrequency());
        vo.setMarket(schedule.getMarket());
        vo.setLocale(schedule.getLocale());
        vo.setRegion(schedule.getRegion());
        vo.setProbeModes(schedule.getProbeModes());
        vo.setModels(schedule.getModels());
        vo.setSampleCount(schedule.getSampleCount());
        vo.setCalibrationRatio(schedule.getCalibrationRatio());
        vo.setQuestionScope(resolveQuestionScopeLabel(schedule.getQuestionScope()));
        vo.setNextRunAt(schedule.getNextRunAt());
        vo.setLastTriggeredAt(schedule.getLastTriggeredAt());
        vo.setLastRunId(schedule.getLastRunId());
        if (schedule.getLastRunId() != null) {
            DiagnosticRun run = diagnosticRunMapper.selectById(schedule.getLastRunId());
            if (run != null) {
                vo.setLastRunName(run.getName());
            }
        }
        return vo;
    }

    private String resolveQuestionScopeLabel(Map<String, Object> scope) {
        if (scope == null || scope.isEmpty()) {
            return "all";
        }
        if (scope.containsKey("questionIds")) {
            return "custom";
        }
        if (scope.containsKey("stage")) {
            return String.valueOf(scope.get("stage"));
        }
        return "all";
    }

    private DiagnosticScheduleVo defaultVo(Long projectId) {
        DiagnosticScheduleVo vo = new DiagnosticScheduleVo();
        vo.setProjectId(projectId);
        vo.setConfigured(false);
        vo.setEnabled(false);
        vo.setFrequency(FREQUENCY_WEEKLY);
        vo.setMarket("US");
        vo.setLocale("en-US");
        vo.setProbeModes(List.of("grounded-api"));
        vo.setModels(List.of("gemini"));
        vo.setSampleCount(3);
        vo.setCalibrationRatio(BigDecimal.ZERO);
        vo.setQuestionScope("all");
        return vo;
    }

    private DiagnosticSchedule findByProjectId(Long projectId) {
        return diagnosticScheduleMapper.selectOne(
            Wrappers.lambdaQuery(DiagnosticSchedule.class)
                .eq(DiagnosticSchedule::getProjectId, projectId)
                .isNull(DiagnosticSchedule::getDeletedAt)
                .last("LIMIT 1")
        );
    }

    private void assertProjectOwned(Long projectId, Long tenantId) {
        CustomerProject project = customerProjectMapper.selectById(projectId);
        if (project == null || project.getDeletedAt() != null) {
            throw new ServiceException("项目不存在", HttpStatus.NOT_FOUND);
        }
        if (!tenantId.equals(project.getTenantId())) {
            throw new ServiceException("无权访问该项目", HttpStatus.FORBIDDEN);
        }
    }

    private void validateFrequency(String frequency) {
        if (StringUtils.isBlank(frequency)) {
            throw new ServiceException("执行频率不能为空", HttpStatus.BAD_REQUEST);
        }
        String normalized = frequency.toUpperCase();
        if (!FREQUENCY_WEEKLY.equals(normalized) && !FREQUENCY_MONTHLY.equals(normalized)) {
            throw new ServiceException("不支持的执行频率: " + frequency, HttpStatus.BAD_REQUEST);
        }
    }
}
