package org.dromara.project.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * FR-005 知识库 RAG 检索预览请求。
 */
@Data
public class KnowledgeSearchBo {

    @NotBlank(message = "检索问题不能为空")
    @Size(max = 4000, message = "检索问题过长")
    private String query;

    /** 默认 3，最大 10 */
    private Integer topK;
}
