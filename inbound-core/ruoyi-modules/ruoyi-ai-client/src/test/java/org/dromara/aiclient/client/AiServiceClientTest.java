package org.dromara.aiclient.client;

import org.dromara.aiclient.config.AiServiceProperties;
import org.dromara.aiclient.model.AiApiResponse;
import org.dromara.aiclient.model.AiHealthData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiServiceClientTest {

    private MockRestServiceServer server;
    private AiServiceClient client;

    @BeforeEach
    void setUp() {
        AiServiceProperties properties = new AiServiceProperties();
        properties.setBaseUrl("http://127.0.0.1:18090");
        properties.setInternalToken("dev_internal_token_change_me");

        RestTemplate restTemplate = new RestTemplateBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .readTimeout(Duration.ofSeconds(2))
            .build();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new AiServiceClient(properties, restTemplate, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @AfterEach
    void verify() {
        server.verify();
    }

    @Test
    void getAiHealth_sendsBearerTokenAndParsesResponse() {
        String body = """
            {"code":0,"message":"ok","data":{"status":"ok","litellm":"no_key","db":"skipped"},"trace_id":"t-1"}
            """;
        server.expect(requestTo("http://127.0.0.1:18090/ai/health"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer dev_internal_token_change_me"))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        AiApiResponse<AiHealthData> response = client.getAiHealth();

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getData().getStatus()).isEqualTo("ok");
        assertThat(response.getData().getLitellm()).isEqualTo("no_key");
        assertThat(response.getTraceId()).isEqualTo("t-1");
    }
}
