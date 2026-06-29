package org.dromara.diagnostic.report;

import org.dromara.diagnostic.domain.vo.DiagnosticResultVo;
import org.dromara.diagnostic.domain.vo.DiagnosticRunVo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticHtmlReportRendererTest {

    @Test
    void render_includesGeoScoreProbeModeAndCompliance() {
        DiagnosticRunVo run = new DiagnosticRunVo();
        run.setId(2L);
        run.setName("E2E Gemini Diagnostic");
        run.setGeoScore(new BigDecimal("85.00"));
        run.setRegion("us-east");
        run.setMarket("US");
        run.setProbeModes(List.of("grounded-api"));

        DiagnosticResultVo result = new DiagnosticResultVo();
        result.setQuestionId(1L);
        result.setPlatform("gemini");
        result.setRank(2);
        result.setAnswerText("Sample grounded answer");
        result.setMentionedBrands(List.of("DemoBrand"));

        DiagnosticReportContext ctx = DiagnosticReportContext.builder()
            .run(run)
            .projectName("Demo Project")
            .brandName("DemoBrand")
            .results(List.of(result))
            .platforms("gemini")
            .sampledAt(OffsetDateTime.parse("2026-06-29T10:00:00+08:00"))
            .probeModesLabel("grounded-api")
            .build();

        String html = DiagnosticHtmlReportRenderer.render(ctx);

        assertThat(html).contains("GEO Score: 85.00");
        assertThat(html).contains("probe_mode: grounded-api");
        assertThat(html).contains("Compliance metadata");
        assertThat(html).contains("no ranking guarantee");
        assertThat(html).contains("runId=2");
    }
}
