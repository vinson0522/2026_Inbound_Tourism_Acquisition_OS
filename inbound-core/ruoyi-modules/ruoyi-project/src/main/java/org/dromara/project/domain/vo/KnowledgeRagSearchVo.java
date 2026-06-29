package org.dromara.project.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * FR-005 RAG 检索响应。
 */
@Data
public class KnowledgeRagSearchVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<KnowledgeRagHitVo> hits = new ArrayList<>();
}
