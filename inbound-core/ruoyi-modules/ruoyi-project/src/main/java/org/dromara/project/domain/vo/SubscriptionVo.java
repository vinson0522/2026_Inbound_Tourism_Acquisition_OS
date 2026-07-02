package org.dromara.project.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SubscriptionVo {

    private String planCode;

    private String planName;

    private String status;

    private String periodStart;

    private String periodEnd;

    private Integer daysRemaining;

    private List<QuotaItemVo> quotas = new ArrayList<>();

    private boolean hasOverage;

    private boolean hasWarning;

    private List<String> overageKeys = new ArrayList<>();
}
