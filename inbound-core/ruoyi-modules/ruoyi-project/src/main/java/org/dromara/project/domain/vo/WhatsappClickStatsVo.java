package org.dromara.project.domain.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class WhatsappClickStatsVo {

    private Long clickCount;

    private OffsetDateTime lastClickAt;
}
