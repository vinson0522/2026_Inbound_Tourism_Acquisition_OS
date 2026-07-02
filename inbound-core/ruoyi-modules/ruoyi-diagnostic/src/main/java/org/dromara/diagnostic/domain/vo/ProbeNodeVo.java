package org.dromara.diagnostic.domain.vo;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ProbeNodeVo {

    private Long id;

    private String nodeKey;

    private String region;

    private List<String> platforms;

    private String extensionVersion;

    private String status;

    private OffsetDateTime lastHeartbeatAt;

    private boolean online;
}
