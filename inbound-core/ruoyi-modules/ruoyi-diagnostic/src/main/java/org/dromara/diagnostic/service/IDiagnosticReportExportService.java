package org.dromara.diagnostic.service;

import org.dromara.diagnostic.report.DiagnosticReportFile;

public interface IDiagnosticReportExportService {

    /**
     * FR-106: export diagnostic run report (docx or pdf).
     */
    DiagnosticReportFile exportReport(Long runId, String format);
}
