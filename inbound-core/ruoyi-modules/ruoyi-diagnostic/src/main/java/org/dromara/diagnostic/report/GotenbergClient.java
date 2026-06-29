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

    private final ReportExportProperties properties;

    public boolean isEnabled() {
        return StringUtils.isNotBlank(properties.getGotenbergBaseUrl());
    }

    public byte[] htmlToPdf(byte[] htmlBytes, String filename) {
        String base = properties.getGotenbergBaseUrl().replaceAll("/$", "");
        String url = base + "/forms/chromium/convert/html";
        try (HttpResponse resp = HttpRequest.post(url)
            .form("files", htmlBytes, filename)
            .timeout(120_000)
            .execute()) {
            if (!resp.isOk()) {
                throw new IllegalStateException("Gotenberg HTTP " + resp.getStatus() + ": " + resp.body());
            }
            return resp.bodyBytes();
        } catch (Exception e) {
            log.error("Gotenberg convert failed: {}", e.getMessage());
            throw new IllegalStateException("PDF conversion failed: " + e.getMessage(), e);
        }
    }
}
