package org.dromara.diagnostic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "inbound.report")
public class ReportExportProperties {

    /** Gotenberg base URL e.g. http://localhost:3002 — empty disables PDF export */
    private String gotenbergBaseUrl = "";
}
