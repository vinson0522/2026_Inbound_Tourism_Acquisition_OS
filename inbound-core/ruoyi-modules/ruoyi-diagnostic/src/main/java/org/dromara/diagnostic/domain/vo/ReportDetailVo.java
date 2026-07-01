package org.dromara.diagnostic.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 报告详情 — summary 为 JSON 对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReportDetailVo extends ReportVo {

    private Map<String, Object> summary;
}
