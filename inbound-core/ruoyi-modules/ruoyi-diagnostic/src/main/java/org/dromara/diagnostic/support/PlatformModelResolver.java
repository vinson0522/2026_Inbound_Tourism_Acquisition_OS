package org.dromara.diagnostic.support;

import org.dromara.common.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Maps Admin display names / model strings to platform + LiteLLM model (ADR-20260626-08).
 */
public final class PlatformModelResolver {

    public static final String DEFAULT_MODEL = "gemini/gemini-2.0-flash";
    public static final String DEFAULT_PLATFORM = "gemini";

    private PlatformModelResolver() {
    }

    public record PlatformModel(String platform, String model) {
    }

    public static List<PlatformModel> resolveModels(List<String> models) {
        if (models == null || models.isEmpty()) {
            return List.of(new PlatformModel(DEFAULT_PLATFORM, DEFAULT_MODEL));
        }
        List<PlatformModel> resolved = new ArrayList<>(models.size());
        for (String raw : models) {
            resolved.add(resolveOne(raw));
        }
        return Collections.unmodifiableList(resolved);
    }

    public static PlatformModel resolveOne(String raw) {
        if (StringUtils.isBlank(raw)) {
            return new PlatformModel(DEFAULT_PLATFORM, DEFAULT_MODEL);
        }
        String trimmed = raw.trim();
        if (trimmed.contains("/")) {
            String platform = trimmed.substring(0, trimmed.indexOf('/'));
            return new PlatformModel(platform, trimmed);
        }
        String key = trimmed.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "gemini" -> new PlatformModel("gemini", DEFAULT_MODEL);
            case "perplexity" -> new PlatformModel("perplexity", "perplexity/sonar-pro");
            case "openai" -> new PlatformModel("openai", "openai/gpt-4o-mini");
            default -> new PlatformModel(key, DEFAULT_MODEL);
        };
    }
}
