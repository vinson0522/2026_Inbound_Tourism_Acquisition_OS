package org.dromara.project.domain.vo;

import lombok.Data;

@Data
public class QuotaItemVo {

    private String key;

    private String label;

    private int used;

    private int limit;

    private String unit;

    /** total | monthly */
    private String period;

    private int percentage;

    /** normal | warning | overage */
    private String status;
}
