package org.dromara.diagnostic.config;

import org.dromara.diagnostic.mq.DiagQueueConstants;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ producer config for GEO grounded-api probe tasks.
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue diagGroundedApiQueue() {
        return QueueBuilder.durable(DiagQueueConstants.GROUNDED_API).build();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        return template;
    }
}
