package org.dromara.diagnostic.report;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.diagnostic.config.ReportExportProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GotenbergClient {

    private static final int HEALTH_TIMEOUT_MS = 3_000;
    private static final int CONVERT_TIMEOUT_MS = 120_000;

    private final ReportExportProperties properties;

    public boolean isEnabled() {
        return StringUtils.isNotBlank(properties.getGotenbergBaseUrl());
    }

    /** GET /health — false when URL unset or service unreachable */
    public boolean isReachable() {
        if (!isEnabled()) {
            return false;
        }
        String base = normalizeBaseUrl();
        try (HttpResponse resp = HttpRequest.get(base + "/health").timeout(HEALTH_TIMEOUT_MS).execute()) {
            return resp.isOk();
        } catch (Exception e) {
            log.debug("Gotenberg health check failed: {}", e.getMessage());
            return false;
        }
    }

    public byte[] htmlToPdf(byte[] htmlBytes, String filename) {
        String url = normalizeBaseUrl() + "/forms/chromium/convert/html";
        try (HttpResponse resp = HttpRequest.post(url)
            .form("files", htmlBytes, filename)
            .timeout(CONVERT_TIMEOUT_MS)
            .execute()) {
            if (!resp.isOk()) {
                throw new IllegalStateException("Gotenberg HTTP " + resp.getStatus() + ": " + resp.body());
            }
            byte[] body = resp.bodyBytes();
            if (body == null || body.length < 5 || body[0] != '%') {
                throw new IllegalStateException("Gotenberg returned non-PDF body");
            }
            return body;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gotenberg convert failed: {}", e.getMessage());
            throw new IllegalStateException("PDF conversion failed: " + e.getMessage(), e);
        }
    }

    private String normalizeBaseUrl() {
        return properties.getGotenbergBaseUrl().replaceAll("/$", "");
    }
}
