package org.dromara.aiclient.config;

import org.dromara.aiclient.client.AiServiceClient;
import org.dromara.aiclient.runner.AiServiceHealthRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

@AutoConfiguration
@EnableConfigurationProperties(AiServiceProperties.class)
public class AiClientAutoConfiguration {

    @Bean
    public RestTemplate aiRestTemplate(AiServiceProperties properties, RestTemplateBuilder builder) {
        return builder
            .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
            .readTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
            .build();
    }

    @Bean
    public AiServiceClient aiServiceClient(
        AiServiceProperties properties,
        RestTemplate aiRestTemplate,
        ObjectMapper objectMapper
    ) {
        return new AiServiceClient(properties, aiRestTemplate, objectMapper);
    }

    @Bean
    public AiServiceHealthRunner aiServiceHealthRunner(AiServiceProperties properties, AiServiceClient aiServiceClient) {
        return new AiServiceHealthRunner(properties, aiServiceClient);
    }
}
