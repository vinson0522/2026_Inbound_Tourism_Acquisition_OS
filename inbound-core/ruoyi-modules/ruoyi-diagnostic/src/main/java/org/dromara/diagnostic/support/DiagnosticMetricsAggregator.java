package org.dromara.diagnostic.support;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.diagnostic.domain.DiagnosticResult;
import org.dromara.diagnostic.domain.vo.DiagnosticTrendMetricsVo;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 从 diagnostic_result 行聚合六分项指标（与 inbound-ai scorer.aggregate_metrics 一致，不调 LLM）。
 */
public final class DiagnosticMetricsAggregator {

    private DiagnosticMetricsAggregator() {
    }

    public static DiagnosticTrendMetricsVo aggregate(
        List<DiagnosticResult> results,
        Map<Long, Boolean> longtailByQuestion
    ) {
        DiagnosticTrendMetricsVo metrics = new DiagnosticTrendMetricsVo();
        if (results == null || results.isEmpty()) {
            metrics.setBrandMentionRate(0.0);
            metrics.setTop3Rate(0.0);
            metrics.setCompetitorSuppression(0.0);
            metrics.setCitationCoverage(0.0);
            metrics.setLongtailCoverage(0.0);
            metrics.setAssetCompleteness(0.0);
            return metrics;
        }

        Map<Long, Boolean> longtail = longtailByQuestion == null ? Collections.emptyMap() : longtailByQuestion;
        int n = results.size();
        double brandHits = 0;
        double top3Hits = 0;
        double competitorSum = 0;
        double citationHits = 0;
        double longtailHits = 0;
        double assetSum = 0;

        for (DiagnosticResult result : results) {
            boolean brandMentioned = result.getMentionedBrands() != null && !result.getMentionedBrands().isEmpty();
            if (brandMentioned) {
                brandHits++;
            }
            if (result.getRank() != null && result.getRank() <= 3) {
                top3Hits++;
            }
            if (result.getCompetitors() != null && !result.getCompetitors().isEmpty() && !brandMentioned) {
                competitorSum += 1.0;
            }
            if (hasCitations(result.getCitationsJson())) {
                citationHits++;
            }
            if (Boolean.TRUE.equals(longtail.getOrDefault(result.getQuestionId(), false))) {
                longtailHits++;
            }
            assetSum += 1.0;
        }

        metrics.setBrandMentionRate(brandHits / n);
        metrics.setTop3Rate(top3Hits / n);
        metrics.setCompetitorSuppression(competitorSum / n);
        metrics.setCitationCoverage(citationHits / n);
        metrics.setLongtailCoverage(longtailHits / n);
        metrics.setAssetCompleteness(assetSum / n);
        return metrics;
    }

    private static boolean hasCitations(String citationsJson) {
        if (StringUtils.isBlank(citationsJson)) {
            return false;
        }
        String trimmed = citationsJson.trim();
        return !"[]".equals(trimmed) && !"{}".equals(trimmed);
    }
}
