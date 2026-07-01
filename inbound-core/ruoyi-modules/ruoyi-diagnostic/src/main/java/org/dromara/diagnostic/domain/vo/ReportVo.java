package org.dromara.diagnostic.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 报告列表行 — 对齐 inbound-admin ReportVo
 */
@Data
public class ReportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long projectId;

    private String type;

    private String period;

    /** 列表摘要一行文案 */
    private String summaryPreview;

    private OffsetDateTime createdAt;
}
