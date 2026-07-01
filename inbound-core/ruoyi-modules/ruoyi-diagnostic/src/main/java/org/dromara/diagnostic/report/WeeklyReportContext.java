package org.dromara.diagnostic.report;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class WeeklyReportContext {

    private String projectName;

    private String brandName;

    private String period;

    private String periodStart;

    private String periodEnd;

    private Map<String, Object> summary;
}
