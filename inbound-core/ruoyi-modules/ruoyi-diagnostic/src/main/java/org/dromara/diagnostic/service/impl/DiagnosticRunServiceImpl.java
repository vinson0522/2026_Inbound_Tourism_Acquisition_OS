package org.dromara.diagnostic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.aiclient.client.AiServiceClient;
import org.dromara.aiclient.model.AiApiResponse;
import org.dromara.aiclient.model.ResultMetricSummary;
import org.dromara.aiclient.model.ScoreRequest;
import org.dromara.aiclient.model.ScoreResultData;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.diagnostic.domain.DiagnosticResult;
import org.dromara.diagnostic.domain.DiagnosticRun;
import org.dromara.diagnostic.domain.ProbeNode;
import org.dromara.diagnostic.domain.ProbeTask;
import org.dromara.diagnostic.domain.QuestionBank;
import org.dromara.diagnostic.domain.bo.CreateDiagnosticBo;
import org.dromara.diagnostic.domain.bo.ProbeCallbackBo;
import org.dromara.diagnostic.domain.vo.DiagnosticCalibrationPairVo;
import org.dromara.diagnostic.domain.vo.DiagnosticCalibrationSideVo;
import org.dromara.diagnostic.domain.vo.DiagnosticCalibrationVo;
import org.dromara.diagnostic.domain.vo.DiagnosticResultVo;
import org.dromara.diagnostic.domain.vo.DiagnosticRunVo;
import org.dromara.diagnostic.domain.vo.DiagnosticTrendMetricsVo;
import org.dromara.diagnostic.domain.vo.DiagnosticTrendVo;
import org.dromara.diagnostic.domain.vo.DiagnosticTrendsVo;
import org.dromara.diagnostic.domain.vo.ProbeTaskVo;
import org.dromara.diagnostic.mapper.DiagnosticResultMapper;
import org.dromara.diagnostic.mapper.DiagnosticRunMapper;
import org.dromara.diagnostic.mapper.ProbeNodeMapper;
import org.dromara.diagnostic.mapper.ProbeTaskMapper;
import org.dromara.diagnostic.mapper.QuestionBankMapper;
import org.dromara.diagnostic.mq.DiagGroundedApiPublisher;
import org.dromara.diagnostic.service.IDiagnosticRunService;
import org.dromara.diagnostic.support.BusinessTenantHelper;
import org.dromara.diagnostic.support.DiagnosticCalibrationSupport;
import org.dromara.diagnostic.support.DiagnosticMetricsAggregator;
import org.dromara.diagnostic.support.PlatformModelResolver;
import org.dromara.diagnostic.support.PlatformModelResolver.PlatformModel;
import org.dromara.project.domain.Competitor;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.billing.QuotaType;
import org.dromara.project.mapper.CompetitorMapper;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.dromara.project.service.IQuotaService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosticRunServiceImpl implements IDiagnosticRunService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_PARTIAL_FAILED = "PARTIAL_FAILED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String PROBE_DISPATCHED = "DISPATCHED";
    private static final String PROBE_SUCCESS = "SUCCESS";
    private static final String PROBE_FAILED = "FAILED";
    private static final String PROBE_MODE_GROUNDED = "grounded-api";
    private static final String PROBE_MODE_EXTENSION = "browser-extension";

    private final DiagnosticRunMapper diagnosticRunMapper;
    private final DiagnosticResultMapper diagnosticResultMapper;
    private final ProbeTaskMapper probeTaskMapper;
    private final ProbeNodeMapper probeNodeMapper;
    private final QuestionBankMapper questionBankMapper;
    private final CompetitorMapper competitorMapper;
    private final CustomerProjectMapper customerProjectMapper;
    private final AiServiceClient aiServiceClient;
    private final DiagGroundedApiPublisher groundedApiPublisher;
    private final ApplicationContext applicationContext;
    private final IQuotaService quotaService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRun(Long projectId, CreateDiagnosticBo bo) {
        return createRunInternal(projectId, BusinessTenantHelper.getBusinessTenantId(), LoginHelper.getUserId(), bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRunForSchedule(Long projectId, Long tenantId, CreateDiagnosticBo bo) {
        return createRunInternal(projectId, tenantId, null, bo);
    }

    private Long createRunInternal(Long projectId, Long tenantId, Long userId, CreateDiagnosticBo bo) {
        CustomerProject project = getOwnedProjectOrThrow(projectId, tenantId);
        quotaService.checkAndConsume(tenantId, QuotaType.DIAGNOSTICS_PER_MONTH, 1);

        List<String> probeModes = normalizeProbeModes(bo.getProbeModes());
        List<String> modelNames = normalizeModels(bo.getModels());
        int sampleCount = bo.getSampleCount() == null || bo.getSampleCount() < 1 ? 1 : bo.getSampleCount();
        String locale = StringUtils.blankToDefault(bo.getLocale(), "en-US");

        List<QuestionBank> questions = resolveQuestions(projectId, tenantId, bo);
        if (questions.isEmpty()) {
            throw new ServiceException("未找到可用诊断问题，请先配置题库", HttpStatus.BAD_REQUEST);
        }

        List<PlatformModel> platforms = PlatformModelResolver.resolveModels(modelNames);
        OffsetDateTime now = OffsetDateTime.now();

        BigDecimal calibrationRatio = bo.getCalibrationRatio() == null ? BigDecimal.ZERO : bo.getCalibrationRatio();
        boolean dualModeCalibration = calibrationRatio.compareTo(BigDecimal.ZERO) > 0
            && probeModes.contains(PROBE_MODE_GROUNDED)
            && probeModes.contains(PROBE_MODE_EXTENSION);
        Set<Long> calibrationQuestionIds = resolveCalibrationQuestionIds(questions, dualModeCalibration, calibrationRatio);

        Map<String, Object> questionScope = buildQuestionScopeMap(bo);
        if (!calibrationQuestionIds.isEmpty()) {
            questionScope.put("calibrationQuestionIds", new ArrayList<>(calibrationQuestionIds));
        }

        DiagnosticRun run = new DiagnosticRun();
        run.setTenantId(tenantId);
        run.setProjectId(projectId);
        run.setName(bo.getName());
        run.setMarket(bo.getMarket());
        run.setLocale(locale);
        run.setRegion(bo.getRegion());
        run.setProbeModes(probeModes);
        run.setCalibrationRatio(calibrationRatio);
        run.setModels(modelNames);
        run.setSampleCount(sampleCount);
        run.setQuestionScope(questionScope);
        run.setStatus(STATUS_PENDING);
        run.setCreatedAt(now);
        run.setUpdatedAt(now);
        run.setCreatedBy(userId);
        diagnosticRunMapper.insert(run);

        List<String> competitorNames = loadCompetitorNames(projectId, tenantId);
        List<ProbeTaskDispatch> dispatches = new ArrayList<>();

        for (String probeMode : probeModes) {
            for (QuestionBank question : questions) {
                if (dualModeCalibration
                    && PROBE_MODE_EXTENSION.equals(probeMode)
                    && !calibrationQuestionIds.contains(question.getId())) {
                    continue;
                }
                for (PlatformModel platformModel : platforms) {
                    int effectiveSampleCount = sampleCount;
                    if (dualModeCalibration && calibrationQuestionIds.contains(question.getId())) {
                        effectiveSampleCount = 1;
                    }
                    for (int sampleIndex = 0; sampleIndex < effectiveSampleCount; sampleIndex++) {
                        ProbeTask task = new ProbeTask();
                        task.setTenantId(tenantId);
                        task.setRunId(run.getId());
                        task.setQuestionId(question.getId());
                        task.setPlatform(platformModel.platform());
                        task.setProbeMode(probeMode);
                        task.setStatus(STATUS_PENDING);
                        task.setRetryCount(0);
                        task.setCreatedAt(now);
                        task.setUpdatedAt(now);
                        task.setCreatedBy(userId);
                        probeTaskMapper.insert(task);
                        if (PROBE_MODE_GROUNDED.equals(probeMode)) {
                            dispatches.add(new ProbeTaskDispatch(task, question, platformModel, sampleIndex));
                        }
                    }
                }
            }
        }

        run.setStatus(STATUS_RUNNING);
        run.setStartedAt(now);
        run.setUpdatedAt(now);
        diagnosticRunMapper.updateById(run);

        if (!dispatches.isEmpty()) {
            String traceId = UUID.randomUUID().toString();
            DispatchBatch batch = new DispatchBatch(
                traceId,
                tenantId,
                projectId,
                bo.getRegion(),
                locale,
                project.getBrandName(),
                competitorNames,
                List.copyOf(dispatches)
            );
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    applicationContext.getBean(DiagnosticRunServiceImpl.class).publishDispatchBatch(batch);
                }
            });
        }

        return run.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void publishDispatchBatch(DispatchBatch batch) {
        OffsetDateTime dispatchTime = OffsetDateTime.now();
        for (ProbeTaskDispatch dispatch : batch.dispatches()) {
            ProbeTask task = dispatch.task();
            QuestionBank question = dispatch.question();
            PlatformModel platformModel = dispatch.platformModel();
            groundedApiPublisher.publish(
                batch.traceId(),
                task.getRunId(),
                task.getId(),
                question.getId(),
                batch.tenantId(),
                batch.projectId(),
                platformModel.platform(),
                PROBE_MODE_GROUNDED,
                platformModel.model(),
                batch.region(),
                batch.locale(),
                dispatch.sampleIndex(),
                question.getQuestion(),
                batch.customerBrand(),
                batch.competitorNames()
            );
            task.setStatus(PROBE_DISPATCHED);
            task.setDispatchedAt(dispatchTime);
            task.setUpdatedAt(dispatchTime);
            probeTaskMapper.updateById(task);
        }
    }

    private record DispatchBatch(
        String traceId,
        Long tenantId,
        Long projectId,
        String region,
        String locale,
        String customerBrand,
        List<String> competitorNames,
        List<ProbeTaskDispatch> dispatches
    ) {
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleProbeCallback(ProbeCallbackBo bo) {
        ProbeTask task = probeTaskMapper.selectById(bo.getProbeTaskId());
        if (task == null || task.getDeletedAt() != null) {
            throw new ServiceException("探针任务不存在", HttpStatus.NOT_FOUND);
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (PROBE_SUCCESS.equalsIgnoreCase(bo.getStatus())) {
            insertDiagnosticResult(task, bo.getResult(), now);
            task.setStatus(PROBE_SUCCESS);
            task.setErrorMessage(null);
        } else if (PROBE_FAILED.equalsIgnoreCase(bo.getStatus())) {
            task.setStatus(PROBE_FAILED);
            task.setErrorMessage(StringUtils.blankToDefault(bo.getErrorMessage(), "probe failed"));
        } else {
            throw new ServiceException("不支持的探针状态: " + bo.getStatus(), HttpStatus.BAD_REQUEST);
        }
        task.setFinishedAt(now);
        task.setUpdatedAt(now);
        probeTaskMapper.updateById(task);

        tryFinalizeRun(task.getRunId());
    }

    @Override
    public TableDataInfo<DiagnosticRunVo> queryPageList(Long projectId, PageQuery pageQuery) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        getOwnedProjectOrThrow(projectId, tenantId);

        LambdaQueryWrapper<DiagnosticRun> lqw = Wrappers.lambdaQuery();
        lqw.eq(DiagnosticRun::getTenantId, tenantId);
        lqw.eq(DiagnosticRun::getProjectId, projectId);
        lqw.isNull(DiagnosticRun::getDeletedAt);
        lqw.orderByDesc(DiagnosticRun::getCreatedAt);

        Page<DiagnosticRunVo> page = diagnosticRunMapper.selectVoPage(pageQuery.build(), lqw);
        page.getRecords().forEach(vo -> vo.setProgress(computeProgress(vo.getId())));
        return TableDataInfo.build(page);
    }

    @Override
    public DiagnosticRunVo queryById(Long runId) {
        DiagnosticRun run = getOwnedRunOrThrow(runId);
        DiagnosticRunVo vo = MapstructUtils.convert(run, DiagnosticRunVo.class);
        vo.setProgress(computeProgress(runId));
        return vo;
    }

    @Override
    public List<DiagnosticResultVo> queryResults(Long runId) {
        getOwnedRunOrThrow(runId);
        return diagnosticResultMapper.selectVoList(
            Wrappers.lambdaQuery(DiagnosticResult.class)
                .eq(DiagnosticResult::getRunId, runId)
                .isNull(DiagnosticResult::getDeletedAt)
                .orderByAsc(DiagnosticResult::getQuestionId)
                .orderByAsc(DiagnosticResult::getPlatform)
        );
    }

    @Override
    public DiagnosticTrendsVo queryTrends(Long projectId, int limit, String market) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        getOwnedProjectOrThrow(projectId, tenantId);

        int cappedLimit = limit <= 0 ? 12 : Math.min(limit, 52);

        LambdaQueryWrapper<DiagnosticRun> lqw = Wrappers.lambdaQuery();
        lqw.eq(DiagnosticRun::getTenantId, tenantId);
        lqw.eq(DiagnosticRun::getProjectId, projectId);
        lqw.isNull(DiagnosticRun::getDeletedAt);
        lqw.isNotNull(DiagnosticRun::getGeoScore);
        lqw.isNotNull(DiagnosticRun::getFinishedAt);
        lqw.apply("status IN ('SUCCESS'::diagnostic_run_status, 'PARTIAL_FAILED'::diagnostic_run_status)");
        if (StringUtils.isNotBlank(market)) {
            lqw.eq(DiagnosticRun::getMarket, market);
        }
        lqw.orderByDesc(DiagnosticRun::getFinishedAt);
        lqw.last("LIMIT " + cappedLimit);

        List<DiagnosticRun> runs = diagnosticRunMapper.selectList(lqw);
        Collections.reverse(runs);

        DiagnosticTrendsVo response = new DiagnosticTrendsVo();
        if (runs.isEmpty()) {
            return response;
        }

        List<Long> runIds = runs.stream().map(DiagnosticRun::getId).toList();
        List<DiagnosticResult> allResults = diagnosticResultMapper.selectList(
            Wrappers.lambdaQuery(DiagnosticResult.class)
                .in(DiagnosticResult::getRunId, runIds)
                .isNull(DiagnosticResult::getDeletedAt)
        );
        Map<Long, List<DiagnosticResult>> resultsByRun = allResults.stream()
            .collect(Collectors.groupingBy(DiagnosticResult::getRunId));
        Map<Long, Boolean> longtailMap = loadLongtailMap(allResults);

        List<DiagnosticTrendVo> trendRuns = new ArrayList<>(runs.size());
        for (DiagnosticRun run : runs) {
            List<DiagnosticResult> runResults = resultsByRun.getOrDefault(run.getId(), Collections.emptyList());
            DiagnosticTrendMetricsVo metrics = DiagnosticMetricsAggregator.aggregate(runResults, longtailMap);

            DiagnosticTrendVo point = new DiagnosticTrendVo();
            point.setRunId(run.getId());
            point.setName(run.getName());
            point.setGeoScore(run.getGeoScore());
            point.setFinishedAt(run.getFinishedAt());
            point.setMarket(run.getMarket());
            point.setStatus(run.getStatus());
            point.setMetrics(metrics);
            trendRuns.add(point);
        }
        response.setRuns(trendRuns);
        return response;
    }

    @Override
    public List<ProbeTaskVo> queryProbeTasks(Long runId) {
        getOwnedRunOrThrow(runId);
        return probeTaskMapper.selectVoList(
            Wrappers.lambdaQuery(ProbeTask.class)
                .eq(ProbeTask::getRunId, runId)
                .isNull(ProbeTask::getDeletedAt)
                .orderByAsc(ProbeTask::getId)
        );
    }

    @Override
    public DiagnosticCalibrationVo queryCalibration(Long projectId, Long runId) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        CustomerProject project = getOwnedProjectOrThrow(projectId, tenantId);
        DiagnosticRun run = getOwnedRunOrThrow(runId);
        if (!projectId.equals(run.getProjectId())) {
            throw new ServiceException("诊断任务不属于该项目", HttpStatus.NOT_FOUND);
        }

        DiagnosticCalibrationVo response = new DiagnosticCalibrationVo();
        BigDecimal ratio = run.getCalibrationRatio() == null ? BigDecimal.ZERO : run.getCalibrationRatio();
        List<String> modes = run.getProbeModes() != null ? run.getProbeModes() : List.of();
        boolean dualMode = ratio.compareTo(BigDecimal.ZERO) > 0
            && modes.contains(PROBE_MODE_GROUNDED)
            && modes.contains(PROBE_MODE_EXTENSION);
        if (!dualMode) {
            return response;
        }

        Set<Long> calibrationQuestionIds = resolveCalibrationQuestionIdsFromRun(run);
        if (calibrationQuestionIds.isEmpty()) {
            return response;
        }

        List<String> platforms = PlatformModelResolver.resolveModels(
            run.getModels() != null && !run.getModels().isEmpty() ? run.getModels() : List.of("gemini")
        ).stream().map(PlatformModel::platform).distinct().toList();
        response.setSampleCount(calibrationQuestionIds.size() * platforms.size());

        List<DiagnosticResult> results = diagnosticResultMapper.selectList(
            Wrappers.lambdaQuery(DiagnosticResult.class)
                .eq(DiagnosticResult::getRunId, runId)
                .in(DiagnosticResult::getQuestionId, calibrationQuestionIds)
                .isNull(DiagnosticResult::getDeletedAt)
        );
        if (results.isEmpty()) {
            return response;
        }

        Map<Long, QuestionBank> questionsById = questionBankMapper.selectList(
            Wrappers.lambdaQuery(QuestionBank.class)
                .in(QuestionBank::getId, calibrationQuestionIds)
                .isNull(QuestionBank::getDeletedAt)
        ).stream().collect(Collectors.toMap(QuestionBank::getId, q -> q, (a, b) -> a));

        Set<Long> probeNodeIds = results.stream()
            .map(DiagnosticResult::getProbeNodeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, ProbeNode> nodesById = probeNodeIds.isEmpty()
            ? Map.of()
            : probeNodeMapper.selectList(
                Wrappers.lambdaQuery(ProbeNode.class)
                    .in(ProbeNode::getId, probeNodeIds)
                    .isNull(ProbeNode::getDeletedAt)
            ).stream().collect(Collectors.toMap(ProbeNode::getId, n -> n, (a, b) -> a));

        List<Long> orderedQuestionIds = calibrationQuestionIds.stream().sorted().toList();
        List<DiagnosticCalibrationPairVo> pairs = new ArrayList<>();
        String brandName = project.getBrandName();

        for (Long questionId : orderedQuestionIds) {
            QuestionBank question = questionsById.get(questionId);
            for (String platform : platforms) {
                DiagnosticResult apiResult = findCalibrationResult(results, questionId, platform, PROBE_MODE_GROUNDED);
                DiagnosticResult extResult = findCalibrationResult(results, questionId, platform, PROBE_MODE_EXTENSION);
                if (apiResult == null || extResult == null) {
                    continue;
                }

                BigDecimal similarity = DiagnosticCalibrationSupport.jaccardSimilarity(
                    apiResult.getAnswerText(), extResult.getAnswerText());
                BigDecimal deviation = BigDecimal.ONE.subtract(similarity).setScale(4, RoundingMode.HALF_UP);
                boolean apiBrand = DiagnosticCalibrationSupport.mentionsBrand(apiResult.getMentionedBrands(), brandName);
                boolean extBrand = DiagnosticCalibrationSupport.mentionsBrand(extResult.getMentionedBrands(), brandName);

                DiagnosticCalibrationPairVo pair = new DiagnosticCalibrationPairVo();
                pair.setQuestionId(questionId);
                pair.setQuestion(DiagnosticCalibrationSupport.questionText(question));
                pair.setStage(DiagnosticCalibrationSupport.questionStage(question));
                pair.setPlatform(platform);
                pair.setBrandMatch(apiBrand == extBrand);
                pair.setSimilarityScore(similarity);
                pair.setDeviationScore(deviation);
                pair.setGroundedApi(toCalibrationSide(apiResult, null, brandName));
                ProbeNode node = extResult.getProbeNodeId() != null ? nodesById.get(extResult.getProbeNodeId()) : null;
                pair.setBrowserExtension(toCalibrationSide(extResult, node, brandName));
                pairs.add(pair);
            }
        }

        response.setPairs(pairs);
        response.setPairedCount(pairs.size());
        if (pairs.isEmpty()) {
            return response;
        }

        BigDecimal totalDeviation = BigDecimal.ZERO;
        int brandMatches = 0;
        for (DiagnosticCalibrationPairVo pair : pairs) {
            totalDeviation = totalDeviation.add(pair.getDeviationScore());
            if (Boolean.TRUE.equals(pair.getBrandMatch())) {
                brandMatches++;
            }
        }
        BigDecimal pairCount = BigDecimal.valueOf(pairs.size());
        response.setDeviationRate(totalDeviation.divide(pairCount, 4, RoundingMode.HALF_UP));
        response.setBrandMentionAgreementRate(
            BigDecimal.valueOf(brandMatches).divide(pairCount, 4, RoundingMode.HALF_UP)
        );
        return response;
    }

    private void tryFinalizeRun(Long runId) {
        List<ProbeTask> tasks = probeTaskMapper.selectList(
            Wrappers.lambdaQuery(ProbeTask.class)
                .eq(ProbeTask::getRunId, runId)
                .isNull(ProbeTask::getDeletedAt)
        );
        if (tasks.isEmpty()) {
            return;
        }
        boolean allTerminal = tasks.stream().allMatch(t ->
            PROBE_SUCCESS.equals(t.getStatus()) || PROBE_FAILED.equals(t.getStatus())
        );
        if (!allTerminal) {
            return;
        }

        long failCount = tasks.stream().filter(t -> PROBE_FAILED.equals(t.getStatus())).count();
        DiagnosticRun run = diagnosticRunMapper.selectById(runId);
        if (run == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        String finalStatus;
        BigDecimal geoScore = null;

        List<DiagnosticResult> results = diagnosticResultMapper.selectList(
            Wrappers.lambdaQuery(DiagnosticResult.class)
                .eq(DiagnosticResult::getRunId, runId)
                .isNull(DiagnosticResult::getDeletedAt)
        );

        if (!results.isEmpty()) {
            Map<Long, Boolean> longtailByQuestion = loadLongtailMap(results);
            ScoreRequest scoreRequest = buildScoreRequest(runId, results, longtailByQuestion);
            try {
                AiApiResponse<ScoreResultData> response = aiServiceClient.score(scoreRequest);
                if (response != null && response.getData() != null && response.getData().getGeoScore() != null) {
                    geoScore = BigDecimal.valueOf(response.getData().getGeoScore()).setScale(2, RoundingMode.HALF_UP);
                }
            } catch (Exception ex) {
                log.warn("GEO score aggregation failed for runId={}: {}", runId, ex.getMessage());
            }
        }

        if (failCount == tasks.size()) {
            finalStatus = STATUS_FAILED;
        } else if (failCount > 0) {
            finalStatus = STATUS_PARTIAL_FAILED;
        } else {
            finalStatus = STATUS_SUCCESS;
        }

        run.setGeoScore(geoScore);
        run.setStatus(finalStatus);
        run.setFinishedAt(now);
        run.setUpdatedAt(now);
        diagnosticRunMapper.updateById(run);
    }

    private ScoreRequest buildScoreRequest(
        Long runId,
        List<DiagnosticResult> results,
        Map<Long, Boolean> longtailByQuestion
    ) {
        List<ResultMetricSummary> metrics = new ArrayList<>(results.size());
        List<Long> longtailIds = new ArrayList<>();
        for (DiagnosticResult result : results) {
            Boolean isLongtail = longtailByQuestion.getOrDefault(result.getQuestionId(), false);
            if (Boolean.TRUE.equals(isLongtail)) {
                longtailIds.add(result.getQuestionId());
            }
            boolean brandMentioned = result.getMentionedBrands() != null && !result.getMentionedBrands().isEmpty();
            boolean citationHit = hasCitations(result.getCitationsJson());
            boolean inTop3 = result.getRank() != null && result.getRank() <= 3;
            double competitorDominance = 0.0;
            if (result.getCompetitors() != null && !result.getCompetitors().isEmpty() && !brandMentioned) {
                competitorDominance = 1.0;
            }

            ResultMetricSummary summary = new ResultMetricSummary();
            summary.setQuestionId(result.getQuestionId());
            summary.setBrandMentioned(brandMentioned);
            summary.setInTop3(inTop3);
            summary.setCompetitorDominance(competitorDominance);
            summary.setCitationHit(citationHit);
            summary.setIsLongtail(isLongtail);
            summary.setAssetComplete(1.0);
            metrics.add(summary);
        }

        ScoreRequest request = new ScoreRequest();
        request.setRunId(runId);
        request.setResults(metrics);
        request.setLongtailQuestionIds(longtailIds);
        return request;
    }

    private Map<Long, Boolean> loadLongtailMap(List<DiagnosticResult> results) {
        List<Long> questionIds = results.stream()
            .map(DiagnosticResult::getQuestionId)
            .distinct()
            .toList();
        if (questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<QuestionBank> questions = questionBankMapper.selectList(
            Wrappers.lambdaQuery(QuestionBank.class)
                .in(QuestionBank::getId, questionIds)
                .isNull(QuestionBank::getDeletedAt)
        );
        Map<Long, Boolean> map = new HashMap<>();
        for (QuestionBank q : questions) {
            map.put(q.getId(), Boolean.TRUE.equals(q.getIsLongtail()));
        }
        return map;
    }

    private void insertDiagnosticResult(ProbeTask task, Map<String, Object> result, OffsetDateTime now) {
        if (result == null || result.isEmpty()) {
            throw new ServiceException("探针成功回调缺少 result", HttpStatus.BAD_REQUEST);
        }

        DiagnosticResult entity = new DiagnosticResult();
        entity.setTenantId(task.getTenantId());
        entity.setRunId(task.getRunId());
        entity.setQuestionId(task.getQuestionId());
        entity.setPlatform(stringVal(result.get("platform"), task.getPlatform()));
        entity.setProbeMode(stringVal(result.get("probe_mode"), task.getProbeMode()));
        entity.setProbeNodeId(task.getProbeNodeId());
        entity.setModel(stringVal(result.get("model"), null));
        entity.setAnswerText(stringVal(result.get("answer_text"), null));
        entity.setMentionedBrands(stringList(result.get("mentioned_brands")));
        entity.setCompetitors(stringList(result.get("competitors")));
        entity.setLinks(stringList(result.get("links")));
        entity.setCitationsJson(jsonString(result.get("citations"), "[]"));
        entity.setCaptureMethod(stringVal(result.get("capture_method"), PROBE_MODE_GROUNDED));
        entity.setRawResponseJson(mapVal(result.get("raw_response_json")));
        entity.setScreenshotUrl(stringVal(result.get("screenshot_url"), null));
        entity.setRank(intVal(result.get("rank")));
        entity.setScoreJson(mapVal(result.get("score_json")));
        entity.setHumanCorrected(false);
        entity.setSampledAt(parseDateTime(result.get("sampled_at"), now));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        diagnosticResultMapper.insert(entity);
    }

    private List<QuestionBank> resolveQuestions(Long projectId, Long tenantId, CreateDiagnosticBo bo) {
        if (bo.getQuestionIds() != null && !bo.getQuestionIds().isEmpty()) {
            return questionBankMapper.selectList(
                Wrappers.lambdaQuery(QuestionBank.class)
                    .eq(QuestionBank::getTenantId, tenantId)
                    .eq(QuestionBank::getProjectId, projectId)
                    .in(QuestionBank::getId, bo.getQuestionIds())
                    .isNull(QuestionBank::getDeletedAt)
            );
        }

        String stage = bo.getQuestionStage();
        if (StringUtils.isBlank(stage) && StringUtils.isNotBlank(bo.getQuestionScope())) {
            Map<String, Object> scope = parseScopeJson(bo.getQuestionScope());
            if (scope.containsKey("questionIds")) {
                List<Long> ids = longList(scope.get("questionIds"));
                if (!ids.isEmpty()) {
                    CreateDiagnosticBo scoped = new CreateDiagnosticBo();
                    scoped.setQuestionIds(ids);
                    return resolveQuestions(projectId, tenantId, scoped);
                }
            }
            Object stageVal = scope.get("stage");
            if (stageVal != null) {
                stage = String.valueOf(stageVal);
            }
        }

        LambdaQueryWrapper<QuestionBank> lqw = Wrappers.lambdaQuery();
        lqw.eq(QuestionBank::getTenantId, tenantId);
        lqw.eq(QuestionBank::getProjectId, projectId);
        lqw.isNull(QuestionBank::getDeletedAt);
        if (StringUtils.isNotBlank(stage)) {
            lqw.eq(QuestionBank::getStage, stage);
        }
        return questionBankMapper.selectList(lqw);
    }

    private Map<String, Object> buildQuestionScopeMap(CreateDiagnosticBo bo) {
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
            Map<String, Object> parsed = parseScopeJson(bo.getQuestionScope());
            if (!parsed.isEmpty()) {
                return parsed;
            }
            scope.put("stage", bo.getQuestionScope());
            return scope;
        }
        scope.put("mode", "all");
        return scope;
    }

    private Map<String, Object> parseScopeJson(String raw) {
        if (StringUtils.isBlank(raw)) {
            return Collections.emptyMap();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{")) {
            return JsonUtils.parseMap(trimmed);
        }
        return Collections.emptyMap();
    }

    private List<String> normalizeProbeModes(List<String> probeModes) {
        List<String> modes = probeModes == null || probeModes.isEmpty()
            ? List.of(PROBE_MODE_GROUNDED)
            : probeModes.stream().distinct().toList();
        for (String mode : modes) {
            if (!PROBE_MODE_GROUNDED.equals(mode) && !PROBE_MODE_EXTENSION.equals(mode)) {
                throw new ServiceException("不支持的探针模式: " + mode, HttpStatus.BAD_REQUEST);
            }
        }
        return modes;
    }

    private List<String> normalizeModels(List<String> models) {
        if (models == null || models.isEmpty()) {
            return List.of("gemini");
        }
        return models;
    }

    private List<String> loadCompetitorNames(Long projectId, Long tenantId) {
        return competitorMapper.selectList(
            Wrappers.lambdaQuery(Competitor.class)
                .eq(Competitor::getTenantId, tenantId)
                .eq(Competitor::getProjectId, projectId)
                .isNull(Competitor::getDeletedAt)
                .select(Competitor::getName)
        ).stream().map(Competitor::getName).filter(StringUtils::isNotBlank).collect(Collectors.toList());
    }

    private Set<Long> resolveCalibrationQuestionIds(
        List<QuestionBank> questions,
        boolean dualModeCalibration,
        BigDecimal calibrationRatio
    ) {
        if (!dualModeCalibration || questions.isEmpty()) {
            return Set.of();
        }
        int count = Math.max(1, (int) Math.ceil(questions.size() * calibrationRatio.doubleValue()));
        return questions.stream()
            .sorted(Comparator.comparing(QuestionBank::getId))
            .limit(count)
            .map(QuestionBank::getId)
            .collect(Collectors.toCollection(HashSet::new));
    }

    private Set<Long> resolveCalibrationQuestionIdsFromRun(DiagnosticRun run) {
        Map<String, Object> scope = run.getQuestionScope();
        if (scope != null && scope.containsKey("calibrationQuestionIds")) {
            return new HashSet<>(longList(scope.get("calibrationQuestionIds")));
        }
        List<ProbeTask> extensionTasks = probeTaskMapper.selectList(
            Wrappers.lambdaQuery(ProbeTask.class)
                .eq(ProbeTask::getRunId, run.getId())
                .eq(ProbeTask::getProbeMode, PROBE_MODE_EXTENSION)
                .isNull(ProbeTask::getDeletedAt)
        );
        return extensionTasks.stream().map(ProbeTask::getQuestionId).collect(Collectors.toSet());
    }

    private DiagnosticResult findCalibrationResult(
        List<DiagnosticResult> results,
        Long questionId,
        String platform,
        String probeMode
    ) {
        return results.stream()
            .filter(r -> questionId.equals(r.getQuestionId()))
            .filter(r -> platform.equals(r.getPlatform()))
            .filter(r -> probeMode.equals(r.getProbeMode()))
            .findFirst()
            .orElse(null);
    }

    private DiagnosticCalibrationSideVo toCalibrationSide(
        DiagnosticResult result,
        ProbeNode node,
        String brandName
    ) {
        DiagnosticCalibrationSideVo side = new DiagnosticCalibrationSideVo();
        side.setResultId(result.getId());
        side.setAnswerPreview(DiagnosticCalibrationSupport.preview(result.getAnswerText()));
        side.setBrandMentioned(DiagnosticCalibrationSupport.mentionsBrand(result.getMentionedBrands(), brandName));
        side.setRank(result.getRank());
        side.setCitationCount(DiagnosticCalibrationSupport.citationCount(result));
        if (node != null) {
            side.setProbeNodeKey(node.getNodeKey());
        }
        return side;
    }

    private CustomerProject getOwnedProjectOrThrow(Long projectId, Long tenantId) {
        CustomerProject project = customerProjectMapper.selectById(projectId);
        if (project == null || project.getDeletedAt() != null) {
            throw new ServiceException("项目不存在", HttpStatus.NOT_FOUND);
        }
        if (!tenantId.equals(project.getTenantId())) {
            throw new ServiceException("无权访问该项目", HttpStatus.FORBIDDEN);
        }
        return project;
    }

    private DiagnosticRun getOwnedRunOrThrow(Long runId) {
        DiagnosticRun run = diagnosticRunMapper.selectById(runId);
        if (run == null || run.getDeletedAt() != null) {
            throw new ServiceException("诊断任务不存在", HttpStatus.NOT_FOUND);
        }
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        if (!tenantId.equals(run.getTenantId())) {
            throw new ServiceException("无权访问该诊断任务", HttpStatus.FORBIDDEN);
        }
        return run;
    }

    private Integer computeProgress(Long runId) {
        long total = probeTaskMapper.selectCount(
            Wrappers.lambdaQuery(ProbeTask.class)
                .eq(ProbeTask::getRunId, runId)
                .isNull(ProbeTask::getDeletedAt)
        );
        if (total == 0) {
            return 0;
        }
        long done = probeTaskMapper.selectCount(
            Wrappers.lambdaQuery(ProbeTask.class)
                .eq(ProbeTask::getRunId, runId)
                .isNull(ProbeTask::getDeletedAt)
                .apply("status IN ('SUCCESS'::probe_task_status, 'FAILED'::probe_task_status)")
        );
        return (int) (done * 100 / total);
    }

    private boolean hasCitations(String citationsJson) {
        if (StringUtils.isBlank(citationsJson)) {
            return false;
        }
        String trimmed = citationsJson.trim();
        return !"[]".equals(trimmed) && !"{}".equals(trimmed);
    }

    private String stringVal(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
        }
        return List.of(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapVal(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return Collections.emptyMap();
    }

    private String jsonString(Object value, String defaultJson) {
        if (value == null) {
            return defaultJson;
        }
        if (value instanceof String str) {
            return str;
        }
        return JsonUtils.toJsonString(value);
    }

    private Integer intVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> longList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Number number) {
                ids.add(number.longValue());
            } else if (item != null) {
                try {
                    ids.add(Long.parseLong(String.valueOf(item)));
                } catch (NumberFormatException ignored) {
                    // skip invalid id
                }
            }
        }
        return ids;
    }

    private OffsetDateTime parseDateTime(Object value, OffsetDateTime fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return OffsetDateTime.parse(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private record ProbeTaskDispatch(
        ProbeTask task,
        QuestionBank question,
        PlatformModel platformModel,
        int sampleIndex
    ) {
    }
}
