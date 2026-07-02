package org.dromara.diagnostic.support;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.diagnostic.domain.DiagnosticResult;
import org.dromara.diagnostic.domain.QuestionBank;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GEO 校准对比：Jaccard 文本相似度 + 品牌 mention 一致率 — ADR-20260709-22
 */
public final class DiagnosticCalibrationSupport {

    private static final int PREVIEW_LEN = 120;

    private DiagnosticCalibrationSupport() {
    }

    public static BigDecimal jaccardSimilarity(String left, String right) {
        Set<String> a = tokenize(left);
        Set<String> b = tokenize(right);
        if (a.isEmpty() && b.isEmpty()) {
            return BigDecimal.ONE;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        double score = (double) intersection.size() / union.size();
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    public static boolean mentionsBrand(List<String> mentionedBrands, String brandName) {
        if (StringUtils.isBlank(brandName) || mentionedBrands == null || mentionedBrands.isEmpty()) {
            return false;
        }
        String normalizedBrand = brandName.trim().toLowerCase(Locale.ROOT);
        return mentionedBrands.stream()
            .filter(StringUtils::isNotBlank)
            .map(name -> name.trim().toLowerCase(Locale.ROOT))
            .anyMatch(name -> name.contains(normalizedBrand) || normalizedBrand.contains(name));
    }

    public static String preview(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= PREVIEW_LEN) {
            return trimmed;
        }
        return trimmed.substring(0, PREVIEW_LEN) + "…";
    }

    public static int citationCount(DiagnosticResult result) {
        if (result == null || StringUtils.isBlank(result.getCitationsJson())) {
            return 0;
        }
        try {
            List<?> parsed = JsonUtils.parseArray(result.getCitationsJson(), Object.class);
            return parsed != null ? parsed.size() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static String questionText(QuestionBank question) {
        return question != null ? question.getQuestion() : null;
    }

    public static String questionStage(QuestionBank question) {
        return question != null ? question.getStage() : null;
    }

    private static Set<String> tokenize(String text) {
        if (StringUtils.isBlank(text)) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
    }
}
