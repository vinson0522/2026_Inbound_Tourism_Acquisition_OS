package org.dromara.diagnostic.report;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.project.report.ReportBranding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

public final class DiagnosticDocxReportRenderer {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");

    private DiagnosticDocxReportRenderer() {
    }

    public static byte[] render(DiagnosticReportContext ctx) throws IOException {
        ReportBranding branding = ctx.getBranding() != null ? ctx.getBranding() : ReportBranding.defaults();
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var run = ctx.getRun();
            if (StringUtils.isNotBlank(branding.getLogoUrl())) {
                addParagraph(doc, "Logo: " + branding.getLogoUrl());
            }
            addTitle(doc, branding.getCoverTitle());
            addTitle(doc, "GEO Diagnostic Report — " + run.getName());
            addParagraph(doc, branding.getCompanyName());
            addParagraph(doc, "Project: " + ctx.getProjectName() + " · Brand: " + ctx.getBrandName());
            String geo = run.getGeoScore() != null
                ? run.getGeoScore().setScale(2, RoundingMode.HALF_UP).toPlainString()
                : "—";
            addParagraph(doc, "GEO Score: " + geo, true);

            addParagraph(doc, "Compliance metadata", true);
            addParagraph(doc, "probe_mode: " + ctx.getProbeModesLabel());
            addParagraph(doc, "sampled_at: " + (ctx.getSampledAt() != null ? DT.format(ctx.getSampledAt()) : "—"));
            addParagraph(doc, "region: " + StringUtils.blankToDefault(run.getRegion(), run.getMarket()));
            addParagraph(doc, "platforms: " + ctx.getPlatforms());
            addParagraph(doc, "Results are based on grounded sampling; no ranking guarantee.");

            addParagraph(doc, "Question results", true);
            XWPFTable table = doc.createTable();
            setRow(table.getRow(0), "Q#", "Platform", "Rank", "Answer excerpt", "Brands");
            for (var r : ctx.getResults()) {
                String answer = StringUtils.blankToDefault(r.getAnswerText(), "—");
                if (answer.length() > 200) {
                    answer = answer.substring(0, 200) + "…";
                }
                XWPFTableRow row = table.createRow();
                setRow(row,
                    String.valueOf(r.getQuestionId()),
                    r.getPlatform(),
                    r.getRank() != null ? String.valueOf(r.getRank()) : "—",
                    answer,
                    String.join(", ", r.getMentionedBrands() != null ? r.getMentionedBrands() : java.util.List.of())
                );
            }

            addParagraph(doc, branding.getFooterText() + " · runId=" + run.getId());
            doc.write(out);
            return out.toByteArray();
        }
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

    private static void setRow(XWPFTableRow row, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            while (row.getTableCells().size() <= i) {
                row.addNewTableCell();
            }
            row.getCell(i).setText(cells[i]);
        }
    }
}
