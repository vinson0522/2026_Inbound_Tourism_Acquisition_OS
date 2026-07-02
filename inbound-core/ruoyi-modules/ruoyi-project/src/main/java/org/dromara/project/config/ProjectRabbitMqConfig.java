package org.dromara.project.config;

import org.dromara.project.mq.AiBreakdownQueueConstants;
import org.dromara.project.mq.AiEmbedQueueConstants;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectRabbitMqConfig {

    @Bean
    public Queue aiEmbedQueue() {
        return QueueBuilder.durable(AiEmbedQueueConstants.AI_EMBED).build();
    }

    @Bean
    public Queue aiBreakdownQueue() {
        return QueueBuilder.durable(AiBreakdownQueueConstants.AI_BREAKDOWN).build();
    }
}
