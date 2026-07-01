package org.dromara.project.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * EPIC-6 M2 — 允许 Astro landing origin 访问公开 API。
 */
@Configuration
@RequiredArgsConstructor
public class PublicApiCorsConfig {

    private final LandingPublishProperties landingPublishProperties;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnProperty(prefix = "inbound.landing", name = "cors-enabled", havingValue = "true", matchIfMissing = true)
    public CorsFilter publicApiCorsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        List<String> origins = landingPublishProperties.getCorsAllowedOrigins();
        if (origins == null || origins.isEmpty()) {
            config.addAllowedOriginPattern("*");
        } else {
            origins.stream()
                .filter(o -> o != null && !o.isBlank())
                .forEach(config::addAllowedOrigin);
        }
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/public/**", config);
        return new CorsFilter(source);
    }
}
