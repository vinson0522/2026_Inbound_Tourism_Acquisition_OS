package org.dromara.diagnostic.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.diagnostic.domain.DiagnosticRun;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@AutoMapper(target = DiagnosticRun.class)
public class DiagnosticRunVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long projectId;

    private String name;

    private String market;

    private String locale;

    private String region;

    private List<String> probeModes;

    private BigDecimal calibrationRatio;

    private List<String> models;

    private Integer sampleCount;

    private Map<String, Object> questionScope;

    private String status;

    private BigDecimal geoScore;

    /** 0–100 进度（probe_task 终态占比） */
    private Integer progress;

    private OffsetDateTime startedAt;

    private OffsetDateTime finishedAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
