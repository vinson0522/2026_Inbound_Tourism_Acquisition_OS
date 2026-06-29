package org.dromara.diagnostic.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.diagnostic.domain.bo.CreateDiagnosticBo;
import org.dromara.diagnostic.domain.vo.DiagnosticResultVo;
import org.dromara.diagnostic.domain.vo.DiagnosticRunVo;
import org.dromara.diagnostic.domain.vo.DiagnosticTrendsVo;
import org.dromara.diagnostic.domain.vo.ProbeTaskVo;
import org.dromara.diagnostic.service.IDiagnosticRunService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.diagnostic.report.DiagnosticReportFile;
import org.dromara.diagnostic.service.IDiagnosticReportExportService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * GEO 诊断 API — EPIC-2 FR-103~105
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class DiagnosticController extends BaseController {

    private final IDiagnosticRunService diagnosticRunService;
    private final IDiagnosticReportExportService diagnosticReportExportService;

    @SaCheckLogin
    @Log(title = "GEO诊断", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/projects/{projectId}/diagnostics")
    public R<Long> create(
        @NotNull @PathVariable Long projectId,
        @Validated @RequestBody CreateDiagnosticBo bo
    ) {
        return R.ok(diagnosticRunService.createRun(projectId, bo));
    }

    @SaCheckLogin
    @GetMapping("/projects/{projectId}/diagnostics")
    public TableDataInfo<DiagnosticRunVo> list(
        @NotNull @PathVariable Long projectId,
        PageQuery pageQuery
    ) {
        return diagnosticRunService.queryPageList(projectId, pageQuery);
    }

    /** FR-108 诊断趋势序列（按 finished_at ASC） */
    @SaCheckLogin
    @GetMapping("/projects/{projectId}/diagnostics/trends")
    public R<DiagnosticTrendsVo> trends(
        @NotNull @PathVariable Long projectId,
        @RequestParam(defaultValue = "12") int limit,
        @RequestParam(required = false) String market
    ) {
        return R.ok(diagnosticRunService.queryTrends(projectId, limit, market));
    }

    @SaCheckLogin
    @GetMapping("/diagnostics/{runId}")
    public R<DiagnosticRunVo> getInfo(@NotNull @PathVariable Long runId) {
        return R.ok(diagnosticRunService.queryById(runId));
    }

    @SaCheckLogin
    @GetMapping("/diagnostics/{runId}/results")
    public R<List<DiagnosticResultVo>> results(@NotNull @PathVariable Long runId) {
        return R.ok(diagnosticRunService.queryResults(runId));
    }

    @SaCheckLogin
    @GetMapping("/diagnostics/{runId}/probe-tasks")
    public R<List<ProbeTaskVo>> probeTasks(@NotNull @PathVariable Long runId) {
        return R.ok(diagnosticRunService.queryProbeTasks(runId));
    }

    /** FR-106 诊断报告导出（DOCX / PDF via Gotenberg） */
    @SaCheckLogin
    @Log(title = "GEO诊断报告", businessType = BusinessType.EXPORT)
    @GetMapping("/diagnostics/{runId}/report")
    public void exportReport(
        @NotNull @PathVariable Long runId,
        @RequestParam(defaultValue = "docx") String format,
        HttpServletResponse response
    ) throws IOException {
        DiagnosticReportFile file = diagnosticReportExportService.exportReport(runId, format);
        response.setContentType(file.getContentType());
        String encoded = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        response.getOutputStream().write(file.getContent());
    }
}
