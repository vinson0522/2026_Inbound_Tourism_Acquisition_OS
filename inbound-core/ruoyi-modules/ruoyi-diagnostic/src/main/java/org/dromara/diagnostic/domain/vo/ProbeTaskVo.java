package org.dromara.diagnostic.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.diagnostic.domain.ProbeTask;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@AutoMapper(target = ProbeTask.class)
public class ProbeTaskVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long runId;

    private Long questionId;

    private String platform;

    private String probeMode;

    private Long probeNodeId;

    private String status;

    private Integer retryCount;

    private String errorMessage;

    private OffsetDateTime dispatchedAt;

    private OffsetDateTime finishedAt;

    private OffsetDateTime createdAt;
}
