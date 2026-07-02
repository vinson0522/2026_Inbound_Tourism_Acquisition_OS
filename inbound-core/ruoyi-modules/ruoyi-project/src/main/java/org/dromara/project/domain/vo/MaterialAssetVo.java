package org.dromara.project.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.project.domain.MaterialAsset;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AutoMapper(target = MaterialAsset.class)
public class MaterialAssetVo {

    private Long id;

    private Long projectId;

    private String type;

    private String url;

    private String thumbnailUrl;

    private List<String> tags;

    private String copyrightStatus;

    private String source;

    private String fileName;

    private Long breakdownId;

    private String breakdownStatus;

    private Integer frameCount;

    private Boolean needsHumanReview;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
