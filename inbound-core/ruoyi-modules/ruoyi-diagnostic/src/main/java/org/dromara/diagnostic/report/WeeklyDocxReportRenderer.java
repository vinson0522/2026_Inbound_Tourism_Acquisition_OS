package org.dromara.diagnostic.report;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.project.report.ReportBranding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class WeeklyDocxReportRenderer {

    private WeeklyDocxReportRenderer() {
    }

    public static byte[] render(WeeklyReportContext ctx) throws IOException {
        ReportBranding branding = ctx.getBranding() != null ? ctx.getBranding() : ReportBranding.defaults();
        boolean monthly = "MONTHLY".equalsIgnoreCase(ctx.getReportKind());
        String kindLabel = monthly ? "增长月报" : "增长周报";

        Map<String, Object> summary = ctx.getSummary();
        Map<String, Object> geo = map(summary, "geo");
        Map<String, Object> keywords = map(summary, "keywords");
        Map<String, Object> content = map(summary, "content");
        Map<String, Object> landing = map(summary, "landing");
        Map<String, Object> leads = map(summary, "leads");
        List<String> recommendations = (List<String>) summary.get("recommendations");

        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (StringUtils.isNotBlank(branding.getLogoUrl())) {
                addParagraph(doc, "Logo: " + branding.getLogoUrl());
            }
            addTitle(doc, branding.getCoverTitle());
            addTitle(doc, kindLabel + " · " + ctx.getPeriod());
            addParagraph(doc, branding.getCompanyName());
            addParagraph(doc, "Project: " + ctx.getProjectName() + " · Brand: " + ctx.getBrandName());
            addParagraph(doc, "Period: " + ctx.getPeriodStart() + " ~ " + ctx.getPeriodEnd());

            addParagraph(doc, "KPI Summary", true);
            addParagraph(doc, "GEO runs: " + num(geo, "runs")
                + " · latest score: " + numOrDash(geo, "latestScore")
                + " · delta: " + (monthly ? momDelta(geo) : delta(geo)));
            if (monthly && geo.get("prevScore") != null) {
                addParagraph(doc, "MoM: " + numOrDash(geo, "prevScore")
                    + " → " + numOrDash(geo, "latestScore")
                    + " (" + momDelta(geo) + ")");
            }
            addParagraph(doc, "New keywords: " + num(keywords, "newCount")
                + (monthly ? " · avg score: " + numOrDash(keywords, "avgScore") : ""));
            addParagraph(doc, "Content tasks: " + num(content, "tasksCreated")
                + " · generated: " + num(content, "generated"));
            addParagraph(doc, "Landing drafts: " + num(landing, "draftCount")
                + (monthly ? " · published: " + num(landing, "publishedCount") : "")
                + " · new leads: " + num(leads, "newCount")
                + (monthly ? " · won: " + num(leads, "wonCount") : ""));

            if (monthly) {
                Map<String, Object> byStatus = map(leads, "byStatus");
                if (!byStatus.isEmpty()) {
                    addParagraph(doc, "Lead CRM by status", true);
                    for (String status : List.of("NEW", "FOLLOWING", "QUOTED", "WON", "LOST")) {
                        addParagraph(doc, status + ": " + (byStatus.get(status) != null ? byStatus.get(status) : "0"));
                    }
                }
            }

            addParagraph(doc, "Recommendations", true);
            if (recommendations != null) {
                int i = 1;
                for (String r : recommendations) {
                    addParagraph(doc, i + ". " + StringUtils.blankToDefault(r, "—"));
                    i++;
                }
            }

            addParagraph(doc, "Disclaimer: 数据来自系统统计区间，不构成 AI 推荐排名承诺。");
            addParagraph(doc, branding.getFooterText() + " · period=" + ctx.getPeriod());
            doc.write(out);
            return out.toByteArray();
        }
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

    private static void addTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun run = p.createRun();
        run.setBold(true);
        run.setFontSize(16);
        run.setText(text);
    }

    private static void addParagraph(XWPFDocument doc, String text) {
        addParagraph(doc, text, false);
    }

    private static void addParagraph(XWPFDocument doc, String text, boolean bold) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setBold(bold);
        run.setText(text);
    }
}
