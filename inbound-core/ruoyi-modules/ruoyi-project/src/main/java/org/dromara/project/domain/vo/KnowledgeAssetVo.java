package org.dromara.project.domain.vo;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class KnowledgeAssetVo {

    private Long id;

    private Long projectId;

    private String type;

    private String title;

    private String content;

    private String fileUrl;

    private List<String> tags;

    private String vectorStatus;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
