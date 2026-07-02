package org.dromara.project.mq;

import lombok.RequiredArgsConstructor;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiBreakdownPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(
        String traceId,
        Long breakdownId,
        Long materialId,
        Long tenantId,
        Long projectId,
        String sourceUrl
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("trace_id", traceId);
        payload.put("breakdownId", breakdownId);
        payload.put("materialId", materialId);
        payload.put("tenantId", tenantId);
        payload.put("projectId", projectId);
        payload.put("sourceUrl", sourceUrl);
        rabbitTemplate.convertAndSend(AiBreakdownQueueConstants.AI_BREAKDOWN, JsonUtils.toJsonString(payload));
    }
}
