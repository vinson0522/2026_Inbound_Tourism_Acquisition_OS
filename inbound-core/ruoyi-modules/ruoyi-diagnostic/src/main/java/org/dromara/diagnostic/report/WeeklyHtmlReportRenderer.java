package org.dromara.diagnostic.report;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.project.report.ReportBranding;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class WeeklyHtmlReportRenderer {

    private WeeklyHtmlReportRenderer() {
    }

    public static String render(WeeklyReportContext ctx) {
        ReportBranding branding = ctx.getBranding() != null ? ctx.getBranding() : ReportBranding.defaults();
        boolean monthly = "MONTHLY".equalsIgnoreCase(ctx.getReportKind());
        String kindLabel = monthly ? "增长月报" : "增长周报";
        String color = branding.safePrimaryColor();

        Map<String, Object> summary = ctx.getSummary();
        Map<String, Object> geo = map(summary, "geo");
        Map<String, Object> keywords = map(summary, "keywords");
        Map<String, Object> content = map(summary, "content");
        Map<String, Object> landing = map(summary, "landing");
        Map<String, Object> leads = map(summary, "leads");
        List<String> recommendations = (List<String>) summary.get("recommendations");

        String logoBlock = buildLogoBlock(branding);
        String monthlyKpi = monthly ? buildMonthlyKpi(geo, leads) : "";

        StringBuilder recHtml = new StringBuilder("<ol>");
        if (recommendations != null) {
            for (String r : recommendations) {
                recHtml.append("<li>").append(escape(r)).append("</li>");
            }
        }
        recHtml.append("</ol>");

        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8"/>
              <title>%s — %s</title>
              <style>
                body { font-family: Arial, sans-serif; margin: 32px; color: #1f2937; }
                h1 { font-size: 22px; margin-bottom: 8px; color: %s; }
                .cover { border-bottom: 2px solid %s; padding-bottom: 16px; margin-bottom: 16px; }
                .cover img { max-height: 48px; margin-bottom: 8px; }
                .cover .company { font-size: 14px; color: #4b5563; }
                .meta { font-size: 13px; color: #4b5563; margin-bottom: 16px; }
                .kpi { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin: 16px 0; }
                .kpi div { background: #f9fafb; border: 1px solid #e5e7eb; padding: 12px; border-radius: 6px; }
                .kpi strong { display: block; font-size: 20px; color: %s; }
                .disclaimer { background: #eff6ff; border-left: 4px solid %s; padding: 12px; margin: 16px 0; font-size: 12px; }
                .footer { margin-top: 24px; font-size: 12px; color: #6b7280; border-top: 1px solid #e5e7eb; padding-top: 12px; }
              </style>
            </head>
            <body>
              <div class="cover">
                %s
                <h1>%s · %s</h1>
                <p class="company">%s</p>
              </div>
              <p class="meta"><strong>%s</strong> · Brand: %s · %s ~ %s</p>
              <div class="kpi">
                <div>GEO 诊断次数<strong>%s</strong>最新分 %s · Δ %s</div>
                <div>新关键词<strong>%s</strong>%s</div>
                <div>内容任务<strong>%s</strong>已生成 %s</div>
                <div>落地页草稿<strong>%s</strong>%s</div>
              </div>
              %s
              <h2>行动建议</h2>
              %s
              <div class="disclaimer">
                免责声明：数据来自系统统计区间，不构成 AI 推荐排名承诺。
              </div>
              <p class="footer">%s · period=%s</p>
            </body>
            </html>
            """.formatted(
            escape(kindLabel),
            escape(ctx.getPeriod()),
            color,
            color,
            color,
            color,
            logoBlock,
            escape(kindLabel),
            escape(ctx.getPeriod()),
            escape(branding.getCoverTitle()),
            escape(branding.getCompanyName()),
            escape(ctx.getProjectName()),
            escape(ctx.getBrandName()),
            escape(ctx.getPeriodStart()),
            escape(ctx.getPeriodEnd()),
            num(geo, "runs"),
            numOrDash(geo, "latestScore"),
            monthly ? momDelta(geo) : delta(geo),
            num(keywords, "newCount"),
            monthly ? " · 均分 " + numOrDash(keywords, "avgScore") : "",
            num(content, "tasksCreated"),
            num(content, "generated"),
            num(landing, "draftCount"),
            monthly ? "新询盘 " + num(leads, "newCount") + " · 成交 " + num(leads, "wonCount")
                : "新询盘 " + num(leads, "newCount"),
            monthlyKpi,
            recHtml.toString(),
            escape(branding.getFooterText()),
            escape(ctx.getPeriod())
        );
    }

    private static String buildLogoBlock(ReportBranding branding) {
        if (StringUtils.isBlank(branding.getLogoUrl())) {
            return "";
        }
        return "<img src=\"" + escapeAttr(branding.getLogoUrl()) + "\" alt=\"logo\"/>";
    }

    private static String buildMonthlyKpi(
        Map<String, Object> geo,
        Map<String, Object> leads
    ) {
        Map<String, Object> byStatus = map(leads, "byStatus");
        if (byStatus.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<h2>询盘 CRM</h2><div class=\"kpi\">");
        for (String status : List.of("NEW", "FOLLOWING", "QUOTED", "WON", "LOST")) {
            sb.append("<div>").append(status).append("<strong>")
                .append(byStatus.get(status) != null ? byStatus.get(status) : "0")
                .append("</strong></div>");
        }
        sb.append("</div>");
        if (geo.get("prevScore") != null) {
            sb.append("<p class=\"meta\">MoM: ")
                .append(numOrDash(geo, "prevScore"))
                .append(" → ")
                .append(numOrDash(geo, "latestScore"))
                .append(" (")
                .append(momDelta(geo))
                .append(")</p>");
        }
        return sb.toString();
    }

    private static Map<String, Object> map(Map<String, Object> summary, String key) {
        Object v = summary != null ? summary.get(key) : null;
        if (v instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }

    private static String num(Map<String, Object> m, String key) {
        if (m == null || m.get(key) == null) {
            return "0";
        }
        return String.valueOf(m.get(key));
    }

    private static String numOrDash(Map<String, Object> m, String key) {
        if (m == null || m.get(key) == null) {
            return "—";
        }
        return String.valueOf(m.get(key));
    }

    private static String delta(Map<String, Object> geo) {
        if (geo == null || geo.get("delta") == null) {
            return "—";
        }
        int d = ((Number) geo.get("delta")).intValue();
        return d >= 0 ? "+" + d : String.valueOf(d);
    }

    private static String momDelta(Map<String, Object> geo) {
        if (geo == null || geo.get("momDelta") == null) {
            return delta(geo);
        }
        int d = ((Number) geo.get("momDelta")).intValue();
        return d >= 0 ? "+" + d : String.valueOf(d);
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeAttr(String s) {
        return escape(s).replace("\"", "&quot;");
    }
}
