package org.dromara.aiclient.client;

import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.aiclient.config.AiServiceProperties;
import org.dromara.aiclient.model.AiApiResponse;
import org.dromara.aiclient.model.AiHealthData;
import org.dromara.aiclient.model.ScoreRequest;
import org.dromara.aiclient.model.ScoreResultData;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Java → inbound-ai HTTP 客户端（RestTemplate；后续可换 OpenFeign 同路径）。
 */
@RequiredArgsConstructor
public class AiServiceClient {

    private final AiServiceProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * GET /health — 公开探活（无 token）。
     */
    public ResponseEntity<String> getPublicHealth() {
        String url = normalizeBaseUrl() + "/health";
        return restTemplate.getForEntity(url, String.class);
    }

    /**
     * GET /ai/health — 内网鉴权探活。
     */
    public AiApiResponse<AiHealthData> getAiHealth() {
        String url = normalizeBaseUrl() + "/ai/health";
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<AiApiResponse<AiHealthData>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    /**
     * POST /ai/score — aggregate GEO score from per-question metrics.
     */
    public AiApiResponse<ScoreResultData> score(ScoreRequest request) {
        String url = normalizeBaseUrl() + "/ai/score";
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize score request", ex);
        }
        HttpResponse response = HttpRequest.post(url)
            .header("Authorization", "Bearer " + properties.getInternalToken())
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .body(jsonBody, ContentType.JSON.getValue())
            .timeout(Math.toIntExact(properties.getReadTimeoutMs()))
            .execute();
        if (!response.isOk()) {
            throw new IllegalStateException(
                "Score request failed: HTTP " + response.getStatus() + " " + response.body()
            );
        }
        try {
            return objectMapper.readValue(
                response.body(),
                objectMapper.getTypeFactory().constructParametricType(AiApiResponse.class, ScoreResultData.class)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse score response", ex);
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getInternalToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String normalizeBaseUrl() {
        return UriComponentsBuilder.fromUriString(properties.getBaseUrl())
            .build()
            .toUriString()
            .replaceAll("/$", "");
    }
}
