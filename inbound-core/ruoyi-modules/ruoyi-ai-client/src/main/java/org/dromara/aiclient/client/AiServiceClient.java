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
import org.dromara.aiclient.model.ContentGenerateData;
import org.dromara.aiclient.model.ContentGenerateRequest;
import org.dromara.aiclient.model.LandingGenerateData;
import org.dromara.aiclient.model.LandingGenerateRequest;
import org.dromara.aiclient.model.KeywordGenerateData;
import org.dromara.aiclient.model.KeywordGenerateRequest;
import org.dromara.aiclient.model.RagSearchData;
import org.dromara.aiclient.model.RagSearchRequest;
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

    /**
     * POST /ai/rag/search — tenant + project scoped vector retrieval.
     */
    public AiApiResponse<RagSearchData> ragSearch(RagSearchRequest request) {
        String url = normalizeBaseUrl() + "/ai/rag/search";
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize rag search request", ex);
        }
        HttpResponse response = HttpRequest.post(url)
            .header("Authorization", "Bearer " + properties.getInternalToken())
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .body(jsonBody, ContentType.JSON.getValue())
            .timeout(Math.toIntExact(properties.getReadTimeoutMs()))
            .execute();
        if (!response.isOk()) {
            throw new IllegalStateException(
                "RAG search failed: HTTP " + response.getStatus() + " " + response.body()
            );
        }
        try {
            return objectMapper.readValue(
                response.body(),
                objectMapper.getTypeFactory().constructParametricType(AiApiResponse.class, RagSearchData.class)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse rag search response", ex);
        }
    }

    /**
     * POST /ai/keywords/generate — FR-201 keyword opportunity generation.
     */
    public AiApiResponse<KeywordGenerateData> keywordsGenerate(KeywordGenerateRequest request) {
        String url = normalizeBaseUrl() + "/ai/keywords/generate";
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize keywords generate request", ex);
        }
        int timeoutMs = Math.max(Math.toIntExact(properties.getReadTimeoutMs()), 120_000);
        HttpResponse response = HttpRequest.post(url)
            .header("Authorization", "Bearer " + properties.getInternalToken())
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .body(jsonBody, ContentType.JSON.getValue())
            .timeout(timeoutMs)
            .execute();
        if (!response.isOk()) {
            throw new IllegalStateException(
                "Keywords generate failed: HTTP " + response.getStatus() + " " + response.body()
            );
        }
        try {
            return objectMapper.readValue(
                response.body(),
                objectMapper.getTypeFactory().constructParametricType(AiApiResponse.class, KeywordGenerateData.class)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse keywords generate response", ex);
        }
    }

    /**
     * POST /ai/content/generate — FR-301/302 content script generation.
     */
    public AiApiResponse<ContentGenerateData> contentGenerate(ContentGenerateRequest request) {
        String url = normalizeBaseUrl() + "/ai/content/generate";
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize content generate request", ex);
        }
        int timeoutMs = Math.max(Math.toIntExact(properties.getReadTimeoutMs()), 120_000);
        HttpResponse response = HttpRequest.post(url)
            .header("Authorization", "Bearer " + properties.getInternalToken())
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .body(jsonBody, ContentType.JSON.getValue())
            .timeout(timeoutMs)
            .execute();
        if (!response.isOk()) {
            throw new IllegalStateException(
                "Content generate failed: HTTP " + response.getStatus() + " " + response.body()
            );
        }
        try {
            return objectMapper.readValue(
                response.body(),
                objectMapper.getTypeFactory().constructParametricType(AiApiResponse.class, ContentGenerateData.class)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse content generate response", ex);
        }
    }

    /**
     * POST /ai/landing/generate — FR-502~505 landing page generation.
     */
    public AiApiResponse<LandingGenerateData> landingGenerate(LandingGenerateRequest request) {
        String url = normalizeBaseUrl() + "/ai/landing/generate";
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize landing generate request", ex);
        }
        int timeoutMs = Math.max(Math.toIntExact(properties.getReadTimeoutMs()), 120_000);
        HttpResponse response = HttpRequest.post(url)
            .header("Authorization", "Bearer " + properties.getInternalToken())
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .body(jsonBody, ContentType.JSON.getValue())
            .timeout(timeoutMs)
            .execute();
        if (!response.isOk()) {
            throw new IllegalStateException(
                "Landing generate failed: HTTP " + response.getStatus() + " " + response.body()
            );
        }
        try {
            return objectMapper.readValue(
                response.body(),
                objectMapper.getTypeFactory().constructParametricType(AiApiResponse.class, LandingGenerateData.class)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse landing generate response", ex);
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
