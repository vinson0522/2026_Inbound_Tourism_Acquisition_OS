package org.dromara.project.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * FR-005 RAG 检索命中切片。
 */
@Data
public class KnowledgeRagHitVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long chunkId;

    private Long assetId;

    private Integer chunkIndex;

    private String chunkText;

    private Double score;
}
