package org.dromara.project.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.project.config.TurnstileProperties;
import org.springframework.stereotype.Component;

/**
 * M1 Turnstile placeholder — no outbound HTTP; local dev skips when secret absent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TurnstileValidator {

    private final TurnstileProperties properties;

    public void verifyOrSkip(String turnstileToken) {
        if (!properties.isEnabled()) {
            return;
        }
        if (StringUtils.isBlank(properties.getSecretKey())) {
            log.warn("Turnstile enabled but inbound.turnstile.secret-key is empty; skipping verification (M1 stub)");
            return;
        }
        if (StringUtils.isBlank(turnstileToken)) {
            throw new ServiceException("Turnstile token required", 400);
        }
        log.debug("Turnstile M1 stub: would verify token against Cloudflare siteverify API");
    }
}
