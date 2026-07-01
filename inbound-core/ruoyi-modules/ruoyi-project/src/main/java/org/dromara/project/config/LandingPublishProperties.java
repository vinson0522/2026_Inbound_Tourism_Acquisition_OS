package org.dromara.project.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "inbound.landing")
public class LandingPublishProperties {

    /** Astro 公网预览根 URL，用于 published_url */
    private String publicBaseUrl = "http://localhost:4321";

    /** CORS 允许访问 /api/v1/public/** 的来源（如 Astro dev server） */
    private List<String> corsAllowedOrigins = new ArrayList<>(List.of("http://localhost:4321"));

    public String buildPublishedUrl(Long projectId, String slug) {
        String base = publicBaseUrl != null ? publicBaseUrl.replaceAll("/+$", "") : "http://localhost:4321";
        return base + "/p/" + projectId + "/" + slug;
    }
}
