package org.dromara.project.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "inbound.turnstile")
public class TurnstileProperties {

    /** When true and secret-key is set, require X-Turnstile-Token (M1 stub only logs). */
    private boolean enabled = false;

    /** Cloudflare Turnstile secret; empty → skip verification with warn log. */
    private String secretKey = "";
}
