package org.dromara.project.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.project.domain.TravelProduct;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AutoMapper(target = TravelProduct.class)
public class TravelProductVo {

    private Long id;

    private Long projectId;

    private String name;

    private List<String> destinations;

    private Integer days;

    private String priceRange;

    private String suitableFor;

    private String highlights;

    private String inclusions;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
