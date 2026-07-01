package org.dromara.project.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContentTaskDetailVo extends ContentTaskVo {

    private GeneratedContentVo generatedContent;
}
