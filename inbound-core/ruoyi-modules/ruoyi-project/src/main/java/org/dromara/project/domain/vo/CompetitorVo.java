package org.dromara.project.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.project.domain.Competitor;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@AutoMapper(target = Competitor.class)
public class CompetitorVo {

    private Long id;

    private Long projectId;

    private String name;

    private String website;

    private Map<String, Object> socialLinks;

    private String mainProducts;

    private String notes;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
