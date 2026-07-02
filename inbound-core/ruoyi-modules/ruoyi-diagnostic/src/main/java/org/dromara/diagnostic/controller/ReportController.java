package org.dromara.diagnostic.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.diagnostic.domain.bo.MonthlyReportBo;
import org.dromara.diagnostic.domain.bo.ReportQueryBo;
import org.dromara.diagnostic.domain.bo.WeeklyReportBo;
import org.dromara.diagnostic.domain.vo.ReportDetailVo;
import org.dromara.diagnostic.domain.vo.ReportVo;
import org.dromara.diagnostic.report.DiagnosticReportFile;
import org.dromara.diagnostic.service.IReportService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 报告中心 API — EPIC-8 FR-701/702
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/projects/{projectId}/reports")
public class ReportController extends BaseController {

    private final IReportService reportService;

    @SaCheckLogin
    @GetMapping
    public TableDataInfo<ReportVo> list(
        @NotNull @PathVariable Long projectId,
        ReportQueryBo bo,
        PageQuery pageQuery
    ) {
        return reportService.queryPageList(projectId, bo, pageQuery);
    }

    @SaCheckLogin
    @GetMapping("/{reportId}")
    public R<ReportDetailVo> get(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long reportId
    ) {
        return R.ok(reportService.queryById(projectId, reportId));
    }

    @SaCheckLogin
    @Log(title = "增长周报", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/weekly")
    public R<Long> createWeekly(
        @NotNull @PathVariable Long projectId,
        @RequestBody(required = false) WeeklyReportBo bo
    ) {
        return R.ok(reportService.createWeeklyReport(projectId, bo));
    }

    @SaCheckLogin
    @Log(title = "增长月报", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/monthly")
    public R<Long> createMonthly(
        @NotNull @PathVariable Long projectId,
        @RequestBody(required = false) MonthlyReportBo bo
    ) {
        return R.ok(reportService.createMonthlyReport(projectId, bo));
    }

    @SaCheckLogin
    @Log(title = "报告导出", businessType = BusinessType.EXPORT)
    @GetMapping("/{reportId}/export")
    public void exportReport(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long reportId,
        @RequestParam(defaultValue = "docx") String format,
        HttpServletResponse response
    ) throws IOException {
        DiagnosticReportFile file = reportService.exportReport(projectId, reportId, format);
        response.setContentType(file.getContentType());
        String encoded = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        response.getOutputStream().write(file.getContent());
    }
}
