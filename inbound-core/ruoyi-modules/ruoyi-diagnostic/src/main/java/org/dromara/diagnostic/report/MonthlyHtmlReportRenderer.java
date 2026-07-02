package org.dromara.diagnostic.report;

/**
 * 月报 HTML 导出 — EPIC-8 FR-703
 */
public final class MonthlyHtmlReportRenderer {

    private MonthlyHtmlReportRenderer() {
    }

    public static String render(WeeklyReportContext ctx) {
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
        return WeeklyHtmlReportRenderer.render(monthlyCtx);
    }
}
