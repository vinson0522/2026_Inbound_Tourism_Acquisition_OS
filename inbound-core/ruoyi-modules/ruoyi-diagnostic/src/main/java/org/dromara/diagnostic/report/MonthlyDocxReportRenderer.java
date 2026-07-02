package org.dromara.diagnostic.report;

import java.io.IOException;

/**
 * 月报 DOCX 导出 — EPIC-8 FR-703
 */
public final class MonthlyDocxReportRenderer {

    private MonthlyDocxReportRenderer() {
    }

    public static byte[] render(WeeklyReportContext ctx) throws IOException {
        WeeklyReportContext monthlyCtx = WeeklyReportContext.builder()
            .projectName(ctx.getProjectName())
            .brandName(ctx.getBrandName())
            .period(ctx.getPeriod())
            .periodStart(ctx.getPeriodStart())
            .periodEnd(ctx.getPeriodEnd())
            .reportKind("MONTHLY")
            .summary(ctx.getSummary())
            .branding(ctx.getBranding())
            .build();
        return WeeklyDocxReportRenderer.render(monthlyCtx);
    }
}
