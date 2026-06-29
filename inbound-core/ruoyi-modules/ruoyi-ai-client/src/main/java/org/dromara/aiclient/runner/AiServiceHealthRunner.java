package org.dromara.aiclient.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.aiclient.client.AiServiceClient;
import org.dromara.aiclient.config.AiServiceProperties;
import org.dromara.aiclient.model.AiApiResponse;
import org.dromara.aiclient.model.AiHealthData;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 启动后探测 inbound-ai {@code GET /ai/health}（失败 warn，不抛异常阻断启动）。
 */
@Slf4j
@RequiredArgsConstructor
public class AiServiceHealthRunner implements ApplicationRunner {

    private final AiServiceProperties properties;
    private final AiServiceClient aiServiceClient;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isHealthCheckOnStartup()) {
            log.info("Inbound AI 启动探活已关闭 (ai.service.health-check-on-startup=false)");
            return;
        }
        String baseUrl = properties.getBaseUrl();
        try {
            AiApiResponse<AiHealthData> body = aiServiceClient.getAiHealth();
            if (body != null && body.getCode() != null && body.getCode() == 0
                && body.getData() != null && "ok".equalsIgnoreCase(body.getData().getStatus())) {
                log.info(
                    "Inbound AI 服务连通正常: baseUrl={}, litellm={}, db={}, traceId={}",
                    baseUrl,
                    body.getData().getLitellm(),
                    body.getData().getDb(),
                    body.getTraceId()
                );
            } else {
                log.warn(
                    "Inbound AI 健康检查响应异常: baseUrl={}, body={}",
                    baseUrl,
                    body
                );
            }
        } catch (Exception ex) {
            log.warn(
                "Inbound AI 服务暂不可达 ({}): {} — 请先启动 inbound-ai 或检查 AI_SERVICE_BASE_URL / AI_SERVICE_INTERNAL_TOKEN",
                baseUrl,
                ex.getMessage()
            );
        }
    }
}
