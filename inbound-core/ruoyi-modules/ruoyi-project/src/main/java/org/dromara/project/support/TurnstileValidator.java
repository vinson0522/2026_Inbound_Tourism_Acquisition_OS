package org.dromara.project.support;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.project.config.TurnstileProperties;
import org.springframework.stereotype.Component;

/**
 * Turnstile verification — M1 skip when secret absent; M2 POST Cloudflare siteverify when configured.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TurnstileValidator {

    private static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final int TIMEOUT_MS = 10_000;

    private final TurnstileProperties properties;

    public void verifyOrSkip(String turnstileToken) {
        if (!properties.isEnabled() && StringUtils.isBlank(properties.getSecretKey())) {
            return;
        }
        if (StringUtils.isBlank(properties.getSecretKey())) {
            log.warn("Turnstile enabled but inbound.turnstile.secret-key is empty; skipping verification");
            return;
        }
        if (StringUtils.isBlank(turnstileToken)) {
            throw new ServiceException("Turnstile token required", 400);
        }
        verifyWithCloudflare(turnstileToken);
    }

    private void verifyWithCloudflare(String turnstileToken) {
        try (HttpResponse resp = HttpRequest.post(SITEVERIFY_URL)
            .form("secret", properties.getSecretKey())
            .form("response", turnstileToken)
            .timeout(TIMEOUT_MS)
            .execute()) {
            if (!resp.isOk()) {
                log.warn("Turnstile siteverify HTTP {}: {}", resp.getStatus(), resp.body());
                throw new ServiceException("Turnstile verification failed", 400);
            }
            JSONObject body = JSONUtil.parseObj(resp.body());
            if (!body.getBool("success", false)) {
                Object errors = body.get("error-codes");
                log.warn("Turnstile siteverify rejected: {}", errors);
                throw new ServiceException("Turnstile verification failed", 400);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Turnstile siteverify error: {}", e.getMessage());
            throw new ServiceException("Turnstile verification failed", 400);
        }
    }
}
