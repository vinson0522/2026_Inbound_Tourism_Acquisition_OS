package org.dromara.diagnostic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.diagnostic.domain.DiagnosticRun;
import org.dromara.diagnostic.domain.Report;
import org.dromara.diagnostic.domain.bo.MonthlyReportBo;
import org.dromara.diagnostic.domain.bo.ReportQueryBo;
import org.dromara.diagnostic.domain.bo.WeeklyReportBo;
import org.dromara.diagnostic.domain.vo.ReportDetailVo;
import org.dromara.diagnostic.domain.vo.ReportVo;
import org.dromara.diagnostic.mapper.DiagnosticRunMapper;
import org.dromara.diagnostic.mapper.ReportMapper;
import org.dromara.diagnostic.report.DiagnosticReportFile;
import org.dromara.diagnostic.report.GotenbergClient;
import org.dromara.diagnostic.report.MonthlyDocxReportRenderer;
import org.dromara.diagnostic.report.MonthlyHtmlReportRenderer;
import org.dromara.diagnostic.report.WeeklyDocxReportRenderer;
import org.dromara.diagnostic.report.WeeklyHtmlReportRenderer;
import org.dromara.diagnostic.report.WeeklyReportContext;
import org.dromara.diagnostic.service.IDiagnosticReportExportService;
import org.dromara.diagnostic.service.IReportService;
import org.dromara.diagnostic.support.BusinessTenantHelper;
import org.dromara.project.billing.QuotaType;
import org.dromara.project.domain.ContentTask;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.domain.KeywordOpportunity;
import org.dromara.project.domain.LandingPage;
import org.dromara.project.domain.Lead;
import org.dromara.project.mapper.ContentTaskMapper;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.dromara.project.mapper.KeywordOpportunityMapper;
import org.dromara.project.mapper.LandingPageMapper;
import org.dromara.project.mapper.LeadMapper;
import org.dromara.project.report.ReportBranding;
import org.dromara.project.service.IQuotaService;
import org.dromara.project.service.IReportTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements IReportService {

    private static final int MAX_WEEKLY_DAYS = 31;
    private static final int MAX_MONTHLY_DAYS = 62;
    private static final String PDF_GOTENBERG_HINT =
        "PDF 导出需要 Gotenberg。本地请 cd deploy && docker compose up -d gotenberg，"
            + "并设置 GOTENBERG_BASE_URL=http://localhost:3002";
    private static final String PDF_GOTENBERG_DOWN =
        "Gotenberg 未响应（请确认 inbound-gotenberg 容器 healthy）";

    private final ReportMapper reportMapper;
    private final DiagnosticRunMapper diagnosticRunMapper;
    private final KeywordOpportunityMapper keywordOpportunityMapper;
    private final ContentTaskMapper contentTaskMapper;
    private final LandingPageMapper landingPageMapper;
    private final LeadMapper leadMapper;
    private final CustomerProjectMapper customerProjectMapper;
    private final IDiagnosticReportExportService diagnosticReportExportService;
    private final GotenbergClient gotenbergClient;
    private final IQuotaService quotaService;
    private final IReportTemplateService reportTemplateService;

    @Override
    public TableDataInfo<ReportVo> queryPageList(Long projectId, ReportQueryBo bo, PageQuery pageQuery) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        assertProjectOwned(projectId, tenantId);

        LambdaQueryWrapper<Report> lqw = Wrappers.lambdaQuery();
        lqw.eq(Report::getTenantId, tenantId);
        lqw.eq(Report::getProjectId, projectId);
        lqw.isNull(Report::getDeletedAt);
        if (bo != null && StringUtils.isNotBlank(bo.getType())) {
            lqw.eq(Report::getType, bo.getType().trim().toUpperCase(Locale.ROOT));
        }
        if (bo != null && StringUtils.isNotBlank(bo.getPeriod())) {
            lqw.like(Report::getPeriod, bo.getPeriod().trim());
        }
        lqw.orderByDesc(Report::getCreatedAt);

        Page<Report> page = reportMapper.selectPage(pageQuery.build(), lqw);
        List<ReportVo> rows = page.getRecords().stream().map(this::toListVo).toList();
        Page<ReportVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(rows);
        return TableDataInfo.build(voPage);
    }

    @Override
    public ReportDetailVo queryById(Long projectId, Long reportId) {
        Report report = getOwnedReportOrThrow(projectId, reportId);
        return toDetailVo(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWeeklyReport(Long projectId, WeeklyReportBo bo) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        CustomerProject project = assertProjectOwned(projectId, tenantId);
        quotaService.checkAndConsume(tenantId, QuotaType.REPORTS_PER_MONTH, 1);

        LocalDate end = parseDateOrDefault(bo != null ? bo.getPeriodEnd() : null, LocalDate.now());
        LocalDate start = parseDateOrDefault(
            bo != null ? bo.getPeriodStart() : null,
            end.minusDays(6)
        );
        if (start.isAfter(end)) {
            throw new ServiceException("periodStart 不能晚于 periodEnd", HttpStatus.BAD_REQUEST);
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_WEEKLY_DAYS) {
            throw new ServiceException("统计区间最长 " + MAX_WEEKLY_DAYS + " 天", HttpStatus.BAD_REQUEST);
        }

        OffsetDateTime rangeStart = start.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime rangeEnd = end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1);

        Map<String, Object> summary = aggregateWeeklySummary(projectId, tenantId, start, end, rangeStart, rangeEnd);
        attachTemplateSnapshot(tenantId, summary);
        String period = isoWeekLabel(end);

        Report report = new Report();
        report.setTenantId(tenantId);
        report.setProjectId(projectId);
        report.setType("WEEKLY");
        report.setPeriod(period);
        report.setSummary(JsonUtils.toJsonString(summary));
        report.setTemplateId(reportTemplateService.getReportTemplate(tenantId).getTemplateId());
        report.setCreatedAt(OffsetDateTime.now());
        report.setUpdatedAt(OffsetDateTime.now());
        report.setCreatedBy(LoginHelper.getUserId());
        reportMapper.insert(report);

        log.info("FR-702 weekly report projectId={} reportId={} period={} project={}",
            projectId, report.getId(), period, project.getName());
        return report.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMonthlyReport(Long projectId, MonthlyReportBo bo) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        CustomerProject project = assertProjectOwned(projectId, tenantId);
        quotaService.checkAndConsume(tenantId, QuotaType.REPORTS_PER_MONTH, 1);

        MonthlyRange range = resolveMonthlyRange(bo);
        OffsetDateTime rangeStart = range.start().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime rangeEnd = range.end().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1);
        OffsetDateTime prevRangeStart = range.prevStart().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime prevRangeEnd = range.prevEnd().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1);

        Map<String, Object> summary = aggregateMonthlySummary(
            projectId, tenantId, range, rangeStart, rangeEnd, prevRangeStart, prevRangeEnd
        );
        attachTemplateSnapshot(tenantId, summary);

        Report report = new Report();
        report.setTenantId(tenantId);
        report.setProjectId(projectId);
        report.setType("MONTHLY");
        report.setPeriod(range.periodLabel());
        report.setSummary(JsonUtils.toJsonString(summary));
        report.setTemplateId(reportTemplateService.getReportTemplate(tenantId).getTemplateId());
        report.setCreatedAt(OffsetDateTime.now());
        report.setUpdatedAt(OffsetDateTime.now());
        report.setCreatedBy(LoginHelper.getUserId());
        reportMapper.insert(report);

        log.info("FR-703 monthly report projectId={} reportId={} period={} project={}",
            projectId, report.getId(), range.periodLabel(), project.getName());
        return report.getId();
    }

    @Override
    public DiagnosticReportFile exportReport(Long projectId, Long reportId, String format) {
        Report report = getOwnedReportOrThrow(projectId, reportId);
        String fmt = StringUtils.blankToDefault(format, "docx").toLowerCase(Locale.ROOT);
        if (!Set.of("docx", "pdf").contains(fmt)) {
            throw new ServiceException("Unsupported format: " + format, HttpStatus.BAD_REQUEST);
        }

        if ("DIAGNOSTIC".equalsIgnoreCase(report.getType())) {
            Long runId = extractRunId(report);
            if (runId == null) {
                throw new ServiceException("诊断报告缺少 runId", HttpStatus.BAD_REQUEST);
            }
            return diagnosticReportExportService.exportReport(runId, fmt);
        }

        if ("WEEKLY".equalsIgnoreCase(report.getType())) {
            return exportPeriodReport(report, fmt, "WEEKLY");
        }

        if ("MONTHLY".equalsIgnoreCase(report.getType())) {
            return exportPeriodReport(report, fmt, "MONTHLY");
        }

        throw new ServiceException("暂不支持该报告类型导出: " + report.getType(), HttpStatus.BAD_REQUEST);
    }

    private DiagnosticReportFile exportPeriodReport(Report report, String fmt, String reportKind) {
        CustomerProject project = customerProjectMapper.selectById(report.getProjectId());
        Map<String, Object> summary = parseSummaryMap(report.getSummary());
        String periodStart = stringVal(summary.get("periodStart"));
        String periodEnd = stringVal(summary.get("periodEnd"));

        ReportBranding branding = reportTemplateService.resolveBranding(report.getTenantId(), report.getTemplateId());

        WeeklyReportContext ctx = WeeklyReportContext.builder()
            .projectName(project != null ? project.getName() : "—")
            .brandName(project != null ? StringUtils.blankToDefault(project.getBrandName(), "—") : "—")
            .period(report.getPeriod())
            .periodStart(periodStart)
            .periodEnd(periodEnd)
            .reportKind(reportKind)
            .summary(summary)
            .branding(branding)
            .build();

        String safeProject = sanitizeFilename(project != null ? project.getName() : "project");
        String suffix = "MONTHLY".equalsIgnoreCase(reportKind) ? "monthly" : "weekly";
        String baseName = safeProject + "-" + suffix + "-" + StringUtils.blankToDefault(report.getPeriod(), "report");

        try {
            if ("pdf".equals(fmt)) {
                if (!gotenbergClient.isEnabled()) {
                    throw new ServiceException(PDF_GOTENBERG_HINT, HttpStatus.BAD_REQUEST);
                }
                if (!gotenbergClient.isReachable()) {
                    throw new ServiceException(PDF_GOTENBERG_DOWN, HttpStatus.BAD_REQUEST);
                }
                String html = "MONTHLY".equalsIgnoreCase(reportKind)
                    ? MonthlyHtmlReportRenderer.render(ctx)
                    : WeeklyHtmlReportRenderer.render(ctx);
                byte[] pdf = gotenbergClient.htmlToPdf(html.getBytes(StandardCharsets.UTF_8), "index.html");
                return DiagnosticReportFile.builder()
                    .content(pdf)
                    .filename(baseName + ".pdf")
                    .contentType("application/pdf")
                    .build();
            }

            byte[] docx = "MONTHLY".equalsIgnoreCase(reportKind)
                ? MonthlyDocxReportRenderer.render(ctx)
                : WeeklyDocxReportRenderer.render(ctx);
            return DiagnosticReportFile.builder()
                .content(docx)
                .filename(baseName + ".docx")
                .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build();
        } catch (ServiceException e) {
            throw e;
        } catch (IllegalStateException e) {
            log.warn("{} PDF export failed reportId={}: {}", reportKind.toLowerCase(Locale.ROOT), report.getId(), e.getMessage());
            throw new ServiceException(PDF_GOTENBERG_DOWN, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            throw new ServiceException(reportKind + " 报告导出失败: " + e.getMessage(), HttpStatus.ERROR);
        }
    }

    private void attachTemplateSnapshot(Long tenantId, Map<String, Object> summary) {
        ReportBranding branding = reportTemplateService.resolveBranding(tenantId, null);
        summary.put("templateSnapshot", branding.toSnapshot());
    }

    private Map<String, Object> aggregateMonthlySummary(
        Long projectId,
        Long tenantId,
        MonthlyRange range,
        OffsetDateTime rangeStart,
        OffsetDateTime rangeEnd,
        OffsetDateTime prevRangeStart,
        OffsetDateTime prevRangeEnd
    ) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("periodStart", range.start().toString());
        summary.put("periodEnd", range.end().toString());
        summary.put("periodPrevStart", range.prevStart().toString());
        summary.put("periodPrevEnd", range.prevEnd().toString());
        summary.put("geo", aggregateGeoWithMom(projectId, tenantId, rangeStart, rangeEnd, prevRangeStart, prevRangeEnd));
        summary.put("keywords", aggregateKeywords(projectId, tenantId, rangeStart, rangeEnd));
        summary.put("content", aggregateContent(projectId, tenantId, rangeStart, rangeEnd));
        summary.put("landing", aggregateLanding(projectId, tenantId));
        summary.put("leads", aggregateLeads(projectId, tenantId, rangeStart, rangeEnd));
        summary.put("recommendations", buildMonthlyRecommendations(summary));
        return summary;
    }

    private Map<String, Object> aggregateGeoWithMom(
        Long projectId,
        Long tenantId,
        OffsetDateTime rangeStart,
        OffsetDateTime rangeEnd,
        OffsetDateTime prevRangeStart,
        OffsetDateTime prevRangeEnd
    ) {
        Map<String, Object> geo = aggregateGeo(projectId, tenantId, rangeStart, rangeEnd);
        Integer prevScore = latestScoreInRange(projectId, tenantId, prevRangeStart, prevRangeEnd);
        geo.put("prevScore", prevScore);
        Object latestObj = geo.get("latestScore");
        if (latestObj instanceof Number latest && prevScore != null) {
            geo.put("momDelta", latest.intValue() - prevScore);
        } else {
            geo.put("momDelta", 0);
        }
        return geo;
    }

    private Integer latestScoreInRange(
        Long projectId,
        Long tenantId,
        OffsetDateTime rangeStart,
        OffsetDateTime rangeEnd
    ) {
        DiagnosticRun latest = diagnosticRunMapper.selectList(
            Wrappers.lambdaQuery(DiagnosticRun.class)
                .eq(DiagnosticRun::getTenantId, tenantId)
                .eq(DiagnosticRun::getProjectId, projectId)
                .isNull(DiagnosticRun::getDeletedAt)
                .isNotNull(DiagnosticRun::getFinishedAt)
                .isNotNull(DiagnosticRun::getGeoScore)
                .ge(DiagnosticRun::getFinishedAt, rangeStart)
                .le(DiagnosticRun::getFinishedAt, rangeEnd)
                .apply("status IN ('SUCCESS'::diagnostic_run_status, 'PARTIAL_FAILED'::diagnostic_run_status)")
                .orderByDesc(DiagnosticRun::getFinishedAt)
                .last("LIMIT 1")
        ).stream().findFirst().orElse(null);
        return latest != null ? toIntScore(latest.getGeoScore()) : null;
    }

    private MonthlyRange resolveMonthlyRange(MonthlyReportBo bo) {
        if (bo != null && bo.getYear() != null && bo.getMonth() != null) {
            if (bo.getMonth() < 1 || bo.getMonth() > 12) {
                throw new ServiceException("month 必须在 1-12", HttpStatus.BAD_REQUEST);
            }
            YearMonth ym = YearMonth.of(bo.getYear(), bo.getMonth());
            YearMonth prev = ym.minusMonths(1);
            return new MonthlyRange(
                ym.atDay(1),
                ym.atEndOfMonth(),
                prev.atDay(1),
                prev.atEndOfMonth(),
                ym.toString()
            );
        }

        LocalDate end = parseDateOrDefault(bo != null ? bo.getPeriodEnd() : null, LocalDate.now());
        LocalDate start = parseDateOrDefault(bo != null ? bo.getPeriodStart() : null, end.minusDays(29));
        if (start.isAfter(end)) {
            throw new ServiceException("periodStart 不能晚于 periodEnd", HttpStatus.BAD_REQUEST);
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_MONTHLY_DAYS) {
            throw new ServiceException("统计区间最长 " + MAX_MONTHLY_DAYS + " 天", HttpStatus.BAD_REQUEST);
        }
        LocalDate prevEnd = start.minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(days - 1);
        return new MonthlyRange(start, end, prevStart, prevEnd, YearMonth.from(start).toString());
    }

    private record MonthlyRange(
        LocalDate start,
        LocalDate end,
        LocalDate prevStart,
        LocalDate prevEnd,
        String periodLabel
    ) {
    }

    private Map<String, Object> aggregateWeeklySummary(
        Long projectId,
        Long tenantId,
        LocalDate start,
        LocalDate end,
        OffsetDateTime rangeStart,
        OffsetDateTime rangeEnd
    ) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("periodStart", start.toString());
        summary.put("periodEnd", end.toString());
        summary.put("geo", aggregateGeo(projectId, tenantId, rangeStart, rangeEnd));
        summary.put("keywords", aggregateKeywords(projectId, tenantId, rangeStart, rangeEnd));
        summary.put("content", aggregateContent(projectId, tenantId, rangeStart, rangeEnd));
        summary.put("landing", aggregateLanding(projectId, tenantId));
        summary.put("leads", aggregateLeads(projectId, tenantId, rangeStart, rangeEnd));
        summary.put("recommendations", buildRecommendations(summary));
        return summary;
    }

    private Map<String, Object> aggregateGeo(
        Long projectId,
        Long tenantId,
        OffsetDateTime rangeStart,
        OffsetDateTime rangeEnd
    ) {
        List<DiagnosticRun> inPeriod = diagnosticRunMapper.selectList(
            Wrappers.lambdaQuery(DiagnosticRun.class)
                .eq(DiagnosticRun::getTenantId, tenantId)
                .eq(DiagnosticRun::getProjectId, projectId)
                .isNull(DiagnosticRun::getDeletedAt)
                .isNotNull(DiagnosticRun::getFinishedAt)
                .ge(DiagnosticRun::getFinishedAt, rangeStart)
                .le(DiagnosticRun::getFinishedAt, rangeEnd)
                .apply("status IN ('SUCCESS'::diagnostic_run_status, 'PARTIAL_FAILED'::diagnostic_run_status)")
        );

        Map<String, Object> geo = new LinkedHashMap<>();
        geo.put("runs", inPeriod.size());

        DiagnosticRun latest = inPeriod.stream()
            .filter(r -> r.getGeoScore() != null)
            .max(Comparator.comparing(DiagnosticRun::getFinishedAt))
            .orElse(null);

        Integer latestScore = latest != null ? toIntScore(latest.getGeoScore()) : null;
        geo.put("latestScore", latestScore);

        DiagnosticRun previous = diagnosticRunMapper.selectList(
            Wrappers.lambdaQuery(DiagnosticRun.class)
                .eq(DiagnosticRun::getTenantId, tenantId)
                .eq(DiagnosticRun::getProjectId, projectId)
                .isNull(DiagnosticRun::getDeletedAt)
                .isNotNull(DiagnosticRun::getFinishedAt)
                .isNotNull(DiagnosticRun::getGeoScore)
                .lt(DiagnosticRun::getFinishedAt, rangeStart)
                .apply("status IN ('SUCCESS'::diagnostic_run_status, 'PARTIAL_FAILED'::diagnostic_run_status)")
                .orderByDesc(DiagnosticRun::getFinishedAt)
                .last("LIMIT 1")
        ).stream().findFirst().orElse(null);

        if (latestScore != null && previous != null && previous.getGeoScore() != null) {
            geo.put("delta", latestScore - toIntScore(previous.getGeoScore()));
        } else {
            geo.put("delta", 0);
        }
        return geo;
    }

    private Map<String, Object> aggregateKeywords(
        Long projectId,
        Long tenantId,
        OffsetDateTime rangeStart,
        OffsetDateTime rangeEnd
    ) {
        List<KeywordOpportunity> rows = keywordOpportunityMapper.selectList(
            Wrappers.lambdaQuery(KeywordOpportunity.class)
                .eq(KeywordOpportunity::getTenantId, tenantId)
                .eq(KeywordOpportunity::getProjectId, projectId)
                .isNull(KeywordOpportunity::getDeletedAt)
                .ge(KeywordOpportunity::getCreatedAt, rangeStart)
                .le(KeywordOpportunity::getCreatedAt, rangeEnd)
        );
        Map<String, Long> byStage = rows.stream()
            .filter(k -> StringUtils.isNotBlank(k.getStage()))
            .collect(Collectors.groupingBy(KeywordOpportunity::getStage, LinkedHashMap::new, Collectors.counting()));

        Map<String, Object> keywords = new LinkedHashMap<>();
        keywords.put("newCount", rows.size());
        keywords.put("byStage", byStage);
        double avgScore = rows.stream()
            .filter(k -> k.getScore() != null)
            .mapToDouble(k -> k.getScore().doubleValue())
            .average()
            .orElse(0d);
        keywords.put("avgScore", BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP));
        return keywords;
    }

    private Map<String, Object> aggregateContent(
        Long projectId,
        Long tenantId,
        OffsetDateTime rangeStart,
        OffsetDateTime rangeEnd
    ) {
        long tasksCreated = contentTaskMapper.selectCount(
            Wrappers.lambdaQuery(ContentTask.class)
                .eq(ContentTask::getTenantId, tenantId)
                .eq(ContentTask::getProjectId, projectId)
                .isNull(ContentTask::getDeletedAt)
                .ge(ContentTask::getCreatedAt, rangeStart)
                .le(ContentTask::getCreatedAt, rangeEnd)
        );
        long generated = contentTaskMapper.selectCount(
            Wrappers.lambdaQuery(ContentTask.class)
                .eq(ContentTask::getTenantId, tenantId)
                .eq(ContentTask::getProjectId, projectId)
                .isNull(ContentTask::getDeletedAt)
                .apply("status IN ('GENERATED'::content_task_status, 'ADOPTED'::content_task_status)")
                .ge(ContentTask::getUpdatedAt, rangeStart)
                .le(ContentTask::getUpdatedAt, rangeEnd)
        );

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("tasksCreated", tasksCreated);
        content.put("generated", generated);
        return content;
    }

    private Map<String, Object> aggregateLanding(Long projectId, Long tenantId) {
        long draftCount = landingPageMapper.selectCount(
            Wrappers.lambdaQuery(LandingPage.class)
                .eq(LandingPage::getTenantId, tenantId)
                .eq(LandingPage::getProjectId, projectId)
                .isNull(LandingPage::getDeletedAt)
                .apply("status = 'DRAFT'::landing_page_status")
        );
        long publishedCount = landingPageMapper.selectCount(
            Wrappers.lambdaQuery(LandingPage.class)
                .eq(LandingPage::getTenantId, tenantId)
                .eq(LandingPage::getProjectId, projectId)
                .isNull(LandingPage::getDeletedAt)
                .apply("status = 'PUBLISHED'::landing_page_status")
        );
        Map<String, Object> landing = new LinkedHashMap<>();
        landing.put("draftCount", draftCount);
        landing.put("publishedCount", publishedCount);
        return landing;
    }

    private Map<String, Object> aggregateLeads(
        Long projectId,
        Long tenantId,
        OffsetDateTime rangeStart,
        OffsetDateTime rangeEnd
    ) {
        List<Lead> rows = leadMapper.selectList(
            Wrappers.lambdaQuery(Lead.class)
                .eq(Lead::getTenantId, tenantId)
                .eq(Lead::getProjectId, projectId)
                .isNull(Lead::getDeletedAt)
                .ge(Lead::getCreatedAt, rangeStart)
                .le(Lead::getCreatedAt, rangeEnd)
        );
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String status : List.of("NEW", "FOLLOWING", "QUOTED", "WON", "LOST")) {
            byStatus.put(status, 0L);
        }
        for (Lead lead : rows) {
            String status = StringUtils.blankToDefault(lead.getStatus(), "NEW").toUpperCase(Locale.ROOT);
            byStatus.merge(status, 1L, Long::sum);
        }
        Map<String, Object> leads = new LinkedHashMap<>();
        leads.put("newCount", rows.size());
        leads.put("byStatus", byStatus);
        leads.put("wonCount", byStatus.getOrDefault("WON", 0L));
        return leads;
    }

    @SuppressWarnings("unchecked")
    private List<String> buildRecommendations(Map<String, Object> summary) {
        List<String> recs = new ArrayList<>(3);
        Map<String, Object> geo = (Map<String, Object>) summary.get("geo");
        Map<String, Object> keywords = (Map<String, Object>) summary.get("keywords");
        Map<String, Object> content = (Map<String, Object>) summary.get("content");
        Map<String, Object> leads = (Map<String, Object>) summary.get("leads");

        int runs = geo != null ? intVal(geo.get("runs")) : 0;
        Integer latestScore = geo != null && geo.get("latestScore") != null ? intVal(geo.get("latestScore")) : null;
        int newKeywords = keywords != null ? intVal(keywords.get("newCount")) : 0;
        int generated = content != null ? intVal(content.get("generated")) : 0;
        int newLeads = leads != null ? intVal(leads.get("newCount")) : 0;

        if (runs == 0) {
            recs.add("本周未完成 GEO 诊断，建议运行一次 grounded-api 诊断以跟踪 AI 可见率。");
        } else if (latestScore != null && latestScore < 70) {
            recs.add("GEO 分数偏低（" + latestScore + "），建议补充品牌资产与长尾问题覆盖后再诊断。");
        } else {
            recs.add("GEO 表现稳定，建议保持每周至少一次诊断采样并对比竞品。");
        }

        if (newKeywords < 3) {
            recs.add("关键词机会较少，建议使用关键词 Agent 补充八阶段词库。");
        } else {
            recs.add("已发现 " + newKeywords + " 个新关键词，优先为高机会词规划内容与落地页。");
        }

        if (newLeads == 0) {
            recs.add("本周暂无新询盘，检查落地页发布状态与表单转化路径。");
        } else if (generated == 0) {
            recs.add("有新询盘但内容产出不足，建议为热门关键词生成短视频脚本。");
        } else {
            recs.add("本周新增 " + newLeads + " 条询盘，请在线索列表跟进并标注转化阶段。");
        }

        while (recs.size() < 3) {
            recs.add("持续监控 GEO 分数与询盘转化，按周复盘增长动作。");
        }
        return recs.subList(0, 3);
    }

    @SuppressWarnings("unchecked")
    private List<String> buildMonthlyRecommendations(Map<String, Object> summary) {
        List<String> recs = new ArrayList<>(5);
        Map<String, Object> geo = (Map<String, Object>) summary.get("geo");
        Map<String, Object> keywords = (Map<String, Object>) summary.get("keywords");
        Map<String, Object> content = (Map<String, Object>) summary.get("content");
        Map<String, Object> leads = (Map<String, Object>) summary.get("leads");
        Map<String, Object> landing = (Map<String, Object>) summary.get("landing");

        int runs = geo != null ? intVal(geo.get("runs")) : 0;
        Integer latestScore = geo != null && geo.get("latestScore") != null ? intVal(geo.get("latestScore")) : null;
        int momDelta = geo != null ? intVal(geo.get("momDelta")) : 0;
        int newKeywords = keywords != null ? intVal(keywords.get("newCount")) : 0;
        int generated = content != null ? intVal(content.get("generated")) : 0;
        int newLeads = leads != null ? intVal(leads.get("newCount")) : 0;
        int wonCount = leads != null ? intVal(leads.get("wonCount")) : 0;
        int published = landing != null ? intVal(landing.get("publishedCount")) : 0;

        if (runs == 0) {
            recs.add("本月未完成 GEO 诊断，建议至少运行一次 grounded-api 诊断以建立月度基线。");
        } else if (momDelta < 0) {
            recs.add("GEO 分数环比下降 " + Math.abs(momDelta) + " 分，建议复盘竞品压制与引用覆盖并补充品牌资产。");
        } else if (latestScore != null && latestScore < 70) {
            recs.add("GEO 分数偏低（" + latestScore + "），优先补充长尾问题与官网引用后再诊断。");
        } else {
            recs.add("GEO 表现稳定（MoM " + (momDelta >= 0 ? "+" : "") + momDelta + "），保持月度采样频率。");
        }

        if (newKeywords < 5) {
            recs.add("关键词机会偏少，建议使用关键词 Agent 扩展八阶段词库。");
        } else {
            recs.add("本月新增 " + newKeywords + " 个关键词，优先为高均分词规划内容与落地页。");
        }

        if (newLeads == 0) {
            recs.add("本月暂无新询盘，检查落地页发布与表单转化路径。");
        } else if (wonCount == 0) {
            recs.add("有 " + newLeads + " 条新询盘但未成交，重点跟进 QUOTED 阶段并在 48h 内回访。");
        } else {
            recs.add("本月成交 " + wonCount + " 单，复盘 WON 线索来源并复制到高机会市场。");
        }

        if (generated == 0) {
            recs.add("内容产出不足，建议为热门关键词生成短视频脚本与落地页模块。");
        } else if (published == 0) {
            recs.add("已有内容但未发布落地页，建议将 READY 页面上线以承接询盘。");
        } else {
            recs.add("内容与落地页产出正常，持续监控 GEO 与询盘转化并按 MoM 复盘。");
        }

        while (recs.size() < 5) {
            recs.add("持续监控 GEO MoM 与 CRM 五态分布，按月优化增长动作。");
        }
        return recs.subList(0, 5);
    }

    private ReportVo toListVo(Report report) {
        ReportVo vo = new ReportVo();
        vo.setId(report.getId());
        vo.setProjectId(report.getProjectId());
        vo.setType(report.getType());
        vo.setPeriod(report.getPeriod());
        vo.setCreatedAt(report.getCreatedAt());
        vo.setSummaryPreview(buildSummaryPreview(report));
        return vo;
    }

    private ReportDetailVo toDetailVo(Report report) {
        ReportDetailVo vo = new ReportDetailVo();
        vo.setId(report.getId());
        vo.setProjectId(report.getProjectId());
        vo.setType(report.getType());
        vo.setPeriod(report.getPeriod());
        vo.setCreatedAt(report.getCreatedAt());
        vo.setSummaryPreview(buildSummaryPreview(report));
        vo.setSummary(parseSummaryMap(report.getSummary()));
        return vo;
    }

    private String buildSummaryPreview(Report report) {
        Map<String, Object> summary = parseSummaryMap(report.getSummary());
        if (summary.isEmpty()) {
            return StringUtils.blankToDefault(report.getPeriod(), "—");
        }
        if ("DIAGNOSTIC".equalsIgnoreCase(report.getType())) {
            List<String> parts = new ArrayList<>();
            Object geoScore = summary.get("geoScore");
            parts.add("GEO Score " + (geoScore != null ? geoScore : "—"));
            Object region = summary.get("region");
            if (region != null && StringUtils.isNotBlank(String.valueOf(region))) {
                parts.add(String.valueOf(region));
            }
            return String.join(" · ", parts);
        }
        if ("WEEKLY".equalsIgnoreCase(report.getType())) {
            List<String> parts = new ArrayList<>();
            appendNested(parts, summary, "leads", "newCount", "询盘 ");
            appendNested(parts, summary, "keywords", "newCount", "新词 ");
            Map<String, Object> geo = nested(summary, "geo");
            if (geo != null && geo.get("delta") != null) {
                int d = intVal(geo.get("delta"));
                parts.add("GEO " + (d >= 0 ? "+" : "") + d);
            } else if (geo != null && geo.get("latestScore") != null) {
                parts.add("GEO " + geo.get("latestScore"));
            }
            return parts.isEmpty() ? StringUtils.blankToDefault(report.getPeriod(), "—") : String.join(" · ", parts);
        }
        if ("MONTHLY".equalsIgnoreCase(report.getType())) {
            List<String> parts = new ArrayList<>();
            Map<String, Object> geo = nested(summary, "geo");
            if (geo != null && geo.get("latestScore") != null) {
                parts.add("GEO " + geo.get("latestScore"));
            }
            if (geo != null && geo.get("momDelta") != null) {
                int d = intVal(geo.get("momDelta"));
                parts.add("MoM " + (d >= 0 ? "+" : "") + d);
            }
            appendNested(parts, summary, "leads", "newCount", "询盘 ");
            appendNested(parts, summary, "leads", "wonCount", "成交 ");
            return parts.isEmpty() ? StringUtils.blankToDefault(report.getPeriod(), "—") : String.join(" · ", parts);
        }
        return StringUtils.blankToDefault(report.getPeriod(), "—");
    }

    private static void appendNested(
        List<String> parts,
        Map<String, Object> summary,
        String section,
        String field,
        String label
    ) {
        Map<String, Object> nested = nested(summary, section);
        if (nested != null && nested.get(field) != null) {
            parts.add(label + nested.get(field));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> nested(Map<String, Object> summary, String key) {
        Object v = summary.get(key);
        if (v instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return null;
    }

    private Map<String, Object> parseSummaryMap(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> map = JsonUtils.parseMap(json);
            return map != null ? map : new LinkedHashMap<>();
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private Long extractRunId(Report report) {
        Map<String, Object> summary = parseSummaryMap(report.getSummary());
        Object runId = summary.get("runId");
        if (runId instanceof Number n) {
            return n.longValue();
        }
        if (StringUtils.isNotBlank(report.getPeriod()) && StringUtils.isNumeric(report.getPeriod())) {
            return Long.parseLong(report.getPeriod());
        }
        return null;
    }

    private Report getOwnedReportOrThrow(Long projectId, Long reportId) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        assertProjectOwned(projectId, tenantId);
        Report report = reportMapper.selectOne(
            Wrappers.lambdaQuery(Report.class)
                .eq(Report::getId, reportId)
                .eq(Report::getProjectId, projectId)
                .eq(Report::getTenantId, tenantId)
                .isNull(Report::getDeletedAt)
        );
        if (report == null) {
            throw new ServiceException("报告不存在", HttpStatus.NOT_FOUND);
        }
        return report;
    }

    private CustomerProject assertProjectOwned(Long projectId, Long tenantId) {
        CustomerProject project = customerProjectMapper.selectOne(
            Wrappers.lambdaQuery(CustomerProject.class)
                .eq(CustomerProject::getId, projectId)
                .eq(CustomerProject::getTenantId, tenantId)
                .isNull(CustomerProject::getDeletedAt)
        );
        if (project == null) {
            throw new ServiceException("项目不存在或无权访问", HttpStatus.FORBIDDEN);
        }
        return project;
    }

    private static LocalDate parseDateOrDefault(String raw, LocalDate defaultDate) {
        if (StringUtils.isBlank(raw)) {
            return defaultDate;
        }
        return LocalDate.parse(raw.trim());
    }

    private static String isoWeekLabel(LocalDate date) {
        WeekFields wf = WeekFields.ISO;
        int week = date.get(wf.weekOfWeekBasedYear());
        int year = date.get(wf.weekBasedYear());
        return String.format(Locale.ROOT, "%d-W%02d", year, week);
    }

    private static int toIntScore(BigDecimal score) {
        return score.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private static int intVal(Object v) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private static String stringVal(Object v) {
        return v != null ? String.valueOf(v) : "—";
    }

    private static String sanitizeFilename(String name) {
        if (StringUtils.isBlank(name)) {
            return "report";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "-").trim();
    }
}
