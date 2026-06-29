package org.dromara.project.domain.vo;

import lombok.Data;

/** FR-201 生成结果摘要。 */
@Data
public class KeywordGenerateVo {

    private Integer insertedCount;

    private Boolean needsHumanReview;

    private String captureMethod;
}
