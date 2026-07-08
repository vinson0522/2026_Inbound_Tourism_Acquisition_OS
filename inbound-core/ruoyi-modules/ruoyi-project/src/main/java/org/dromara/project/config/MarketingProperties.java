package org.dromara.project.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Marketing-portal contact inquiries — routed to a fixed platform tenant/project.
 * These leads are inquiries about the TourGEO product itself, not a customer landing page.
 */
@Data
@Component
@ConfigurationProperties(prefix = "inbound.marketing")
public class MarketingProperties {

    /** Tenant that owns marketing-portal inquiries. */
    private Long tenantId = 1L;

    /** Project that owns marketing-portal inquiries. */
    private Long projectId = 1L;
}
