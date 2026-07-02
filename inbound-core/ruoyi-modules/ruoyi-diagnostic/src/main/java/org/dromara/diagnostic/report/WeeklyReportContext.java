package org.dromara.diagnostic.report;

import lombok.Builder;
import lombok.Data;

import org.dromara.project.report.ReportBranding;

import java.util.Map;

@Data
@Builder
public class WeeklyReportContext {

    private String projectName;

    private String brandName;

    private String period;

    private String periodStart;

    private String periodEnd;

    /** WEEKLY | MONTHLY */
    private String reportKind;

    private Map<String, Object> summary;

    private ReportBranding branding;
}
