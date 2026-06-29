package org.dromara.diagnostic.mq;

import lombok.RequiredArgsConstructor;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Publishes grounded-api probe jobs to {@link DiagQueueConstants#GROUNDED_API}.
 */
@Component
@RequiredArgsConstructor
public class DiagGroundedApiPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(
        String traceId,
        Long runId,
        Long probeTaskId,
        Long questionId,
        Long tenantId,
        Long projectId,
        String platform,
        String probeMode,
        String model,
        String region,
        String locale,
        int sampleIndex,
        String question,
        String customerBrand,
        List<String> competitorBrands
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("trace_id", traceId);
        payload.put("runId", runId);
        payload.put("probeTaskId", probeTaskId);
        payload.put("questionId", questionId);
        payload.put("tenantId", tenantId);
        payload.put("projectId", projectId);
        payload.put("platform", platform);
        payload.put("probe_mode", probeMode);
        payload.put("grounding_enabled", true);
        payload.put("model", model);
        payload.put("region", region);
        payload.put("locale", locale);
        payload.put("sampleIndex", sampleIndex);
        payload.put("question", question);
        payload.put("customerBrand", customerBrand);
        payload.put("competitorBrands", competitorBrands);
        rabbitTemplate.convertAndSend(DiagQueueConstants.GROUNDED_API, JsonUtils.toJsonString(payload));
    }
}
