package org.dromara.diagnostic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "inbound.probe")
public class ProbeProperties {

    /** Heartbeat within this window marks node online (seconds). */
    private int onlineWithinSeconds = 60;

    /** M1 dev whitelist; empty = allow any node_key that registers under resolved tenant. */
    private List<String> allowedNodeKeys = new ArrayList<>(List.of("demo-probe-1"));

    public static final String NODE_KEY_HEADER = "X-Probe-Node-Key";
}
