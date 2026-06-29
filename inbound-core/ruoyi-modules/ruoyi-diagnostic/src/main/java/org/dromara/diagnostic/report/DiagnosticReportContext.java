package org.dromara.diagnostic.report;

import lombok.Builder;
import lombok.Data;
import org.dromara.diagnostic.domain.vo.DiagnosticResultVo;
import org.dromara.diagnostic.domain.vo.DiagnosticRunVo;
import org.dromara.diagnostic.domain.vo.ProbeTaskVo;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class DiagnosticReportContext {

    private DiagnosticRunVo run;
    private String projectName;
    private String brandName;
    private List<DiagnosticResultVo> results;
    private List<ProbeTaskVo> probeTasks;

    /** Comma-separated platforms from probe tasks / results */
    private String platforms;

    /** Representative sample timestamp for compliance footer */
    private OffsetDateTime sampledAt;

    private String probeModesLabel;
}
