package org.dromara.diagnostic.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.diagnostic.domain.Report;
import org.dromara.diagnostic.domain.vo.DiagnosticResultVo;
import org.dromara.diagnostic.domain.vo.DiagnosticRunVo;
import org.dromara.diagnostic.domain.vo.ProbeTaskVo;
import org.dromara.diagnostic.mapper.ReportMapper;
import org.dromara.diagnostic.report.DiagnosticDocxReportRenderer;
import org.dromara.diagnostic.report.DiagnosticHtmlReportRenderer;
import org.dromara.diagnostic.report.DiagnosticReportContext;
import org.dromara.diagnostic.report.DiagnosticReportFile;
import org.dromara.diagnostic.report.GotenbergClient;
import org.dromara.diagnostic.service.IDiagnosticReportExportService;
import org.dromara.diagnostic.service.IDiagnosticRunService;
import org.dromara.diagnostic.support.BusinessTenantHelper;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosticReportExportServiceImpl implements IDiagnosticReportExportService {

    private static final Set<String> EXPORTABLE = Set.of("SUCCESS", "PARTIAL_FAILED");

    private final IDiagnosticRunService diagnosticRunService;
    private final CustomerProjectMapper customerProjectMapper;
    private final ReportMapper reportMapper;
    private final GotenbergClient gotenbergClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiagnosticReportFile exportReport(Long runId, String format) {
        String fmt = StringUtils.blankToDefault(format, "docx").toLowerCase(Locale.ROOT);
        if (!Set.of("docx", "pdf").contains(fmt)) {
            throw new ServiceException("Unsupported format: " + format, HttpStatus.BAD_REQUEST);
        }

        DiagnosticRunVo run = diagnosticRunService.queryById(runId);
        if (!EXPORTABLE.contains(run.getStatus())) {
            throw new ServiceException("仅 SUCCESS / PARTIAL_FAILED 诊断可导出报告", HttpStatus.BAD_REQUEST);
        }

        List<DiagnosticResultVo> results = diagnosticRunService.queryResults(runId);
        List<ProbeTaskVo> tasks = diagnosticRunService.queryProbeTasks(runId);
        CustomerProject project = customerProjectMapper.selectById(run.getProjectId());
        DiagnosticReportContext ctx = buildContext(run, results, tasks, project);

        Long reportId = insertReportRecord(run, ctx, fmt);
        log.info("FR-106 report export runId={} reportId={} format={}", runId, reportId, fmt);

        String safeName = sanitizeFilename(run.getName());
        try {
            if ("pdf".equals(fmt)) {
                if (!gotenbergClient.isEnabled()) {
                    throw new ServiceException(
                        "PDF 导出需要 Gotenberg。本地请 docker compose --profile full up gotenberg 并设置 GOTENBERG_BASE_URL=http://localhost:3002",
                        HttpStatus.BAD_REQUEST
                    );
                }
                String html = DiagnosticHtmlReportRenderer.render(ctx);
                byte[] pdf = gotenbergClient.htmlToPdf(html.getBytes(StandardCharsets.UTF_8), "index.html");
                return DiagnosticReportFile.builder()
                    .content(pdf)
                    .filename(safeName + "-geo-report-" + runId + ".pdf")
                    .contentType("application/pdf")
                    .build();
            }

            byte[] docx = DiagnosticDocxReportRenderer.render(ctx);
            return DiagnosticReportFile.builder()
                .content(docx)
                .filename(safeName + "-geo-report-" + runId + ".docx")
                .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("报告生成失败: " + e.getMessage(), HttpStatus.ERROR);
        }
    }

    private Long insertReportRecord(DiagnosticRunVo run, DiagnosticReportContext ctx, String format) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("runId", run.getId());
        summary.put("format", format);
        summary.put("geoScore", run.getGeoScore());
        summary.put("probe_mode", ctx.getProbeModesLabel());
        summary.put("sampled_at", ctx.getSampledAt());
        summary.put("region", run.getRegion());
        summary.put("platforms", ctx.getPlatforms());

        Report report = new Report();
        report.setTenantId(BusinessTenantHelper.getBusinessTenantId());
        report.setProjectId(run.getProjectId());
        report.setType("DIAGNOSTIC");
        report.setPeriod(String.valueOf(run.getId()));
        report.setSummary(JsonUtils.toJsonString(summary));
        report.setCreatedAt(OffsetDateTime.now());
        report.setUpdatedAt(OffsetDateTime.now());
        report.setCreatedBy(LoginHelper.getUserId());
        reportMapper.insert(report);
        return report.getId();
    }

    private DiagnosticReportContext buildContext(
        DiagnosticRunVo run,
        List<DiagnosticResultVo> results,
        List<ProbeTaskVo> tasks,
        CustomerProject project
    ) {
        Set<String> platforms = new LinkedHashSet<>();
        tasks.forEach(t -> {
            if (StringUtils.isNotBlank(t.getPlatform())) {
                platforms.add(t.getPlatform());
            }
        });
        results.forEach(r -> {
            if (StringUtils.isNotBlank(r.getPlatform())) {
                platforms.add(r.getPlatform());
            }
        });
        if (platforms.isEmpty() && run.getModels() != null) {
            platforms.addAll(run.getModels());
        }

        OffsetDateTime sampledAt = results.stream()
            .map(DiagnosticResultVo::getSampledAt)
            .filter(java.util.Objects::nonNull)
            .min(OffsetDateTime::compareTo)
            .orElse(run.getFinishedAt() != null ? run.getFinishedAt() : run.getStartedAt());

        String probeModes = run.getProbeModes() != null && !run.getProbeModes().isEmpty()
            ? String.join(", ", run.getProbeModes())
            : "grounded-api";

        return DiagnosticReportContext.builder()
            .run(run)
            .projectName(project != null ? project.getName() : "—")
            .brandName(project != null ? StringUtils.blankToDefault(project.getBrandName(), "—") : "—")
            .results(results)
            .probeTasks(tasks)
            .platforms(String.join(", ", platforms))
            .sampledAt(sampledAt)
            .probeModesLabel(probeModes)
            .build();
    }

    private static String sanitizeFilename(String name) {
        if (StringUtils.isBlank(name)) {
            return "diagnostic";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "-").trim();
    }
}
