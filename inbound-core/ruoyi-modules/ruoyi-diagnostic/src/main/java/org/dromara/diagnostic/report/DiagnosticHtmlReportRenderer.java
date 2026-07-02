package org.dromara.diagnostic.report;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.project.report.ReportBranding;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

public final class DiagnosticHtmlReportRenderer {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");

    private DiagnosticHtmlReportRenderer() {
    }

    public static String render(DiagnosticReportContext ctx) {
        var run = ctx.getRun();
        ReportBranding branding = ctx.getBranding() != null ? ctx.getBranding() : ReportBranding.defaults();
        String color = branding.safePrimaryColor();
        String sampledAt = ctx.getSampledAt() != null ? DT.format(ctx.getSampledAt()) : "—";
        String geoScore = run.getGeoScore() != null ? run.getGeoScore().setScale(2, RoundingMode.HALF_UP).toPlainString() : "—";
        String logoBlock = StringUtils.isNotBlank(branding.getLogoUrl())
            ? "<img src=\"" + escapeAttr(branding.getLogoUrl()) + "\" alt=\"logo\" style=\"max-height:48px;margin-bottom:8px\"/>"
            : "";

        StringBuilder rows = new StringBuilder();
        for (var r : ctx.getResults()) {
            String answer = StringUtils.blankToDefault(r.getAnswerText(), "—");
            if (answer.length() > 400) {
                answer = answer.substring(0, 400) + "…";
            }
            rows.append("<tr><td>").append(r.getQuestionId())
                .append("</td><td>").append(escape(r.getPlatform()))
                .append("</td><td>").append(r.getRank() != null ? r.getRank() : "—")
                .append("</td><td>").append(escape(answer))
                .append("</td><td>").append(escape(String.join(", ", r.getMentionedBrands() != null ? r.getMentionedBrands() : java.util.List.of())))
                .append("</td></tr>");
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <title>GEO Diagnostic Report — %s</title>
              <style>
                body { font-family: Arial, sans-serif; margin: 32px; color: #1f2937; }
                h1 { font-size: 22px; margin-bottom: 8px; color: %s; }
                .cover { border-bottom: 2px solid %s; padding-bottom: 12px; margin-bottom: 12px; }
                .meta { font-size: 13px; color: #4b5563; margin-bottom: 16px; }
                .compliance { background: #eff6ff; border-left: 4px solid %s; padding: 12px; margin: 16px 0; font-size: 12px; }
                table { border-collapse: collapse; width: 100%%; font-size: 12px; margin-top: 12px; }
                th, td { border: 1px solid #d1d5db; padding: 8px; text-align: left; vertical-align: top; }
                th { background: #f3f4f6; }
                .score { font-size: 28px; font-weight: bold; color: %s; }
                .footer { margin-top: 24px; font-size: 12px; color: #6b7280; }
              </style>
            </head>
            <body>
              <div class="cover">
                %s
                <h1>%s</h1>
                <p class="meta">%s</p>
              </div>
              <p class="meta"><strong>%s</strong> · Project: %s · Brand: %s</p>
              <p class="score">GEO Score: %s</p>
              <div class="compliance">
                <strong>Compliance metadata</strong><br/>
                probe_mode: %s<br/>
                sampled_at: %s<br/>
                region: %s<br/>
                platforms: %s<br/>
                <em>Results are based on grounded sampling at the stated time and region; no ranking guarantee.</em>
              </div>
              <h2>Question results</h2>
              <table>
                <thead><tr><th>Q#</th><th>Platform</th><th>Rank</th><th>Answer excerpt</th><th>Brands</th></tr></thead>
                <tbody>%s</tbody>
              </table>
              <p class="footer">%s · runId=%d</p>
            </body>
            </html>
            """.formatted(
            escape(run.getName()),
            color,
            color,
            color,
            color,
            logoBlock,
            escape(branding.getCoverTitle()),
            escape(branding.getCompanyName()),
            escape(run.getName()),
            escape(ctx.getProjectName()),
            escape(ctx.getBrandName()),
            geoScore,
            escape(ctx.getProbeModesLabel()),
            escape(sampledAt),
            escape(StringUtils.blankToDefault(run.getRegion(), run.getMarket())),
            escape(ctx.getPlatforms()),
            rows.toString(),
            escape(branding.getFooterText()),
            run.getId()
        );
    }

    private static String escapeAttr(String s) {
        return escape(s).replace("\"", "&quot;");
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
