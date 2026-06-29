package org.dromara.aiclient.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * inbound-ai 内网调用配置（与 deploy/.env、inbound-ai/.env.example 对齐）。
 */
@Data
@ConfigurationProperties(prefix = "ai.service")
public class AiServiceProperties {

    /**
     * 环境变量 {@code AI_SERVICE_BASE_URL}，默认本机 uvicorn。
     */
    private String baseUrl = "http://localhost:8090";

    /**
     * 环境变量 {@code AI_SERVICE_INTERNAL_TOKEN}，须与 inbound-ai 一致。
     */
    private String internalToken = "dev_internal_token_change_me";

    /**
     * 启动时探测 {@code GET /ai/health}（失败仅 warn，不阻断启动）。
     */
    private boolean healthCheckOnStartup = true;

    /** 连接/读取超时（毫秒） */
    private int connectTimeoutMs = 5000;

    private int readTimeoutMs = 15000;
}
