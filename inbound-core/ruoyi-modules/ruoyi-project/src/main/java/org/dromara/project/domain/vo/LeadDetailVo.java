package org.dromara.project.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class LeadDetailVo extends LeadVo {

    private LocalDate travelDate;

    private Integer partySize;

    private String budget;

    private String message;

    private Map<String, Object> utm;

    private String device;

    private List<LeadFollowupVo> followups;

    /** FR-602: WhatsApp clicks on same landing page (weak association) */
    private Integer whatsappClickCount;

    private OffsetDateTime lastWhatsappClickAt;
}
