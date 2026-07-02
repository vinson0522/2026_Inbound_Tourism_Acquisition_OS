package org.dromara.diagnostic.domain.vo;

import lombok.Data;

import java.util.Map;

@Data
public class ProbePollTaskVo {

    private Long probeTaskId;

    private Long runId;

    private Long questionId;

    private String question;

    private String platform;

    private String probeMode;

    private String region;

    private String locale;

    private String market;
}
