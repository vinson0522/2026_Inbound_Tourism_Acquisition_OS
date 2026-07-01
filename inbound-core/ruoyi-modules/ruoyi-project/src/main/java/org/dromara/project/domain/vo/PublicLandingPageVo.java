package org.dromara.project.domain.vo;

import lombok.Data;

import java.util.Map;

/**
 * 公网落地页 — 对齐 Astro / EPIC-6 M2
 */
@Data
public class PublicLandingPageVo {

    private Long id;

    private Long projectId;

    private String title;

    private String slug;

    private Map<String, Object> contentJson;

    private Map<String, Object> seoMetaJson;

    private Map<String, Object> formConfigJson;

    private String whatsappLink;

    /** Cloudflare Turnstile site key；未配置时为 null */
    private String turnstileSiteKey;
}
