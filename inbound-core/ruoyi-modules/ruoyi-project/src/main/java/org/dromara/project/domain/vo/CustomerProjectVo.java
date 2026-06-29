package org.dromara.project.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.project.domain.CustomerProject;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@AutoMapper(target = CustomerProject.class)
public class CustomerProjectVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String brandName;

    private String website;

    private String industry;

    private List<String> targetMarkets;

    private List<String> languages;

    private String status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
