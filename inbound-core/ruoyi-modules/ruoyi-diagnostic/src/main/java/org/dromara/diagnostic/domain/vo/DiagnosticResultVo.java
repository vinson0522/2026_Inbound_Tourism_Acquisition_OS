package org.dromara.diagnostic.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.diagnostic.domain.DiagnosticResult;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@AutoMapper(target = DiagnosticResult.class)
public class DiagnosticResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long runId;

    private Long questionId;

    private String platform;

    private String probeMode;

    private Long probeNodeId;

    private String model;

    private String answerText;

    private List<String> mentionedBrands;

    private List<String> competitors;

    private List<String> links;

    private String citationsJson;

    private String captureMethod;

    private Map<String, Object> rawResponseJson;

    private String screenshotUrl;

    private Integer rank;

    private Map<String, Object> scoreJson;

    private Boolean humanCorrected;

    private OffsetDateTime sampledAt;

    private OffsetDateTime createdAt;
}
