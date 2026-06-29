package org.dromara.project.mq;

import lombok.RequiredArgsConstructor;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiEmbedPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(String traceId, Long assetId, Long tenantId, Long projectId, String fileUrl) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("trace_id", traceId);
        payload.put("assetId", assetId);
        payload.put("tenantId", tenantId);
        payload.put("projectId", projectId);
        payload.put("fileUrl", fileUrl);
        rabbitTemplate.convertAndSend(AiEmbedQueueConstants.AI_EMBED, JsonUtils.toJsonString(payload));
    }
}
