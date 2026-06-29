package org.dromara.diagnostic.support;

import org.dromara.diagnostic.domain.DiagnosticResult;
import org.dromara.diagnostic.domain.vo.DiagnosticTrendMetricsVo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticMetricsAggregatorTest {

    @Test
    void aggregate_emptyResults_returnsZeros() {
        DiagnosticTrendMetricsVo metrics = DiagnosticMetricsAggregator.aggregate(List.of(), Map.of());
        assertThat(metrics.getBrandMentionRate()).isZero();
        assertThat(metrics.getTop3Rate()).isZero();
        assertThat(metrics.getCompetitorSuppression()).isZero();
        assertThat(metrics.getCitationCoverage()).isZero();
        assertThat(metrics.getLongtailCoverage()).isZero();
        assertThat(metrics.getAssetCompleteness()).isZero();
    }

    @Test
    void aggregate_twoResults_computesRates() {
        DiagnosticResult hit = new DiagnosticResult();
        hit.setQuestionId(1L);
        hit.setMentionedBrands(List.of("DemoBrand"));
        hit.setRank(2);
        hit.setCitationsJson("[{\"url\":\"https://example.com\"}]");

        DiagnosticResult miss = new DiagnosticResult();
        miss.setQuestionId(2L);
        miss.setMentionedBrands(List.of());
        miss.setCompetitors(List.of("RivalCo"));
        miss.setCitationsJson("[]");

        DiagnosticTrendMetricsVo metrics = DiagnosticMetricsAggregator.aggregate(
            List.of(hit, miss),
            Map.of(2L, true)
        );

        assertThat(metrics.getBrandMentionRate()).isEqualTo(0.5);
        assertThat(metrics.getTop3Rate()).isEqualTo(0.5);
        assertThat(metrics.getCompetitorSuppression()).isEqualTo(0.5);
        assertThat(metrics.getCitationCoverage()).isEqualTo(0.5);
        assertThat(metrics.getLongtailCoverage()).isEqualTo(0.5);
        assertThat(metrics.getAssetCompleteness()).isEqualTo(1.0);
    }
}
