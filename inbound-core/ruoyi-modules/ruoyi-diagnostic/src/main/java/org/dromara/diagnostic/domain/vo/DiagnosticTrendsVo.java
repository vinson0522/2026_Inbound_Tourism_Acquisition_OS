package org.dromara.diagnostic.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * FR-108 GET .../diagnostics/trends 响应体。
 */
@Data
public class DiagnosticTrendsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<DiagnosticTrendVo> runs = new ArrayList<>();
}
